package com.guild.models;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 工会贡献记录数据模型
 */
public class GuildContribution {
    
    private int id;
    private int guildId;
    private UUID playerUuid;
    private String playerName;
    private double amount;
    private ContributionType type;
    private String description;
    private LocalDateTime createdAt;
    
    public enum ContributionType {
        DEPOSIT("存款"),
        WITHDRAW("取款"),
        TRANSFER("转账"),
        CREATION("创建工会"),
        UPGRADE("升级工会"),
        ADMIN("管理员操作");
        
        private final String displayName;
        
        ContributionType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public GuildContribution() {}
    
    public GuildContribution(int guildId, UUID playerUuid, String playerName, 
                           double amount, ContributionType type, String description) {
        this.guildId = guildId;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.amount = amount;
        this.type = type;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getGuildId() {
        return guildId;
    }
    
    public void setGuildId(int guildId) {
        this.guildId = guildId;
    }
    
    public UUID getPlayerUuid() {
        return playerUuid;
    }
    
    public void setPlayerUuid(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public void setAmount(double amount) {
        this.amount = amount;
    }
    
    public ContributionType getType() {
        return type;
    }
    
    public void setType(ContributionType type) {
        this.type = type;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * 获取总贡献（存款 - 取款）
     */
    public double getNetContribution() {
        if (type == ContributionType.WITHDRAW) {
            return -amount;
        }
        return amount;
    }
    
    @Override
    public String toString() {
        return "GuildContribution{" +
                "id=" + id +
                ", guildId=" + guildId +
                ", playerName='" + playerName + '\'' +
                ", amount=" + amount +
                ", type=" + type +
                ", description='" + description + '\'' +
                '}';
    }
}
