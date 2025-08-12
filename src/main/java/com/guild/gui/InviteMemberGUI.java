package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.models.Guild;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 邀请成员GUI
 */
public class InviteMemberGUI implements GUI {
    
    private final GuildPlugin plugin;
    private final Guild guild;
    private int currentPage = 0;
    private List<Player> onlinePlayers;
    
    public InviteMemberGUI(GuildPlugin plugin, Guild guild) {
        this.plugin = plugin;
        this.guild = guild;
        this.onlinePlayers = Bukkit.getOnlinePlayers().stream()
            .filter(player -> !player.getUniqueId().equals(guild.getLeaderUuid()))
            .collect(java.util.stream.Collectors.toList());
    }
    
    @Override
    public String getTitle() {
        return ColorUtils.colorize("&6邀请成员 - 第" + (currentPage + 1) + "页");
    }
    
    @Override
    public int getSize() {
        return 54;
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // 填充边框
        fillBorder(inventory);
        
        // 显示在线玩家
        displayOnlinePlayers(inventory);
        
        // 添加导航按钮
        setupNavigationButtons(inventory);
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (slot >= 9 && slot < 45) {
            // 玩家头像区域
            int playerIndex = slot - 9 + (currentPage * 36);
            if (playerIndex < onlinePlayers.size()) {
                Player targetPlayer = onlinePlayers.get(playerIndex);
                handleInvitePlayer(player, targetPlayer);
            }
        } else if (slot == 45) {
            // 上一页
            if (currentPage > 0) {
                currentPage--;
                plugin.getGuiManager().refreshGUI(player);
            }
        } else if (slot == 53) {
            // 下一页
            int maxPage = (onlinePlayers.size() - 1) / 36;
            if (currentPage < maxPage) {
                currentPage++;
                plugin.getGuiManager().refreshGUI(player);
            }
        } else if (slot == 49) {
            // 返回
            plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild));
        }
    }
    
    /**
     * 填充边框
     */
    private void fillBorder(Inventory inventory) {
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(i + 45, border);
        }
        for (int i = 9; i < 45; i += 9) {
            inventory.setItem(i, border);
            inventory.setItem(i + 8, border);
        }
    }
    
    /**
     * 显示在线玩家
     */
    private void displayOnlinePlayers(Inventory inventory) {
        int startIndex = currentPage * 36;
        int endIndex = Math.min(startIndex + 36, onlinePlayers.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            Player targetPlayer = onlinePlayers.get(i);
            int slot = 9 + (i - startIndex);
            
            ItemStack playerHead = createPlayerHead(targetPlayer);
            inventory.setItem(slot, playerHead);
        }
    }
    
    /**
     * 设置导航按钮
     */
    private void setupNavigationButtons(Inventory inventory) {
        // 上一页按钮
        if (currentPage > 0) {
            ItemStack prevPage = createItem(
                Material.ARROW,
                ColorUtils.colorize("&e上一页"),
                ColorUtils.colorize("&7点击查看上一页")
            );
            inventory.setItem(45, prevPage);
        }
        
        // 下一页按钮
        int maxPage = (onlinePlayers.size() - 1) / 36;
        if (currentPage < maxPage) {
            ItemStack nextPage = createItem(
                Material.ARROW,
                ColorUtils.colorize("&e下一页"),
                ColorUtils.colorize("&7点击查看下一页")
            );
            inventory.setItem(53, nextPage);
        }
        
        // 返回按钮
        ItemStack back = createItem(
            Material.BARRIER,
            ColorUtils.colorize("&c返回"),
            ColorUtils.colorize("&7返回工会设置")
        );
        inventory.setItem(49, back);
    }
    
    /**
     * 创建玩家头像
     */
    private ItemStack createPlayerHead(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(ColorUtils.colorize("&a" + player.getName()));
            meta.setLore(Arrays.asList(
                ColorUtils.colorize("&7点击邀请该玩家"),
                ColorUtils.colorize("&7加入工会")
            ));
            head.setItemMeta(meta);
        }
        
        return head;
    }
    
    /**
     * 处理邀请玩家
     */
    private void handleInvitePlayer(Player inviter, Player target) {
        // 检查目标玩家是否已经在工会中
        plugin.getGuildService().getGuildMemberAsync(target.getUniqueId()).thenAccept(member -> {
            if (member != null) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("invite.already-in-guild", "&c该玩家已经在工会中！");
                inviter.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            // 发送邀请
            plugin.getGuildService().sendInvitationAsync(guild.getId(), inviter.getUniqueId(), inviter.getName(), target.getUniqueId(), target.getName()).thenAccept(success -> {
                if (success) {
                    String inviterMessage = plugin.getConfigManager().getMessagesConfig().getString("invite.sent", "&a已向 &e{player} &a发送邀请！")
                        .replace("{player}", target.getName());
                    inviter.sendMessage(ColorUtils.colorize(inviterMessage));
                    
                    String targetMessage = plugin.getConfigManager().getMessagesConfig().getString("invite.received", "&a你收到了来自工会 &e{guild} &a的邀请！")
                        .replace("{guild}", guild.getName());
                    target.sendMessage(ColorUtils.colorize(targetMessage));
                } else {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("invite.failed", "&c邀请发送失败！");
                    inviter.sendMessage(ColorUtils.colorize(message));
                }
            });
        });
    }
    
    /**
     * 创建物品
     */
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        
        return item;
    }
}
