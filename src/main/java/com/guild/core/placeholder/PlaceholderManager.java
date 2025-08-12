package com.guild.core.placeholder;

import com.guild.GuildPlugin;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.services.GuildService;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/**
 * 占位符管理器 - 管理PlaceholderAPI集成
 */
public class PlaceholderManager {
    
    private final GuildPlugin plugin;
    private GuildService guildService;
    private Object placeholderExpansion;
    private boolean placeholderApiAvailable = false;
    
    public PlaceholderManager(GuildPlugin plugin) {
        this.plugin = plugin;
        this.guildService = null; // 临时设置为null，避免循环依赖
    }
    
    /**
     * 设置工会服务（在服务容器初始化后调用）
     */
    public void setGuildService(GuildService guildService) {
        this.guildService = guildService;
    }
    
    /**
     * 注册占位符
     */
    public void registerPlaceholders() {
        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                // 使用反射创建 PlaceholderExpansion 实例
                Class<?> expansionClass = Class.forName("me.clip.placeholderapi.expansion.PlaceholderExpansion");
                placeholderExpansion = createPlaceholderExpansion(expansionClass);
                
                if (placeholderExpansion != null) {
                    // 注册占位符
                    Method registerMethod = expansionClass.getMethod("register");
                    registerMethod.invoke(placeholderExpansion);
                    placeholderApiAvailable = true;
                    plugin.getLogger().info("PlaceholderAPI 占位符注册成功");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("PlaceholderAPI 初始化失败: " + e.getMessage());
                placeholderApiAvailable = false;
            }
        } else {
            plugin.getLogger().warning("PlaceholderAPI 未找到，占位符功能将不可用");
            placeholderApiAvailable = false;
        }
    }
    
    /**
     * 创建 PlaceholderExpansion 实例
     */
    private Object createPlaceholderExpansion(Class<?> expansionClass) {
        try {
            // 创建一个匿名内部类来继承 PlaceholderExpansion
            return java.lang.reflect.Proxy.newProxyInstance(
                expansionClass.getClassLoader(),
                new Class<?>[] { expansionClass },
                (proxy, method, args) -> {
                    String methodName = method.getName();
                    
                    switch (methodName) {
                        case "getIdentifier":
                            return "guild";
                        case "getAuthor":
                            return "GuildTeam";
                        case "getVersion":
                            return plugin.getDescription().getVersion();
                        case "persist":
                            return true;
                        case "onPlaceholderRequest":
                            if (args.length >= 2 && args[0] instanceof Player && args[1] instanceof String) {
                                return handlePlaceholderRequest((Player) args[0], (String) args[1]);
                            }
                            return "";
                        default:
                            return null;
                    }
                }
            );
        } catch (Exception e) {
            plugin.getLogger().warning("创建 PlaceholderExpansion 实例失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 处理占位符请求
     */
    private String handlePlaceholderRequest(Player player, String params) {
        if (player == null) {
            return "";
        }
        
        String[] args = params.split("_");
        if (args.length == 0) {
            return "";
        }
        
        try {
            switch (args[0].toLowerCase()) {
                case "name":
                    return getGuildName(player);
                case "tag":
                    return getGuildTag(player);
                case "description":
                    return getGuildDescription(player);
                case "leader":
                    return getGuildLeader(player);
                case "membercount":
                    return getGuildMemberCount(player);
                case "role":
                    return getPlayerRole(player);
                case "hasguild":
                    return hasGuild(player);
                case "isleader":
                    return isLeader(player);
                case "isofficer":
                    return isOfficer(player);
                default:
                    return "";
            }
        } catch (Exception e) {
            plugin.getLogger().warning("处理占位符时发生错误: " + e.getMessage());
            return "";
        }
    }
    
    /**
     * 获取工会名称
     */
    private String getGuildName(Player player) {
        if (guildService == null) {
            return "无工会";
        }
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        return guild != null ? guild.getName() : "无工会";
    }
    
    /**
     * 获取工会标签
     */
    private String getGuildTag(Player player) {
        if (guildService == null) {
            return "";
        }
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        return guild != null ? guild.getTag() : "";
    }
    
    /**
     * 获取工会描述
     */
    private String getGuildDescription(Player player) {
        if (guildService == null) {
            return "";
        }
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        return guild != null ? guild.getDescription() : "";
    }
    
    /**
     * 获取工会会长
     */
    private String getGuildLeader(Player player) {
        if (guildService == null) {
            return "";
        }
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        return guild != null ? guild.getLeaderName() : "";
    }
    
    /**
     * 获取工会成员数量
     */
    private String getGuildMemberCount(Player player) {
        if (guildService == null) {
            return "0";
        }
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        return guild != null ? String.valueOf(guildService.getGuildMemberCount(guild.getId())) : "0";
    }
    
    /**
     * 获取玩家在工会中的角色
     */
    private String getPlayerRole(Player player) {
        if (guildService == null) {
            return "";
        }
        GuildMember member = guildService.getGuildMember(player.getUniqueId());
        return member != null ? member.getRole().getDisplayName() : "";
    }
    
    /**
     * 检查玩家是否有工会
     */
    private String hasGuild(Player player) {
        if (guildService == null) {
            return "否";
        }
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        return guild != null ? "是" : "否";
    }
    
    /**
     * 检查玩家是否是会长
     */
    private String isLeader(Player player) {
        if (guildService == null) {
            return "否";
        }
        GuildMember member = guildService.getGuildMember(player.getUniqueId());
        return member != null && member.getRole() == GuildMember.Role.LEADER ? "是" : "否";
    }
    
    /**
     * 检查玩家是否是官员
     */
    private String isOfficer(Player player) {
        if (guildService == null) {
            return "否";
        }
        GuildMember member = guildService.getGuildMember(player.getUniqueId());
        return member != null && member.getRole() == GuildMember.Role.OFFICER ? "是" : "否";
    }
    
    /**
     * 检查 PlaceholderAPI 是否可用
     */
    public boolean isPlaceholderApiAvailable() {
        return placeholderApiAvailable;
    }
}
