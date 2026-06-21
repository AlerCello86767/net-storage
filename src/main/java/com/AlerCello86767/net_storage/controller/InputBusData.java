package com.AlerCello86767.net_storage.controller;

import com.AlerCello86767.net_storage.Net_storage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * 输入总线数据
 * 用于将原版容器中的物品自动传输到网络存储中
 */
public class InputBusData {

    public UUID busUuid;           // 总线唯一标识
    public String location;        // 方块位置
    public UUID networkId;         // 所属网络ID
    public String containerLocation; // 容器位置
    public String containerType;   // 容器类型

    // 过滤相关设置
    public List<String> filterItems;  // 过滤物品列表（序列化后的物品，Base64格式）
    public boolean whitelistMode;     // true=白名单 false=黑名单
    public boolean nbtMatching;       // true=启用NBT匹配 false=忽略NBT

    /**
     * 构造函数（指定UUID）
     */
    public InputBusData(UUID busUuid, String location, UUID networkId, String containerLocation, String containerType) {
        this.busUuid = busUuid;
        this.location = location;
        this.networkId = networkId;
        this.containerLocation = containerLocation;
        this.containerType = containerType;
        this.filterItems = new ArrayList<>();
        this.whitelistMode = true;
        this.nbtMatching = false;
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

    /**
     * 检查物品是否应该被过滤
     * @param item 要检查的物品
     * @return true=应该过滤(不传输) false=不应该过滤(传输)
     */
    public boolean shouldFilter(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return true; // 空气物品应该被过滤
        }

        // 如果过滤列表为空，不过滤任何物品
        if (filterItems == null || filterItems.isEmpty()) {
            return false;
        }

        // 检查物品是否在过滤列表中
        boolean inFilterList = isItemInFilterList(item);

        // 白名单模式：只在列表中的物品才传输
        // 黑名单模式：排除列表中的物品
        if (whitelistMode) {
            return !inFilterList; // 白名单：不在列表中→过滤
        } else {
            return inFilterList;  // 黑名单：在列表中→过滤
        }
    }

    /**
     * 检查物品是否在过滤列表中
     */
    private boolean isItemInFilterList(ItemStack item) {
        if (filterItems == null || filterItems.isEmpty()) {
            return false;
        }

        for (String serialized : filterItems) {
            if (serialized == null || serialized.isEmpty()) continue;

            try {
                ItemStack filterItem = deserializeItemStack(serialized);
                if (filterItem == null) continue;

                if (nbtMatching) {
                    // 启用NBT匹配：完全匹配（包括NBT）
                    if (filterItem.isSimilar(item)) {
                        return true;
                    }
                } else {
                    // 忽略NBT匹配：只比较材质和数据值
                    if (filterItem.getType() == item.getType() &&
                            filterItem.getDurability() == item.getDurability()) {
                        return true;
                    }
                }
            } catch (Exception e) {
                // 忽略反序列化错误
            }
        }

        return false;
    }

    // ==================== NBT 序列化/反序列化（完整NBT） ====================

    /**
     * 序列化物品为 Base64 字符串（完整NBT）
     * @param item 要序列化的物品
     * @return Base64 字符串
     */
    public static String serializeItemStack(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return "";
        }

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(item);
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 反序列化 Base64 字符串为 ItemStack（完整NBT）
     * @param data Base64 字符串
     * @return ItemStack 物品
     */
    public static ItemStack deserializeItemStack(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }

        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack item = (ItemStack) dataInput.readObject();
            dataInput.close();
            return item;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 序列化物品（兼容旧格式，调用新方法）
     * @deprecated 请使用 {@link #serializeItemStack(ItemStack)}
     */
    @Deprecated
    public static String serializeItemSimple(ItemStack item) {
        return serializeItemStack(item);
    }
}