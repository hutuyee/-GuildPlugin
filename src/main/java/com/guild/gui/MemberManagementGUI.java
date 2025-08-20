package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.PlaceholderUtils;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 成员管理GUI
 */
public class MemberManagementGUI implements GUI {
    
    private final GuildPlugin plugin;
    private final Guild guild;
    private int currentPage = 0;
    private static final int MEMBERS_PER_PAGE = 28; // 4行7列，除去边框
    
    public MemberManagementGUI(GuildPlugin plugin, Guild guild) {
        this.plugin = plugin;
        this.guild = guild;
    }
    
    @Override
    public String getTitle() {
        return ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.title", "&6成员管理"));
    }
    
    @Override
    public int getSize() {
        return plugin.getConfigManager().getGuiConfig().getInt("member-management.size", 54);
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // 填充边框
        fillBorder(inventory);
        
        // 添加功能按钮
        setupFunctionButtons(inventory);
        
        // 加载成员列表
        loadMembers(inventory);
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        // 检查是否是功能按钮
        if (isFunctionButton(slot)) {
            handleFunctionButton(player, slot);
            return;
        }
        
        // 检查是否是分页按钮
        if (isPaginationButton(slot)) {
            handlePaginationButton(player, slot);
            return;
        }
        
        // 检查是否是成员按钮
        if (isMemberSlot(slot)) {
            handleMemberClick(player, slot, clickedItem, clickType);
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
     * 设置功能按钮
     */
    private void setupFunctionButtons(Inventory inventory) {
        // 邀请成员按钮
        ItemStack inviteMember = createItem(
            Material.EMERALD_BLOCK,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.invite-member.name", "&a邀请成员")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.invite-member.lore.1", "&7邀请新成员加入"))
        );
        inventory.setItem(45, inviteMember);
        
        // 踢出成员按钮
        ItemStack kickMember = createItem(
            Material.REDSTONE_BLOCK,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.kick-member.name", "&c踢出成员")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.kick-member.lore.1", "&7踢出工会成员"))
        );
        inventory.setItem(47, kickMember);
        
        // 提升成员按钮
        ItemStack promoteMember = createItem(
            Material.GOLD_INGOT,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.promote-member.name", "&6提升成员")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.promote-member.lore.1", "&7提升成员职位"))
        );
        inventory.setItem(49, promoteMember);
        
        // 降级成员按钮
        ItemStack demoteMember = createItem(
            Material.IRON_INGOT,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.demote-member.name", "&7降级成员")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.demote-member.lore.1", "&7降级成员职位"))
        );
        inventory.setItem(51, demoteMember);
        
        // 返回按钮
        ItemStack back = createItem(
            Material.ARROW,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.back.name", "&7返回")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.back.lore.1", "&7返回主菜单"))
        );
        inventory.setItem(53, back);
    }

    /**
     * 转让会长
     */
    private void handleChangeLeader(Player player) {
        // 检查权限（只有会长可以转）
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&c只有工会会长才能执行此操作");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // 打开降级成员GUI
        plugin.getGuiManager().openGUI(player, new ChangeLeaderGUI(plugin, guild));
    }
    /**
     * 加载成员列表
     */
    private void loadMembers(Inventory inventory) {
        plugin.getGuildService().getGuildMembersAsync(guild.getId()).thenAccept(members -> {
            if (members == null || members.isEmpty()) {
                // 显示无成员信息
                ItemStack noMembers = createItem(
                    Material.BARRIER,
                    ColorUtils.colorize("&c暂无成员"),
                    ColorUtils.colorize("&7工会中还没有成员")
                );
                inventory.setItem(22, noMembers);
                return;
            }
            
            // 计算分页
            int totalPages = (members.size() - 1) / MEMBERS_PER_PAGE;
            if (currentPage > totalPages) {
                currentPage = totalPages;
            }
            
            // 设置分页按钮
            setupPaginationButtons(inventory, totalPages);
            
            // 显示当前页的成员
            int startIndex = currentPage * MEMBERS_PER_PAGE;
            int endIndex = Math.min(startIndex + MEMBERS_PER_PAGE, members.size());
            
            int slotIndex = 10; // 从第2行第2列开始
            for (int i = startIndex; i < endIndex; i++) {
                GuildMember member = members.get(i);
                if (slotIndex >= 44) break; // 避免超出显示区域
                
                ItemStack memberItem = createMemberItem(member);
                inventory.setItem(slotIndex, memberItem);
                
                slotIndex++;
                if (slotIndex % 9 == 8) { // 跳过边框
                    slotIndex += 2;
                }
            }
        });
    }
    
    /**
     * 设置分页按钮
     */
    private void setupPaginationButtons(Inventory inventory, int totalPages) {
        // 上一页按钮
        if (currentPage > 0) {
            ItemStack previousPage = createItem(
                Material.ARROW,
                ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.previous-page.name", "&c上一页")),
                ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.previous-page.lore.1", "&7查看上一页"))
            );
            inventory.setItem(18, previousPage);
        }
        
        // 下一页按钮
        if (currentPage < totalPages) {
            ItemStack nextPage = createItem(
                Material.ARROW,
                ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.next-page.name", "&a下一页")),
                ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.next-page.lore.1", "&7查看下一页"))
            );
            inventory.setItem(26, nextPage);
        }
    }
    
    /**
     * 创建成员物品
     */
    private ItemStack createMemberItem(GuildMember member) {
        Material material;
        String name;
        List<String> lore = new ArrayList<>();
        
        switch (member.getRole()) {
            case LEADER:
                material = Material.GOLDEN_HELMET;
                name = PlaceholderUtils.replaceMemberPlaceholders("&c{member_name}", member, guild);
                lore.add(PlaceholderUtils.replaceMemberPlaceholders("&7角色: &c{member_role}", member, guild));
                break;
            case OFFICER:
                material = Material.GOLDEN_HELMET;
                name = PlaceholderUtils.replaceMemberPlaceholders("&6{member_name}", member, guild);
                lore.add(PlaceholderUtils.replaceMemberPlaceholders("&7角色: &6{member_role}", member, guild));
                break;
            default:
                material = Material.PLAYER_HEAD;
                name = PlaceholderUtils.replaceMemberPlaceholders("&f{member_name}", member, guild);
                lore.add(PlaceholderUtils.replaceMemberPlaceholders("&7角色: &f{member_role}", member, guild));
                break;
        }
        
        lore.add(PlaceholderUtils.replaceMemberPlaceholders("&7加入时间: {member_join_time}", member, guild));
        lore.add(PlaceholderUtils.replaceMemberPlaceholders("&7权限: " + getRolePermissions(member.getRole()), member, guild));
        lore.add("");
        lore.add(ColorUtils.colorize("&a左键: 查看详情"));
        
        if (member.getRole() != GuildMember.Role.LEADER) {
            lore.add(ColorUtils.colorize("&c右键: 踢出成员"));
            lore.add(ColorUtils.colorize("&6中键: 提升/降级"));
        }
        
        return createItem(material, name, lore.toArray(new String[0]));
    }
    
    /**
     * 获取角色权限描述
     */
    private String getRolePermissions(GuildMember.Role role) {
        switch (role) {
            case LEADER:
                return "所有权限";
            case OFFICER:
                return "邀请、踢出";
            default:
                return "基础权限";
        }
    }
    
    /**
     * 检查是否是功能按钮
     */
    private boolean isFunctionButton(int slot) {
        return slot == 45 || slot == 47 || slot == 49 || slot == 51 || slot == 53;
    }
    
    /**
     * 检查是否是分页按钮
     */
    private boolean isPaginationButton(int slot) {
        return slot == 18 || slot == 26;
    }
    
    /**
     * 检查是否是成员槽位
     */
    private boolean isMemberSlot(int slot) {
        return slot >= 10 && slot <= 44 && slot % 9 != 0 && slot % 9 != 8;
    }
    
    /**
     * 处理功能按钮点击
     */
    private void handleFunctionButton(Player player, int slot) {
        switch (slot) {
            case 45: // 邀请成员
                handleInviteMember(player);
                break;
            case 47: // 踢出成员
                handleKickMember(player);
                break;
            case 49: // 提升成员
                handlePromoteMember(player);
                break;
            case 51: // 降级成员
                handleDemoteMember(player);
                break;
            case 52: // 转让
                handleChangeLeader(player);
                break;
            case 53: // 返回
                plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin));
                break;
        }
    }
    
    /**
     * 处理分页按钮点击
     */
    private void handlePaginationButton(Player player, int slot) {
        if (slot == 18) { // 上一页
            if (currentPage > 0) {
                currentPage--;
                refreshInventory(player);
            }
        } else if (slot == 26) { // 下一页
            currentPage++;
            refreshInventory(player);
        }
    }
    
    /**
     * 处理成员点击
     */
    private void handleMemberClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        // 获取点击的成员
        int memberIndex = (currentPage * MEMBERS_PER_PAGE) + (slot - 10);
        if (memberIndex % 9 == 0 || memberIndex % 9 == 8) return; // 跳过边框
        
        plugin.getGuildService().getGuildMembersAsync(guild.getId()).thenAccept(members -> {
            if (members != null && memberIndex < members.size()) {
                GuildMember member = members.get(memberIndex);
                
                if (clickType == ClickType.LEFT) {
                    // 查看成员详情
                    showMemberDetails(player, member);
                } else if (clickType == ClickType.RIGHT) {
                    // 踢出成员
                    handleKickMemberDirect(player, member);
                } else if (clickType == ClickType.MIDDLE) {
                    // 提升/降级成员
                    handlePromoteDemoteMember(player, member);
                }
            }
        });
    }
    
    /**
     * 显示成员详情
     */
    private void showMemberDetails(Player player, GuildMember member) {
        // 打开成员详情GUI
        plugin.getGuiManager().openGUI(player, new MemberDetailsGUI(plugin, guild, member, player));
    }
    
    /**
     * 直接踢出成员
     */
    private void handleKickMemberDirect(Player player, GuildMember member) {
        // 检查权限
        plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(executor -> {
            if (executor == null || !executor.getRole().canKick()) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-permission", "&c权限不足");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            // 不能踢出会长
            if (member.getRole() == GuildMember.Role.LEADER) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.cannot-kick-leader", "&c不能踢出工会会长");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            // 确认踢出
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.confirm-kick", "&c确定要踢出成员 {member} 吗？输入 &f/guild kick {member} confirm &c确认")
                .replace("{member}", member.getPlayerName());
            player.sendMessage(ColorUtils.colorize(message));
        });
    }
    
    /**
     * 提升/降级成员
     */
    private void handlePromoteDemoteMember(Player player, GuildMember member) {
        // 检查权限
        plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(executor -> {
            if (executor == null || executor.getRole() != GuildMember.Role.LEADER) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&c只有工会会长才能执行此操作");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            // 不能操作会长
            if (member.getRole() == GuildMember.Role.LEADER) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.cannot-modify-leader", "&c不能修改工会会长的职位");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            if (member.getRole() == GuildMember.Role.OFFICER) {
                // 降级为普通成员
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.confirm-demote", "&c确定要降级成员 {member} 吗？输入 &f/guild demote {member} confirm &c确认")
                    .replace("{member}", member.getPlayerName());
                player.sendMessage(ColorUtils.colorize(message));
            } else {
                // 提升为官员
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.confirm-promote", "&a确定要提升成员 {member} 为官员吗？输入 &f/guild promote {member} confirm &a确认")
                    .replace("{member}", member.getPlayerName());
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    /**
     * 处理邀请成员
     */
    private void handleInviteMember(Player player) {
        // 检查权限
        plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(member -> {
            if (member == null || !member.getRole().canInvite()) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-permission", "&c权限不足");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            // 打开邀请成员GUI
            InviteMemberGUI inviteMemberGUI = new InviteMemberGUI(plugin, guild);
            plugin.getGuiManager().openGUI(player, inviteMemberGUI);
        });
    }
    
    /**
     * 处理踢出成员
     */
    private void handleKickMember(Player player) {
        // 检查权限
        plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(member -> {
            if (member == null || !member.getRole().canKick()) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-permission", "&c权限不足");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            // 打开踢出成员GUI
            KickMemberGUI kickMemberGUI = new KickMemberGUI(plugin, guild);
            plugin.getGuiManager().openGUI(player, kickMemberGUI);
        });
    }
    
    /**
     * 处理提升成员
     */
    private void handlePromoteMember(Player player) {
        // 检查权限
        plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(member -> {
            if (member == null || member.getRole() != GuildMember.Role.LEADER) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&c只有工会会长才能执行此操作");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            // 打开提升成员GUI
            PromoteMemberGUI promoteMemberGUI = new PromoteMemberGUI(plugin, guild);
            plugin.getGuiManager().openGUI(player, promoteMemberGUI);
        });
    }
    
    /**
     * 处理降级成员
     */
    private void handleDemoteMember(Player player) {
        // 检查权限
        plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(member -> {
            if (member == null || member.getRole() != GuildMember.Role.LEADER) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&c只有工会会长才能执行此操作");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            // 打开降级成员GUI
            DemoteMemberGUI demoteMemberGUI = new DemoteMemberGUI(plugin, guild);
            plugin.getGuiManager().openGUI(player, demoteMemberGUI);
        });
    }
    
    /**
     * 刷新库存
     */
    private void refreshInventory(Player player) {
        plugin.getGuiManager().refreshGUI(player);
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
