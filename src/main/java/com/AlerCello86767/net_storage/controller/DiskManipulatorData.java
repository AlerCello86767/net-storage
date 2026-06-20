package com.AlerCello86767.net_storage.controller;

import java.util.UUID;

/**
 * 磁盘操纵器数据结构
 */
public class DiskManipulatorData {
    
    /** 方块位置字符串 */
    public String location;
    
    /** 所属网络ID */
    public UUID networkId;
    
    /** 磁盘槽位（8个），存储磁盘UUID，为null表示空槽 */
    public UUID[] slots;
    
    /** 创建时间 */
    public java.sql.Timestamp createdAt;
    
    public DiskManipulatorData() {
        this.slots = new UUID[8];
    }
    
    public DiskManipulatorData(String location, UUID networkId) {
        this.location = location;
        this.networkId = networkId;
        this.slots = new UUID[8];
    }
    
    /**
     * 获取已插入磁盘数量
     */
    public int getInsertedDiskCount() {
        int count = 0;
        for (UUID slot : slots) {
            if (slot != null) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * 获取空槽位数量
     */
    public int getEmptySlotCount() {
        return 8 - getInsertedDiskCount();
    }
    
    /**
     * 获取第一个空槽位索引，如果没有空槽位返回-1
     */
    public int getFirstEmptySlot() {
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == null) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * 插入磁盘到第一个空槽位
     * @return 槽位索引，-1表示没有空槽位
     */
    public int insertDisk(UUID diskUuid) {
        int slot = getFirstEmptySlot();
        if (slot >= 0) {
            slots[slot] = diskUuid;
        }
        return slot;
    }
    
    /**
     * 从指定槽位取出磁盘
     * @return 取出的磁盘UUID，null表示槽位为空
     */
    public UUID removeDisk(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.length) {
            return null;
        }
        UUID diskUuid = slots[slotIndex];
        slots[slotIndex] = null;
        return diskUuid;
    }
    
    /**
     * 检查指定槽位是否有磁盘
     */
    public boolean hasDisk(int slotIndex) {
        return slotIndex >= 0 && slotIndex < slots.length && slots[slotIndex] != null;
    }
}
