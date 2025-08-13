package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.models.Guild;
import com.guild.models.GuildLog;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 工会日志查看GUI
 */
public class GuildLogsGUI implements GUI {
    
    private final GuildPlugin plugin;
    private final Guild guild;
    private final Player player;
    private final int page;
    private final int itemsPerPage = 28; // 2-8列，2-5行
    private List<GuildLog> logs;
    private int totalLogs;
    
    public GuildLogsGUI(GuildPlugin plugin, Guild guild, Player player) {
        this(plugin, guild, player, 0);
    }
    
    public GuildLogsGUI(GuildPlugin plugin, Guild guild, Player player, int page) {
        this.plugin = plugin;
        this.guild = guild;
        this.player = player;
        this.page = page;
    }
    
    @Override
    public String getTitle() {
        return ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-logs.title", "&6工会日志 - {guild_name}")
            .replace("{guild_name}", guild.getName()));
    }
    
    @Override
    public int getSize() {
        return 54;
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // 填充边框
        fillBorder(inventory);
        
        // 异步加载日志数据
        loadLogsAsync().thenAccept(success -> {
            if (success) {
                // 在主线程中设置物品和完整的导航按钮
                Bukkit.getScheduler().runTask(plugin, () -> {
                    setupLogItems(inventory);
                    setupBasicNavigationButtons(inventory);
                    setupFullNavigationButtons(inventory);
                });
            } else {
                // 如果加载失败，在主线程中显示错误信息
                Bukkit.getScheduler().runTask(plugin, () -> {
                    ItemStack errorItem = createItem(
                        Material.BARRIER,
                        ColorUtils.colorize("&c加载失败"),
                        ColorUtils.colorize("&7无法加载日志数据，请重试")
                    );
                    inventory.setItem(22, errorItem);
                    setupBasicNavigationButtons(inventory);
                });
            }
        });
    }
    
    /**
     * 异步加载日志数据
     */
    private CompletableFuture<Boolean> loadLogsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                plugin.getLogger().info("开始加载工会 " + guild.getName() + " 的日志数据...");
                
                // 检查工会ID是否有效
                if (guild.getId() <= 0) {
                    plugin.getLogger().warning("工会ID无效: " + guild.getId());
                    return false;
                }
                
                // 获取日志总数
                totalLogs = plugin.getGuildService().getGuildLogsCountAsync(guild.getId()).get();
                plugin.getLogger().info("工会 " + guild.getName() + " 共有 " + totalLogs + " 条日志记录");
                
                // 获取当前页的日志
                int offset = page * itemsPerPage;
                logs = plugin.getGuildService().getGuildLogsAsync(guild.getId(), itemsPerPage, offset).get();
                plugin.getLogger().info("成功加载第 " + (page + 1) + " 页的 " + logs.size() + " 条日志记录");
                
                return true;
            } catch (Exception e) {
                plugin.getLogger().severe("加载工会日志时发生错误: " + e.getMessage());
                e.printStackTrace();
                
                // 设置默认值
                totalLogs = 0;
                logs = new java.util.ArrayList<>();
                
                return false;
            }
        });
    }
    
    /**
     * 设置日志物品
     */
    private void setupLogItems(Inventory inventory) {
        if (logs == null) {
            logs = new java.util.ArrayList<>(); // 确保logs不为null
        }
        
        if (logs.isEmpty()) {
            // 显示无日志信息
            ItemStack noLogs = createItem(
                Material.BARRIER,
                ColorUtils.colorize("&c暂无日志记录"),
                ColorUtils.colorize("&7该工会还没有任何操作记录"),
                ColorUtils.colorize("&7请等待工会活动产生日志")
            );
            inventory.setItem(22, noLogs);
            return;
        }
        
        // 显示日志列表
        for (int i = 0; i < Math.min(logs.size(), itemsPerPage); i++) {
            GuildLog log = logs.get(i);
            int slot = getLogSlot(i);
            
            ItemStack logItem = createLogItem(log);
            inventory.setItem(slot, logItem);
        }
    }
    
    /**
     * 创建日志物品
     */
    private ItemStack createLogItem(GuildLog log) {
        Material material = getLogMaterial(log.getLogType());
        String name = ColorUtils.colorize("&e" + log.getLogType().getDisplayName());
        
        List<String> lore = new java.util.ArrayList<>();
        lore.add(ColorUtils.colorize("&7操作者: &f" + log.getPlayerName()));
        lore.add(ColorUtils.colorize("&7时间: &f" + log.getSimpleTime()));
        lore.add(ColorUtils.colorize("&7描述: &f" + log.getDescription()));
        
        if (log.getDetails() != null && !log.getDetails().isEmpty()) {
            lore.add(ColorUtils.colorize("&7详情: &f" + log.getDetails()));
        }
        
        return createItem(material, name, lore.toArray(new String[0]));
    }
    
    /**
     * 根据日志类型获取物品材质
     */
    private Material getLogMaterial(GuildLog.LogType logType) {
        switch (logType) {
            case GUILD_CREATED:
                return Material.GREEN_WOOL;
            case GUILD_DISSOLVED:
                return Material.RED_WOOL;
            case MEMBER_JOINED:
                return Material.EMERALD;
            case MEMBER_LEFT:
            case MEMBER_KICKED:
                return Material.REDSTONE;
            case MEMBER_PROMOTED:
                return Material.GOLD_INGOT;
            case MEMBER_DEMOTED:
                return Material.IRON_INGOT;
            case LEADER_TRANSFERRED:
                return Material.DIAMOND;
            case FUND_DEPOSITED:
                return Material.GOLD_NUGGET;
            case FUND_WITHDRAWN:
                return Material.IRON_NUGGET;
            case FUND_TRANSFERRED:
                return Material.EMERALD_BLOCK;
            case RELATION_CREATED:
            case RELATION_ACCEPTED:
                return Material.BLUE_WOOL;
            case RELATION_DELETED:
            case RELATION_REJECTED:
                return Material.ORANGE_WOOL;
            case GUILD_FROZEN:
                return Material.ICE;
            case GUILD_UNFROZEN:
                return Material.WATER_BUCKET;
            case GUILD_LEVEL_UP:
                return Material.EXPERIENCE_BOTTLE;
            case APPLICATION_SUBMITTED:
            case APPLICATION_ACCEPTED:
            case APPLICATION_REJECTED:
                return Material.PAPER;
            case INVITATION_SENT:
            case INVITATION_ACCEPTED:
            case INVITATION_REJECTED:
                return Material.BOOK;
            default:
                return Material.GRAY_WOOL;
        }
    }
    
    /**
     * 获取日志物品的槽位
     */
    private int getLogSlot(int index) {
        int row = index / 7; // 7列
        int col = index % 7;
        return (row + 2) * 9 + (col + 1); // 从第2行第1列开始
    }
    
    /**
     * 设置基本的导航按钮（不依赖日志数据）
     */
    private void setupBasicNavigationButtons(Inventory inventory) {
        // 返回按钮 - 移到槽位49，与其他GUI保持一致
        ItemStack backButton = createItem(
            Material.ARROW,
            ColorUtils.colorize("&c返回"),
            ColorUtils.colorize("&7返回上一级菜单")
        );
        inventory.setItem(49, backButton);
    }
    
    /**
     * 设置完整的导航按钮（依赖日志数据）
     */
    private void setupFullNavigationButtons(Inventory inventory) {
        // 分页按钮
        if (page > 0) {
            ItemStack prevButton = createItem(
                Material.ARROW,
                ColorUtils.colorize("&e上一页"),
                ColorUtils.colorize("&7查看上一页日志")
            );
            inventory.setItem(45, prevButton);
        }
        
        if ((page + 1) * itemsPerPage < totalLogs) {
            ItemStack nextButton = createItem(
                Material.ARROW,
                ColorUtils.colorize("&e下一页"),
                ColorUtils.colorize("&7查看下一页日志")
            );
            inventory.setItem(53, nextButton);
        }
        
        // 页码信息
        ItemStack pageInfo = createItem(
            Material.PAPER,
            ColorUtils.colorize("&6页码信息"),
            ColorUtils.colorize("&7当前页: &f" + (page + 1)),
            ColorUtils.colorize("&7总页数: &f" + ((totalLogs - 1) / itemsPerPage + 1)),
            ColorUtils.colorize("&7总记录: &f" + totalLogs)
        );
        inventory.setItem(47, pageInfo);
        
        // 刷新按钮
        ItemStack refreshButton = createItem(
            Material.EMERALD,
            ColorUtils.colorize("&a刷新"),
            ColorUtils.colorize("&7刷新日志列表")
        );
        inventory.setItem(51, refreshButton);
    }
    
    /**
     * 填充边框
     */
    private void fillBorder(Inventory inventory) {
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        
        // 填充第一行和最后一行
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(45 + i, border);
        }
        
        // 填充第一列和最后一列
        for (int i = 0; i < 6; i++) {
            inventory.setItem(i * 9, border);
            inventory.setItem(i * 9 + 8, border);
        }
    }
    
    /**
     * 创建物品
     */
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(java.util.Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        // 检查是否点击了边框（但允许底部导航按钮）
        if (slot < 9 || (slot >= 54) || slot % 9 == 0 || slot % 9 == 8) {
            return;
        }
        
        // 检查点击的物品是否为空
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        
        try {
            // 处理导航按钮
            switch (slot) {
                case 45: // 上一页
                    if (page > 0) {
                        plugin.getGuiManager().openGUI(player, new GuildLogsGUI(plugin, guild, player, page - 1));
                    }
                    break;
                case 47: // 页码信息（无操作）
                    break;
                case 49: // 返回按钮
                    plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild));
                    break;
                case 51: // 刷新按钮
                    // 使用GUIManager的内置刷新方法
                    plugin.getGuiManager().refreshGUI(player);
                    break;
                case 53: // 下一页
                    if ((page + 1) * itemsPerPage < totalLogs) {
                        plugin.getGuiManager().openGUI(player, new GuildLogsGUI(plugin, guild, player, page + 1));
                    }
                    break;
                default:
                    // 检查是否点击了日志物品
                    int logIndex = getLogIndex(slot);
                    if (logIndex >= 0 && logs != null && logIndex < logs.size()) {
                        GuildLog log = logs.get(logIndex);
                        showLogDetails(player, log);
                    }
                    break;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("处理GUI点击事件时发生错误: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage(ColorUtils.colorize("&c处理操作时发生错误，请重试"));
        }
    }
    
    /**
     * 获取日志索引
     */
    private int getLogIndex(int slot) {
        int row = slot / 9 - 2; // 减去第一行和边框
        int col = slot % 9 - 1; // 减去第一列边框
        if (row < 0 || col < 0 || col >= 7) {
            return -1;
        }
        return row * 7 + col;
    }
    
    /**
     * 显示日志详情
     */
    private void showLogDetails(Player player, GuildLog log) {
        String message = plugin.getConfigManager().getMessagesConfig().getString("log-details", "&6=== 日志详情 ===");
        player.sendMessage(ColorUtils.colorize(message));
        
        String[] details = {
            "&e操作类型: &f" + log.getLogType().getDisplayName(),
            "&e操作者: &f" + log.getPlayerName(),
            "&e时间: &f" + log.getFormattedTime(),
            "&e描述: &f" + log.getDescription()
        };
        
        for (String detail : details) {
            player.sendMessage(ColorUtils.colorize(detail));
        }
        
        if (log.getDetails() != null && !log.getDetails().isEmpty()) {
            player.sendMessage(ColorUtils.colorize("&e详情: &f" + log.getDetails()));
        }
        
        player.sendMessage(ColorUtils.colorize("&6=================="));
    }
    
    @Override
    public void onClose(Player player) {
        // GUI关闭时的清理工作
        plugin.getLogger().info("玩家 " + player.getName() + " 关闭了工会日志GUI");
    }
}
