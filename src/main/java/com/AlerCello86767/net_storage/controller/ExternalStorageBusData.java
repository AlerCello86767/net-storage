package com.AlerCello86767.net_storage.controller;

import com.AlerCello86767.net_storage.Net_storage;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

/**
 * 外部存储总线数据
 * 用于将原版容器（箱子、漏斗、熔炉等）连接到网络
 */
public class ExternalStorageBusData {
    
    public UUID busUuid;           // 总线唯一标识
    public String location;        // 方块位置
    public UUID networkId;         // 所属网络ID
    public String containerLocation; // 容器位置
    public String containerType;   // 容器类型
    
    /**
     * 构造函数（自动生成UUID）
     */
    public ExternalStorageBusData(String location, UUID networkId, String containerLocation, String containerType) {
        this.busUuid = UUID.randomUUID();
        this.location = location;
        this.networkId = networkId;
        this.containerLocation = containerLocation;
        this.containerType = containerType;
    }
    
    /**
     * 构造函数（指定UUID）
     */
    public ExternalStorageBusData(UUID busUuid, String location, UUID networkId, String containerLocation, String containerType) {
        this.busUuid = busUuid;
        this.location = location;
        this.networkId = networkId;
        this.containerLocation = containerLocation;
        this.containerType = containerType;
    }
    
    /**
     * 获取总线位置
     */
    public Location getLocation() {
        return stringToLocation(location);
    }
    
    /**
     * 获取容器位置
     */
    public Location getContainerLocation() {
        return stringToLocation(containerLocation);
    }
    
    /**
     * 字符串转位置
     */
    private Location stringToLocation(String locStr) {
        if (locStr == null) return null;
        
        String[] parts = locStr.split(",");
        if (parts.length != 4) return null;
        
        World world = Net_storage.getInstance().getServer().getWorld(parts[0]);
        if (world == null) return null;
        
        try {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            return new Location(world, x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * 获取容器类型显示名称
     */
    public String getContainerDisplayName() {
        if (containerType == null) {
            return "未知容器";
        }
        
        switch (containerType) {
            case "CHEST":
                return "箱子";
            case "TRAPPED_CHEST":
                return "陷阱箱";
            case "ENDER_CHEST":
                return "末影箱";
            case "HOPPER":
                return "漏斗";
            case "FURNACE":
                return "熔炉";
            case "BLAST_FURNACE":
                return "高炉";
            case "SMOKER":
                return "烟熏炉";
            case "DISPENSER":
                return "发射器";
            case "DROPPER":
                return "投掷器";
            case "SHULKER_BOX":
                return "潜影盒";
            default:
                return containerType;
        }
    }
}