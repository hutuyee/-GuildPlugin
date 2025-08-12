package com.guild.core.permissions;

import com.guild.GuildPlugin;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.services.GuildService;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * 权限管理器 - 提供插件独立的权限功能
 */
public class PermissionManager {
    
    private final GuildPlugin plugin;
    private final Logger logger;
    private final Map<UUID, PlayerPermissions> playerPermissions = new HashMap<>();
    
    public PermissionManager(GuildPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    /**
     * 检查玩家是否有指定权限
     */
    public boolean hasPermission(Player player, String permission) {
        if (player == null || permission == null) {
            return false;
        }
        
        // 首先检查Bukkit权限系统
        if (player.hasPermission(permission)) {
            return true;
        }
        
        // 检查插件内置权限
        return hasInternalPermission(player, permission);
    }
    
    /**
     * 检查插件内置权限
     */
    private boolean hasInternalPermission(Player player, String permission) {
        UUID playerUuid = player.getUniqueId();
        
        // 获取玩家权限
        PlayerPermissions permissions = getPlayerPermissions(playerUuid);
        
        // 检查具体权限
        switch (permission) {
            case "guild.use":
                return true; // 所有玩家都可以使用工会系统
                
            case "guild.create":
                return permissions.canCreateGuild();
                
            case "guild.invite":
                return permissions.canInviteMembers();
                
            case "guild.kick":
                return permissions.canKickMembers();
                
            case "guild.delete":
                return permissions.canDeleteGuild();
                
            case "guild.admin":
                return permissions.isAdmin();
                
            default:
                return false;
        }
    }
    
    /**
     * 获取玩家权限
     */
    private PlayerPermissions getPlayerPermissions(UUID playerUuid) {
        return playerPermissions.computeIfAbsent(playerUuid, uuid -> {
            PlayerPermissions permissions = new PlayerPermissions();
            
            // 检查玩家是否在工会中
            GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
            if (guildService != null) {
                Guild guild = guildService.getPlayerGuild(uuid);
                if (guild != null) {
                    GuildMember member = guildService.getGuildMember(uuid);
                    if (member != null) {
                        // 根据角色设置权限
                        switch (member.getRole()) {
                            case LEADER:
                                permissions.setCanCreateGuild(true);
                                permissions.setCanInviteMembers(true);
                                permissions.setCanKickMembers(true);
                                permissions.setCanDeleteGuild(true);
                                permissions.setCanPromoteMembers(true);
                                permissions.setCanDemoteMembers(true);
                                break;
                            case OFFICER:
                                permissions.setCanInviteMembers(true);
                                permissions.setCanKickMembers(true);
                                permissions.setCanPromoteMembers(false);
                                permissions.setCanDemoteMembers(false);
                                break;
                            case MEMBER:
                                // 普通成员只有基本权限
                                break;
                        }
                    }
                }
            }
            
            return permissions;
        });
    }
    
    /**
     * 更新玩家权限（当工会状态改变时调用）
     */
    public void updatePlayerPermissions(UUID playerUuid) {
        playerPermissions.remove(playerUuid);
        // 重新计算权限
        getPlayerPermissions(playerUuid);
    }
    
    /**
     * 检查玩家是否可以邀请成员
     */
    public boolean canInviteMembers(Player player) {
        if (!hasPermission(player, "guild.invite")) {
            return false;
        }
        
        // 检查玩家是否在工会中
        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            return false;
        }
        
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            return false;
        }
        
        GuildMember member = guildService.getGuildMember(player.getUniqueId());
        return member != null && member.getRole().canInvite();
    }
    
    /**
     * 检查玩家是否可以踢出成员
     */
    public boolean canKickMembers(Player player) {
        if (!hasPermission(player, "guild.kick")) {
            return false;
        }
        
        // 检查玩家是否在工会中
        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            return false;
        }
        
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            return false;
        }
        
        GuildMember member = guildService.getGuildMember(player.getUniqueId());
        return member != null && member.getRole().canKick();
    }
    
    /**
     * 检查玩家是否可以删除工会
     */
    public boolean canDeleteGuild(Player player) {
        if (!hasPermission(player, "guild.delete")) {
            return false;
        }
        
        // 检查玩家是否在工会中
        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            return false;
        }
        
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            return false;
        }
        
        GuildMember member = guildService.getGuildMember(player.getUniqueId());
        return member != null && member.getRole().canDeleteGuild();
    }
    
    /**
     * 检查玩家是否可以创建工会
     */
    public boolean canCreateGuild(Player player) {
        if (!hasPermission(player, "guild.create")) {
            return false;
        }
        
        // 检查玩家是否已有工会
        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            return false;
        }
        
        return guildService.getPlayerGuild(player.getUniqueId()) == null;
    }
    
    /**
     * 玩家权限类
     */
    private static class PlayerPermissions {
        private boolean canCreateGuild = false;
        private boolean canInviteMembers = false;
        private boolean canKickMembers = false;
        private boolean canDeleteGuild = false;
        private boolean canPromoteMembers = false;
        private boolean canDemoteMembers = false;
        private boolean isAdmin = false;
        
        // Getters and Setters
        public boolean canCreateGuild() { return canCreateGuild; }
        public void setCanCreateGuild(boolean canCreateGuild) { this.canCreateGuild = canCreateGuild; }
        
        public boolean canInviteMembers() { return canInviteMembers; }
        public void setCanInviteMembers(boolean canInviteMembers) { this.canInviteMembers = canInviteMembers; }
        
        public boolean canKickMembers() { return canKickMembers; }
        public void setCanKickMembers(boolean canKickMembers) { this.canKickMembers = canKickMembers; }
        
        public boolean canDeleteGuild() { return canDeleteGuild; }
        public void setCanDeleteGuild(boolean canDeleteGuild) { this.canDeleteGuild = canDeleteGuild; }
        
        public boolean canPromoteMembers() { return canPromoteMembers; }
        public void setCanPromoteMembers(boolean canPromoteMembers) { this.canPromoteMembers = canPromoteMembers; }
        
        public boolean canDemoteMembers() { return canDemoteMembers; }
        public void setCanDemoteMembers(boolean canDemoteMembers) { this.canDemoteMembers = canDemoteMembers; }
        
        public boolean isAdmin() { return isAdmin; }
        public void setAdmin(boolean admin) { isAdmin = admin; }
    }
}
