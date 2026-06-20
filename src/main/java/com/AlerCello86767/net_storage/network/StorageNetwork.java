package com.AlerCello86767.net_storage.network;

import org.bukkit.entity.Player;

import java.util.*;

/**
 * 存储网络
 * 网络只是一个请求路由层，不实际存储任何物品
 * 所有物品数据都存储在磁盘中，网络只负责管理磁盘操纵器和终端的连接关系
 */
public class StorageNetwork {

    private final UUID networkId;
    private String name;
    private final UUID creatorUUID;
    private String creatorName;
    private long createdTime;
    private long lastModifiedTime;
    private final Set<UUID> nodes = new HashSet<>();
    private boolean isPublic = false;
    private String description = "";
    private boolean isDirty = false;

    // 构造函数（由Player创建）
    public StorageNetwork(String name, Player creator) {
        this.networkId = UUID.randomUUID();
        this.name = name;
        this.creatorUUID = creator.getUniqueId();
        this.creatorName = creator.getName();
        this.createdTime = System.currentTimeMillis();
        this.lastModifiedTime = System.currentTimeMillis();
        this.isDirty = true; // 新创建的网络需要被保存
    }

    // 构造函数（由UUID创建，用于从数据库加载）
    public StorageNetwork(UUID networkId, String name, UUID creatorUUID, String creatorName, long createdTime) {
        this.networkId = networkId;
        this.name = name;
        this.creatorUUID = creatorUUID;
        this.creatorName = creatorName;
        this.createdTime = createdTime;
        this.lastModifiedTime = System.currentTimeMillis();
    }

    // ========== Getters ==========

    public UUID getNetworkId() { return networkId; }
    public String getName() { return name; }
    public UUID getCreatorUUID() { return creatorUUID; }
    public String getCreatorName() { return creatorName; }
    public long getCreatedTime() { return createdTime; }
    public long getLastModifiedTime() { return lastModifiedTime; }
    public boolean isPublic() { return isPublic; }
    public String getDescription() { return description; }

    // ========== Setters ==========

    public void setName(String name) {
        if (name != null && !name.trim().isEmpty()) {
            this.name = name.trim();
            updateLastModified();
        }
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
        updateLastModified();
    }

    public void setDescription(String description) {
        this.description = description != null ? description : "";
        updateLastModified();
    }

    private void updateLastModified() {
        this.lastModifiedTime = System.currentTimeMillis();
        this.isDirty = true;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void setDirty(boolean dirty) {
        isDirty = dirty;
    }

    // ========== 节点管理 ==========

    public void addNode(UUID nodeId) {
        nodes.add(nodeId);
        updateLastModified();
    }

    public void removeNode(UUID nodeId) {
        nodes.remove(nodeId);
        updateLastModified();
    }

    public Set<UUID> getNodes() { return new HashSet<>(nodes); }
    public int getNodeCount() { return nodes.size(); }

    // ========== 工具方法 ==========

    /**
     * 检查玩家是否是网络创建者
     */
    public boolean isCreator(Player player) {
        return player.getUniqueId().equals(creatorUUID);
    }

    /**
     * 检查玩家是否有权限管理此网络
     * (创建者或OP)
     */
    public boolean hasManagePermission(Player player) {
        return isCreator(player) || player.isOp();
    }

    /**
     * 格式化创建时间
     */
    public String getFormattedCreatedTime() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new Date(createdTime));
    }

    /**
     * 格式化最后修改时间
     */
    public String getFormattedLastModifiedTime() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new Date(lastModifiedTime));
    }
}