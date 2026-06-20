package com.AlerCello86767.net_storage.disk;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * 磁盘物品数据类
 * 用于存储磁盘中的单个物品条目，包含完整的NBT数据
 */
public class DiskItem {
    
    // 存储完整的ItemStack序列化数据（包含所有NBT属性）
    private final String serializedItem;
    
    // 物品数量（单独存储，用于堆叠）
    private int amount;
    
    // 快速访问字段（用于匹配和显示）
    private final Material material;
    private final String displayName;
    
    // 关联的磁盘UUID（用于终端取出物品）
    private UUID diskUuid;
    
    /**
     * 创建 DiskItem
     * @param serializedItem 序列化的ItemStack数据（不含数量）
     * @param amount 物品数量
     * @param material 物品材质
     * @param displayName 显示名称
     */
    public DiskItem(String serializedItem, int amount, Material material, String displayName) {
        this.serializedItem = serializedItem;
        this.amount = amount;
        this.material = material;
        this.displayName = displayName;
        this.diskUuid = null;
    }
    
    /**
     * 获取关联的磁盘UUID
     */
    public UUID getDiskUuid() {
        return diskUuid;
    }
    
    /**
     * 设置关联的磁盘UUID
     */
    public void setDiskUuid(UUID diskUuid) {
        this.diskUuid = diskUuid;
    }
    
    // 外部存储总线位置（用于表示容器物品）
    private String externalBus;
    
    // 容器槽位索引（用于表示容器物品）
    private int slotIndex;
    
    /**
     * 获取外部存储总线位置
     */
    public String getExternalBus() {
        return externalBus;
    }
    
    /**
     * 设置外部存储总线位置
     */
    public void setExternalBus(String externalBus) {
        this.externalBus = externalBus;
    }
    
    /**
     * 获取容器槽位索引
     */
    public int getSlotIndex() {
        return slotIndex;
    }
    
    /**
     * 设置容器槽位索引
     */
    public void setSlotIndex(int slotIndex) {
        this.slotIndex = slotIndex;
    }
    
    /**
     * 检查是否是外部容器物品
     */
    public boolean isExternalItem() {
        return externalBus != null;
    }
    
    /**
     * 从 ItemStack 创建 DiskItem（存储完整NBT数据）
     */
    public static DiskItem fromItemStack(ItemStack itemStack, int amount) {
        // 创建一个数量为1的副本用于序列化
        ItemStack toSerialize = itemStack.clone();
        toSerialize.setAmount(1);
        
        // 序列化ItemStack（包含所有NBT数据）
        String serialized = serializeItemStack(toSerialize);
        
        // 提取快速访问字段
        Material material = itemStack.getType();
        String displayName = null;
        if (itemStack.hasItemMeta()) {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta.hasDisplayName()) {
                displayName = meta.getDisplayName();
            }
        }
        
        return new DiskItem(serialized, amount, material, displayName);
    }
    
    /**
     * 序列化 ItemStack 到 Base64 字符串
     */
    private static String serializeItemStack(ItemStack item) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            
            dataOutput.writeObject(item);
            dataOutput.close();
            
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("序列化物品失败", e);
        }
    }
    
    /**
     * 从 Base64 字符串反序列化 ItemStack
     */
    private static ItemStack deserializeItemStack(String data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            
            ItemStack item = (ItemStack) dataInput.readObject();
            dataInput.close();
            
            return item;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("反序列化物品失败", e);
        }
    }
    
    /**
     * 转换为 ItemStack（包含完整NBT数据）
     */
    public ItemStack toItemStack() {
        ItemStack item = deserializeItemStack(serializedItem);
        item.setAmount(amount);
        return item;
    }
    
    /**
     * 获取物品显示名称（用于消息显示）
     */
    public String getDisplayString() {
        if (displayName != null) {
            return displayName + " (" + material.name() + ")";
        }
        return material.name();
    }
    
    /**
     * 检查两个 DiskItem 是否可以堆叠（相同材质和NBT数据）
     */
    public boolean canStackWith(DiskItem other) {
        // 比较序列化数据（不含数量）
        return this.serializedItem.equals(other.serializedItem);
    }
    
    /**
     * 检查是否与给定的 ItemStack 相同（可堆叠）
     */
    public boolean matchesItemStack(ItemStack itemStack) {
        ItemStack toCompare = itemStack.clone();
        toCompare.setAmount(1);
        String serialized = serializeItemStack(toCompare);
        return this.serializedItem.equals(serialized);
    }
    
    // Getters
    public String getSerializedItem() {
        return serializedItem;
    }
    
    public Material getMaterial() {
        return material;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getAmount() {
        return amount;
    }
    
    // Setter
    public void setAmount(int amount) {
        this.amount = amount;
    }
    
    /**
     * 增加数量
     */
    public void addAmount(int additional) {
        this.amount += additional;
    }
    
    /**
     * 减少数量
     */
    public void reduceAmount(int reduction) {
        this.amount -= reduction;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        DiskItem other = (DiskItem) obj;
        // 比较序列化数据（决定是否可堆叠）
        return serializedItem.equals(other.serializedItem);
    }
    
    @Override
    public int hashCode() {
        return serializedItem.hashCode();
    }
}