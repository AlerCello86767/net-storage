package com.AlerCello86767.net_storage.disk;

import com.AlerCello86767.net_storage.Net_storage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 磁盘管理器
 * 管理磁盘的创建、读取、保存和物品操作
 * 使用缓存减少数据库查询
 */
public class DiskManager {

    private final Net_storage plugin;
    
    // 磁盘数据缓存 - 使用 ConcurrentHashMap 保证并发安全
    private final ConcurrentHashMap<UUID, List<DiskItem>> diskCache = new ConcurrentHashMap<>();
    
    // 磁盘类型缓存
    private final ConcurrentHashMap<UUID, String> diskTypeCache = new ConcurrentHashMap<>();
    
    // 待保存的磁盘队列 - 用于批量异步保存
    private final ConcurrentHashMap<UUID, List<DiskItem>> pendingSaves = new ConcurrentHashMap<>();
    
    // 自定义Gson，处理DiskItem序列化
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(DiskItem.class, new DiskItemSerializer())
            .registerTypeAdapter(DiskItem.class, new DiskItemDeserializer())
            .create();
    
    private final Type diskItemListType = new TypeToken<List<DiskItem>>(){}.getType();
    
    // PDC 键名
    private final NamespacedKey DISK_UUID_KEY;
    
    // 磁盘容量（从配置读取）
    private int maxCapacity;
    
    // 磁盘类型配置
    private final java.util.Map<String, DiskTypeConfig> diskTypes = new java.util.HashMap<>();
    
    public DiskManager(Net_storage plugin) {
        this.plugin = plugin;
        this.DISK_UUID_KEY = new NamespacedKey(plugin, "disk_uuid");
        this.maxCapacity = plugin.getConfigManager().getConfig()
                .getInt("disk.max-capacity", 1024);
        loadDiskTypes();
    }
    
    private void loadDiskTypes() {
        diskTypes.clear();
        
        DiskTypeConfig config1k = new DiskTypeConfig();
        config1k.capacity = 1024;
        config1k.displayName = "&b1K 存储磁盘";
        config1k.customModelData = 1001;
        diskTypes.put("disk_1k", config1k);
        
        DiskTypeConfig config4k = new DiskTypeConfig();
        config4k.capacity = 4096;
        config4k.displayName = "&c4K 存储磁盘";
        config4k.customModelData = 1002;
        diskTypes.put("disk_4k", config4k);
        
        DiskTypeConfig config16k = new DiskTypeConfig();
        config16k.capacity = 16384;
        config16k.displayName = "&616K 存储磁盘";
        config16k.customModelData = 1003;
        diskTypes.put("disk_16k", config16k);
    }
    
    private DiskTypeConfig getDiskTypeConfig(String diskType) {
        return diskTypes.getOrDefault(diskType, diskTypes.get("disk_1k"));
    }
    
    private static class DiskTypeConfig {
        int capacity;
        String displayName;
        int customModelData;
    }
    
    /**
     * 创建新的磁盘物品
     */
    public ItemStack createDiskItem(String diskType) {
        DiskTypeConfig config = getDiskTypeConfig(diskType);
        
        String materialName = plugin.getConfigManager().getConfig()
                .getString("disk.item-material", "MAP");
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            material = Material.MAP;
        }
        
        String displayName = ChatColor.translateAlternateColorCodes('&', config.displayName);
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta == null) return item;
        
        meta.setDisplayName(displayName);
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "容量: " + ChatColor.WHITE + config.capacity + " 物品");
        lore.add(ChatColor.GRAY + "使用: " + ChatColor.WHITE + "0/" + config.capacity);
        lore.add(ChatColor.DARK_GRAY + "类型: " + diskType);
        lore.add("");
        lore.add(ChatColor.YELLOW + "使用 /netdebug disktest 操作");
        meta.setLore(lore);
        
        meta.setCustomModelData(config.customModelData);
        
        UUID diskUuid = UUID.randomUUID();
        meta.getPersistentDataContainer().set(DISK_UUID_KEY, PersistentDataType.STRING, diskUuid.toString());
        
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "item_type"),
                PersistentDataType.STRING,
                diskType
        );
        
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "disk_capacity"),
                PersistentDataType.INTEGER,
                config.capacity
        );
        
        item.setItemMeta(meta);
        
        plugin.getDatabaseManager().saveDiskToDB(diskUuid, "[]", diskType);
        
        diskTypeCache.put(diskUuid, diskType);
        
        return item;
    }
    
    /**
     * 从物品获取磁盘 UUID
     */
    public UUID getDiskUuidFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        
        if (!pdc.has(DISK_UUID_KEY, PersistentDataType.STRING)) {
            return null;
        }
        
        String uuidStr = pdc.get(DISK_UUID_KEY, PersistentDataType.STRING);
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * 检查物品是否是磁盘
     */
    public boolean isDisk(ItemStack item) {
        return getDiskUuidFromItem(item) != null;
    }
    
    /**
     * 从 UUID 获取磁盘物品（用于显示）
     * 注意：这个物品只是一个显示用的副本，不能直接使用
     */
    public ItemStack getDiskItemFromUUID(UUID diskUuid) {
        if (diskUuid == null) {
            return null;
        }
        
        String jsonData = plugin.getDatabaseManager().loadDiskFromDB(diskUuid);
        
        String diskType = diskTypeCache.getOrDefault(diskUuid, null);
        if (diskType == null) {
            diskType = plugin.getDatabaseManager().loadDiskTypeFromDB(diskUuid);
            diskTypeCache.put(diskUuid, diskType);
        }
        
        String materialName = plugin.getConfigManager().getConfig()
                .getString("disk.item-material", "MAP");
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            material = Material.MAP;
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta == null) return item;
        
        int usedItems = 0;
        if (jsonData != null && !jsonData.isEmpty()) {
            try {
                List<DiskItem> items = gson.fromJson(jsonData, diskItemListType);
                if (items != null) {
                    usedItems = getTotalItems(items);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("解析磁盘数据失败: " + e.getMessage());
            }
        }
        
        DiskTypeConfig config = getDiskTypeConfig(diskType);
        
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', config.displayName));
        meta.setCustomModelData(config.customModelData);
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "容量: " + ChatColor.WHITE + config.capacity + " 物品");
        lore.add(ChatColor.GRAY + "使用: " + ChatColor.WHITE + usedItems + "/" + config.capacity);
        lore.add(ChatColor.DARK_GRAY + "类型: " + diskType);
        lore.add("");
        lore.add(ChatColor.YELLOW + "使用 /netdebug disktest 操作");
        meta.setLore(lore);
        
        meta.getPersistentDataContainer().set(DISK_UUID_KEY, PersistentDataType.STRING, diskUuid.toString());
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "item_type"),
                PersistentDataType.STRING,
                diskType
        );
        
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * 获取磁盘数据（优先使用缓存）
     */
    public List<DiskItem> getDiskData(UUID diskUuid) {
        List<DiskItem> cached = diskCache.get(diskUuid);
        if (cached != null) {
            return new ArrayList<>(cached);
        }
        
        String jsonData = plugin.getDatabaseManager().loadDiskFromDB(diskUuid);
        
        String diskType = diskTypeCache.getOrDefault(diskUuid, null);
        if (diskType == null) {
            diskType = plugin.getDatabaseManager().loadDiskTypeFromDB(diskUuid);
            diskTypeCache.put(diskUuid, diskType);
        }
        
        if (jsonData == null || jsonData.isEmpty()) {
            List<DiskItem> emptyList = new ArrayList<>();
            diskCache.put(diskUuid, emptyList);
            return emptyList;
        }
        
        try {
            List<DiskItem> items = gson.fromJson(jsonData, diskItemListType);
            if (items == null) {
                items = new ArrayList<>();
            }
            diskCache.put(diskUuid, items);
            return new ArrayList<>(items);
        } catch (Exception e) {
            plugin.getLogger().warning("解析磁盘数据失败: " + e.getMessage());
            List<DiskItem> emptyList = new ArrayList<>();
            diskCache.put(diskUuid, emptyList);
            return emptyList;
        }
    }
    
    /**
     * 获取磁盘类型
     */
    public String getDiskType(UUID diskUuid) {
        String diskType = diskTypeCache.get(diskUuid);
        if (diskType != null) {
            return diskType;
        }
        diskType = plugin.getDatabaseManager().loadDiskTypeFromDB(diskUuid);
        diskTypeCache.put(diskUuid, diskType);
        return diskType;
    }
    
    /**
     * 保存磁盘数据（更新缓存并异步保存到数据库）
     */
    public void saveDiskData(UUID diskUuid, List<DiskItem> items) {
        diskCache.put(diskUuid, new ArrayList<>(items));
        
        String diskType = diskTypeCache.getOrDefault(diskUuid, "disk_1k");
        
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String jsonData = gson.toJson(items);
            plugin.getDatabaseManager().saveDiskToDB(diskUuid, jsonData, diskType);
        });
    }
    
    /**
     * 批量保存磁盘数据（异步）
     */
    public void saveAllDiskData() {
        if (diskCache.isEmpty()) {
            return;
        }
        
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            saveAllDiskDataSync();
        });
    }
    
    /**
     * 批量保存磁盘数据（同步，用于插件禁用时）
     */
    public void saveAllDiskDataSync() {
        if (diskCache.isEmpty()) {
            return;
        }
        
        for (ConcurrentHashMap.Entry<UUID, List<DiskItem>> entry : diskCache.entrySet()) {
            UUID diskUuid = entry.getKey();
            String jsonData = gson.toJson(entry.getValue());
            String diskType = diskTypeCache.getOrDefault(diskUuid, "disk_1k");
            plugin.getDatabaseManager().saveDiskToDB(diskUuid, jsonData, diskType);
        }
        plugin.getLogger().info("批量保存磁盘数据完成，共 " + diskCache.size() + " 个磁盘");
    }
    
    /**
     * 清除磁盘缓存（用于强制刷新）
     */
    public void clearCache(UUID diskUuid) {
        diskCache.remove(diskUuid);
        diskTypeCache.remove(diskUuid);
    }
    
    /**
     * 清除所有缓存
     */
    public void clearAllCache() {
        diskCache.clear();
        diskTypeCache.clear();
    }
    
    /**
     * 获取缓存大小
     */
    public int getCacheSize() {
        return diskCache.size();
    }
    
    /**
     * 计算磁盘物品总数
     */
    public int getTotalItems(List<DiskItem> items) {
        return items.stream().mapToInt(DiskItem::getAmount).sum();
    }
    
    /**
     * 检查磁盘是否有足够空间
     */
    public boolean hasCapacity(List<DiskItem> items, int additionalAmount) {
        return getTotalItems(items) + additionalAmount <= maxCapacity;
    }
    
    /**
     * 查找磁盘中是否存在与给定ItemStack相同的物品（用于堆叠）
     */
    public DiskItem findMatchingItem(List<DiskItem> items, ItemStack itemStack) {
        for (DiskItem item : items) {
            if (item.matchesItemStack(itemStack)) {
                return item;
            }
        }
        return null;
    }
    
    /**
     * 查找磁盘中是否存在指定物品（通过名称匹配）
     */
    public DiskItem findItemByName(List<DiskItem> items, String itemName) {
        for (DiskItem item : items) {
            // 尝试匹配材质名称
            if (item.getMaterial().name().equalsIgnoreCase(itemName)) {
                return item;
            }
            // 尝试匹配显示名称（去除颜色代码）
            if (item.getDisplayName() != null) {
                String cleanDisplayName = ChatColor.stripColor(item.getDisplayName());
                if (cleanDisplayName.equalsIgnoreCase(itemName)) {
                    return item;
                }
            }
        }
        return null;
    }
    
    /**
     * 添加物品到磁盘（自动堆叠相同物品）
     * @return 添加的物品数量，-1 表示失败
     */
    public int addItem(List<DiskItem> items, ItemStack itemStack, int amount) {
        // 检查容量
        if (!hasCapacity(items, amount)) {
            return -1;
        }
        
        // 查找是否已存在相同物品（相同NBT数据）
        DiskItem existing = findMatchingItem(items, itemStack);
        
        if (existing != null) {
            // 已存在相同物品，增加数量（堆叠）
            existing.addAmount(amount);
        } else {
            // 不存在，添加新物品（包含完整NBT数据）
            items.add(DiskItem.fromItemStack(itemStack, amount));
        }
        
        return amount;
    }
    
    /**
     * 从磁盘移除物品（通过ItemStack匹配）
     * @return 移除的物品数量，-1 表示失败
     */
    public int removeItem(List<DiskItem> items, ItemStack itemStack) {
        DiskItem existing = findMatchingItem(items, itemStack);
        if (existing == null) {
            return -1;
        }
        
        int removedAmount = existing.getAmount();
        items.remove(existing);
        
        return removedAmount;
    }
    
    /**
     * 通过名称移除物品
     * @return 移除的物品数量，-1 表示失败
     */
    public int removeItemByName(List<DiskItem> items, String itemName) {
        DiskItem existing = findItemByName(items, itemName);
        if (existing == null) {
            return -1;
        }
        
        int removedAmount = existing.getAmount();
        items.remove(existing);
        
        return removedAmount;
    }
    
    /**
     * 清空磁盘
     */
    public void clearDisk(List<DiskItem> items) {
        items.clear();
    }
    
    /**
     * 更新磁盘物品的 Lore（显示当前使用量）
     */
    public void updateDiskLore(ItemStack diskItem, List<DiskItem> items) {
        if (diskItem == null || !diskItem.hasItemMeta()) {
            return;
        }
        
        ItemMeta meta = diskItem.getItemMeta();
        int totalItems = getTotalItems(items);
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "容量: " + ChatColor.WHITE + maxCapacity + " 物品");
        lore.add(ChatColor.GRAY + "使用: " + ChatColor.WHITE + totalItems + "/" + maxCapacity);
        lore.add(ChatColor.DARK_GRAY + "物品种类: " + items.size());
        lore.add("");
        lore.add(ChatColor.YELLOW + "使用 /netdebug disktest 操作");
        
        meta.setLore(lore);
        diskItem.setItemMeta(meta);
    }
    
    /**
     * 获取最大容量
     */
    public int getMaxCapacity() {
        return maxCapacity;
    }
    
    /**
     * 获取指定磁盘类型的容量
     */
    public int getDiskCapacity(String diskType) {
        DiskTypeConfig config = getDiskTypeConfig(diskType);
        return config != null ? config.capacity : maxCapacity;
    }
    
    /**
     * 获取指定磁盘UUID的容量
     */
    public int getDiskCapacity(UUID diskUuid) {
        String diskType = getDiskType(diskUuid);
        return getDiskCapacity(diskType);
    }
    
    /**
     * 格式化磁盘内容显示
     */
    public String formatDiskContents(UUID diskUuid, List<DiskItem> items) {
        StringBuilder sb = new StringBuilder();
        sb.append(ChatColor.GREEN).append("===== 磁盘内容 =====\n");
        sb.append(ChatColor.GRAY).append("UUID: ").append(ChatColor.WHITE).append(diskUuid).append("\n");
        sb.append(ChatColor.GRAY).append("使用量: ").append(ChatColor.WHITE)
                .append(getTotalItems(items)).append("/").append(maxCapacity).append("\n");
        sb.append(ChatColor.GRAY).append("物品种类: ").append(ChatColor.WHITE).append(items.size()).append("\n");
        sb.append("\n");
        
        if (items.isEmpty()) {
            sb.append(ChatColor.YELLOW).append("磁盘为空");
        } else {
            sb.append(ChatColor.AQUA).append("物品列表:\n");
            int index = 1;
            for (DiskItem item : items) {
                sb.append(ChatColor.WHITE).append(index).append(". ")
                        .append(item.getDisplayString())
                        .append(ChatColor.GRAY).append(" x").append(item.getAmount())
                        .append("\n");
                index++;
            }
        }
        
        return sb.toString();
    }
    
    /**
     * DiskItem JSON 序列化器
     */
    private static class DiskItemSerializer implements JsonSerializer<DiskItem> {
        @Override
        public JsonElement serialize(DiskItem src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            json.addProperty("serializedItem", src.getSerializedItem());
            json.addProperty("amount", src.getAmount());
            json.addProperty("material", src.getMaterial().name());
            json.addProperty("displayName", src.getDisplayName());
            return json;
        }
    }
    
    /**
     * DiskItem JSON 反序列化器
     */
    private static class DiskItemDeserializer implements JsonDeserializer<DiskItem> {
        @Override
        public DiskItem deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) 
                throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            
            String serializedItem = obj.get("serializedItem").getAsString();
            int amount = obj.get("amount").getAsInt();
            Material material = Material.matchMaterial(obj.get("material").getAsString());
            String displayName = obj.has("displayName") && !obj.get("displayName").isJsonNull() 
                    ? obj.get("displayName").getAsString() : null;
            
            return new DiskItem(serializedItem, amount, material, displayName);
        }
    }
}