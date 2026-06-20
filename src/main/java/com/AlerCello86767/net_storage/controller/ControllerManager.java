package com.AlerCello86767.net_storage.controller;

import com.AlerCello86767.net_storage.Net_storage;
import com.AlerCello86767.net_storage.commands.ControllerCommand;
import com.AlerCello86767.net_storage.network.StorageNetwork;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ControllerManager {

    private final Net_storage plugin;
    private final Map<String, UUID> controllers = new HashMap<>();
    private final Map<UUID, Set<String>> networkControllers = new HashMap<>();
    
    // 调试子设备位置 -> 网络ID
    private final Map<String, UUID> debugDevices = new HashMap<>();
    
    // 磁盘操纵器位置 -> 数据
    private final Map<String, DiskManipulatorData> diskManipulators = new HashMap<>();
    
    // 终端位置 -> 数据
    private final Map<String, TerminalData> terminals = new HashMap<>();
    
    // 连接工具：玩家UUID -> 选择的网络UUID
    private final Map<UUID, UUID> selectedNetwork = new HashMap<>();
    
    // Action Bar 定时显示任务
    private BukkitTask actionBarTask;

    public ControllerManager(Net_storage plugin) {
        this.plugin = plugin;
    }

    /**
     * 启动 Action Bar 定时显示任务（每2秒更新一次）
     */
    public void startActionBarTask() {
        actionBarTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                // 检查是否已选择网络
                if (!hasSelectedNetwork(player.getUniqueId())) {
                    continue;
                }

                // 检查是否手持连接工具
                ItemStack handItem = player.getInventory().getItemInMainHand();
                String itemType = ControllerCommand.getItemType(plugin, handItem);
                if (!"connect_tool".equals(itemType)) {
                    continue;
                }

                // 显示已选择的网络名称
                UUID networkId = getSelectedNetwork(player.getUniqueId());
                StorageNetwork network = plugin.getNetworkManager().getNetwork(networkId);
                if (network != null) {
                    player.sendActionBar(ChatColor.GREEN + "已选择网络: " + ChatColor.WHITE + network.getName());
                }
            }
        }, 40L, 40L); // 40 ticks = 2秒
    }

    /**
     * 停止 Action Bar 定时显示任务
     */
    public void stopActionBarTask() {
        if (actionBarTask != null) {
            actionBarTask.cancel();
            actionBarTask = null;
        }
    }

    public String locationToString(Location location) {
        return location.getWorld().getName() + "," +
               location.getBlockX() + "," +
               location.getBlockY() + "," +
               location.getBlockZ();
    }

    public Location stringToLocation(String locStr) {
        String[] parts = locStr.split(",");
        if (parts.length != 4) return null;
        
        World world = plugin.getServer().getWorld(parts[0]);
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

    public void registerController(Location location, UUID networkId) {
        String locStr = locationToString(location);
        controllers.put(locStr, networkId);
        
        networkControllers.computeIfAbsent(networkId, k -> new HashSet<>()).add(locStr);
    }

    public void unregisterController(Location location) {
        String locStr = locationToString(location);
        UUID networkId = controllers.remove(locStr);
        
        if (networkId != null && networkControllers.containsKey(networkId)) {
            networkControllers.get(networkId).remove(locStr);
            if (networkControllers.get(networkId).isEmpty()) {
                networkControllers.remove(networkId);
            }
        }
    }

    public UUID getNetworkId(Location location) {
        String locStr = locationToString(location);
        return controllers.get(locStr);
    }

    public boolean isController(Location location) {
        return controllers.containsKey(locationToString(location));
    }

    public Set<String> getControllerLocations(UUID networkId) {
        return networkControllers.getOrDefault(networkId, Collections.emptySet());
    }

    public void loadAllControllers() {
        plugin.getLogger().info("开始加载所有控制器...");
        List<ControllerData> controllerDataList = plugin.getDatabaseManager().loadAllControllersFromDB();
        
        for (ControllerData data : controllerDataList) {
            Location location = stringToLocation(data.location);
            if (location != null) {
                controllers.put(data.location, data.networkId);
                networkControllers.computeIfAbsent(data.networkId, k -> new HashSet<>()).add(data.location);
            } else {
                plugin.getLogger().warning("无法解析控制器位置: " + data.location);
            }
        }
        
        plugin.getLogger().info("控制器加载完成，共 " + controllers.size() + " 个控制器");
    }

    public void saveAllControllers() {
        plugin.getLogger().info("保存所有控制器...");
        plugin.getDatabaseManager().saveAllControllersToDB(controllers);
        plugin.getLogger().info("控制器保存完成");
    }

    public void saveController(Location location, UUID networkId, UUID ownerUuid) {
        String locStr = locationToString(location);
        plugin.getDatabaseManager().saveControllerToDB(locStr, networkId, ownerUuid);
    }

    public void deleteController(Location location) {
        String locStr = locationToString(location);
        plugin.getDatabaseManager().deleteControllerFromDB(locStr);
        unregisterController(location);
    }

    public void removeControllersForNetwork(UUID networkId) {
        Set<String> locations = networkControllers.remove(networkId);
        if (locations == null || locations.isEmpty()) {
            return;
        }

        for (String locStr : locations) {
            controllers.remove(locStr);
            plugin.getDatabaseManager().deleteControllerFromDB(locStr);

            // 删除控制器方块
            Location loc = stringToLocation(locStr);
            if (loc != null && loc.getBlock().getType().name().contains("HONEYCOMB")) {
                loc.getBlock().setType(Material.AIR);
            }
        }

        plugin.getLogger().info("已删除网络 " + networkId + " 的 " + locations.size() + " 个控制器");
    }

    // ========== 连接工具状态管理 ==========

    public void setSelectedNetwork(UUID playerUUID, UUID networkId) {
        selectedNetwork.put(playerUUID, networkId);
    }

    public UUID getSelectedNetwork(UUID playerUUID) {
        return selectedNetwork.get(playerUUID);
    }

    public void clearSelectedNetwork(UUID playerUUID) {
        selectedNetwork.remove(playerUUID);
    }

    public boolean hasSelectedNetwork(UUID playerUUID) {
        return selectedNetwork.containsKey(playerUUID);
    }

    // ========== 调试子设备管理 ==========

    public void registerDebugDevice(Location location, UUID networkId) {
        String locStr = locationToString(location);
        debugDevices.put(locStr, networkId);
    }

    public void unregisterDebugDevice(Location location) {
        debugDevices.remove(locationToString(location));
    }

    public UUID getDebugDeviceNetwork(Location location) {
        return debugDevices.get(locationToString(location));
    }

    public boolean isDebugDevice(Location location) {
        return debugDevices.containsKey(locationToString(location));
    }

    public Map<String, UUID> getAllDebugDevices() {
        return new HashMap<>(debugDevices);
    }

    public void removeDebugDevicesForNetwork(UUID networkId) {
        debugDevices.entrySet().removeIf(entry -> entry.getValue().equals(networkId));
    }

    /**
     * 断开网络的所有调试子设备连接，将方块变为红色
     */
    public void disconnectDebugDevicesForNetwork(UUID networkId) {
        // 找到所有连接到该网络的调试子设备
        List<String> locations = new ArrayList<>();
        for (Map.Entry<String, UUID> entry : debugDevices.entrySet()) {
            if (entry.getValue() != null && entry.getValue().equals(networkId)) {
                locations.add(entry.getKey());
            }
        }

        // 将方块变为红色，并更新数据库
        for (String locStr : locations) {
            Location location = stringToLocation(locStr);
            if (location != null) {
                updateDebugDeviceBlock(location, false);
                debugDevices.put(locStr, null); // 设为未连接状态
                plugin.getDatabaseManager().saveDebugDeviceToDB(locStr, null);
            }
        }
    }

    public void saveDebugDeviceToDB(Location location, UUID networkId) {
        plugin.getDatabaseManager().saveDebugDeviceToDB(locationToString(location), networkId);
    }

    public void deleteDebugDeviceFromDB(Location location) {
        plugin.getDatabaseManager().deleteDebugDeviceFromDB(locationToString(location));
    }

    /**
     * 扫描指定位置周围6格内的控制器，返回找到的网络ID
     */
    public UUID scanNearbyController(Location location) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    
                    Location checkLoc = location.clone().add(dx, dy, dz);
                    if (isController(checkLoc)) {
                        return getNetworkId(checkLoc);
                    }
                }
            }
        }
        return null;
    }

    /**
     * 更新调试子设备方块材质（红/绿）
     */
    public void updateDebugDeviceBlock(Location location, boolean connected) {
        if (location.getBlock().getType() == Material.RED_CONCRETE && connected) {
            location.getBlock().setType(Material.GREEN_CONCRETE);
        } else if (location.getBlock().getType() == Material.GREEN_CONCRETE && !connected) {
            location.getBlock().setType(Material.RED_CONCRETE);
        }
    }
    
    /**
     * 更新方块材质（通用方法）
     * @param location 方块位置
     * @param connectedMaterial 已连接时显示的材质
     * @param connected 是否已连接
     */
    public void updateDeviceBlock(Location location, Material connectedMaterial, boolean connected) {
        if (connected) {
            location.getBlock().setType(connectedMaterial);
        } else {
            // 未连接时使用较深的颜色表示
            switch (connectedMaterial) {
                case ORANGE_CONCRETE -> location.getBlock().setType(Material.ORANGE_TERRACOTTA);
                case CYAN_CONCRETE -> location.getBlock().setType(Material.CYAN_TERRACOTTA);
                default -> location.getBlock().setType(Material.GRAY_CONCRETE);
            }
        }
    }

    public void loadAllDebugDevices() {
        plugin.getLogger().info("开始加载所有调试子设备...");
        List<DebugDeviceData> deviceDataList = plugin.getDatabaseManager().loadAllDebugDevicesFromDB();

        for (DebugDeviceData data : deviceDataList) {
            Location location = stringToLocation(data.location);
            if (location != null) {
                debugDevices.put(data.location, data.networkId);
                if (data.networkId != null) {
                    updateDebugDeviceBlock(location, true);
                }
            } else {
                plugin.getLogger().warning("无法解析调试子设备位置: " + data.location);
            }
        }

        plugin.getLogger().info("调试子设备加载完成，共 " + debugDevices.size() + " 个设备");
    }

    public void saveAllDebugDevices() {
        plugin.getLogger().info("保存所有调试子设备...");
        plugin.getDatabaseManager().saveAllDebugDevicesToDB(debugDevices);
        plugin.getLogger().info("调试子设备保存完成");
    }

    public record ControllerData(String location, UUID networkId, UUID ownerUuid) {
    }

    public record DebugDeviceData(String location, UUID networkId) {
    }
    
    // ========== 磁盘操纵器管理 ==========
    
    /**
     * 注册磁盘操纵器
     * 如果已存在数据，只更新 networkId，保留 slots 数据
     */
    public void registerDiskManipulator(Location location, UUID networkId) {
        String locStr = locationToString(location);
        DiskManipulatorData data = diskManipulators.get(locStr);
        
        if (data != null) {
            // 已存在，只更新 networkId，保留 slots 数据
            data.networkId = networkId;
        } else {
            // 不存在，创建新数据
            data = new DiskManipulatorData(locStr, networkId);
            diskManipulators.put(locStr, data);
        }
        
        plugin.getDatabaseManager().saveDiskManipulatorToDB(data);
    }
    
    /**
     * 注销磁盘操纵器
     */
    public void unregisterDiskManipulator(Location location) {
        String locStr = locationToString(location);
        diskManipulators.remove(locStr);
        plugin.getDatabaseManager().deleteDiskManipulatorFromDB(locStr);
    }
    
    /**
     * 获取磁盘操纵器数据
     */
    public DiskManipulatorData getDiskManipulator(Location location) {
        return diskManipulators.get(locationToString(location));
    }
    
    /**
     * 检查是否是磁盘操纵器
     */
    public boolean isDiskManipulator(Location location) {
        return diskManipulators.containsKey(locationToString(location));
    }
    
    /**
     * 断开磁盘操纵器的网络连接
     */
    public void disconnectDiskManipulator(Location location) {
        String locStr = locationToString(location);
        DiskManipulatorData data = diskManipulators.get(locStr);
        if (data != null) {
            data.networkId = null;
            plugin.getDatabaseManager().saveDiskManipulatorToDB(data);
            // 更新方块颜色为未连接状态
            updateDeviceBlock(location, Material.ORANGE_CONCRETE, false);
        }
    }
    
    /**
     * 插入磁盘到磁盘操纵器
     */
    public int insertDiskToManipulator(Location location, UUID diskUuid) {
        DiskManipulatorData data = getDiskManipulator(location);
        if (data == null) return -1;
        
        int slot = data.insertDisk(diskUuid);
        if (slot >= 0) {
            plugin.getDatabaseManager().saveDiskManipulatorToDB(data);
        }
        return slot;
    }
    
    /**
     * 从磁盘操纵器取出磁盘
     */
    public UUID removeDiskFromManipulator(Location location, int slotIndex) {
        DiskManipulatorData data = getDiskManipulator(location);
        if (data == null) return null;
        
        UUID diskUuid = data.removeDisk(slotIndex);
        if (diskUuid != null) {
            plugin.getDatabaseManager().saveDiskManipulatorToDB(data);
        }
        return diskUuid;
    }
    
    /**
     * 获取所有磁盘操纵器数据
     */
    public Collection<DiskManipulatorData> getAllDiskManipulators() {
        return diskManipulators.values();
    }
    
    /**
     * 获取连接到指定网络的所有磁盘操纵器
     */
    public List<DiskManipulatorData> getDiskManipulatorsByNetwork(UUID networkId) {
        List<DiskManipulatorData> result = new ArrayList<>();
        for (DiskManipulatorData data : diskManipulators.values()) {
            if (networkId.equals(data.networkId)) {
                result.add(data);
            }
        }
        return result;
    }
    
    /**
     * 删除网络时断开所有磁盘操纵器
     */
    public void removeDiskManipulatorsForNetwork(UUID networkId) {
        for (Map.Entry<String, DiskManipulatorData> entry : diskManipulators.entrySet()) {
            if (networkId.equals(entry.getValue().networkId)) {
                // 更新方块颜色为未连接状态
                Location location = stringToLocation(entry.getKey());
                if (location != null) {
                    updateDeviceBlock(location, Material.ORANGE_CONCRETE, false);
                }
                // 设置 networkId 为 null
                entry.getValue().networkId = null;
                // 保存到数据库
                plugin.getDatabaseManager().saveDiskManipulatorToDB(entry.getValue());
            }
        }
    }
    
    /**
     * 加载所有磁盘操纵器
     */
    public void loadAllDiskManipulators() {
        plugin.getLogger().info("开始加载所有磁盘操纵器...");
        List<DiskManipulatorData> dataList = plugin.getDatabaseManager().loadAllDiskManipulatorsFromDB();
        for (DiskManipulatorData data : dataList) {
            diskManipulators.put(data.location, data);
            // 恢复方块颜色
            Location location = stringToLocation(data.location);
            if (location != null) {
                updateDeviceBlock(location, Material.ORANGE_CONCRETE, data.networkId != null);
            }
        }
        plugin.getLogger().info("磁盘操纵器加载完成，共 " + diskManipulators.size() + " 个");
    }
    
    /**
     * 保存所有磁盘操纵器
     */
    public void saveAllDiskManipulators() {
        plugin.getLogger().info("保存所有磁盘操纵器...");
        for (DiskManipulatorData data : diskManipulators.values()) {
            plugin.getDatabaseManager().saveDiskManipulatorToDB(data);
        }
        plugin.getLogger().info("磁盘操纵器保存完成");
    }
    
    // ========== 终端管理 ==========
    
    /**
     * 注册终端
     * 如果已存在数据，只更新 networkId，保留原有数据
     */
    public void registerTerminal(Location location, UUID networkId) {
        String locStr = locationToString(location);
        TerminalData data = terminals.get(locStr);
        
        if (data != null) {
            // 已存在，只更新 networkId
            data.networkId = networkId;
        } else {
            // 不存在，创建新数据
            data = new TerminalData(locStr, networkId);
            terminals.put(locStr, data);
        }
        
        plugin.getDatabaseManager().saveTerminalToDB(data);
    }
    
    /**
     * 注销终端
     */
    public void unregisterTerminal(Location location) {
        String locStr = locationToString(location);
        terminals.remove(locStr);
        plugin.getDatabaseManager().deleteTerminalFromDB(locStr);
    }
    
    /**
     * 获取终端数据
     */
    public TerminalData getTerminal(Location location) {
        return terminals.get(locationToString(location));
    }
    
    /**
     * 检查是否是终端
     */
    public boolean isTerminal(Location location) {
        return terminals.containsKey(locationToString(location));
    }
    
    /**
     * 断开终端的网络连接
     */
    public void disconnectTerminal(Location location) {
        String locStr = locationToString(location);
        TerminalData data = terminals.get(locStr);
        if (data != null) {
            data.networkId = null;
            plugin.getDatabaseManager().saveTerminalToDB(data);
            // 更新方块颜色为未连接状态
            updateDeviceBlock(location, Material.CYAN_CONCRETE, false);
        }
    }
    
    /**
     * 获取所有终端数据
     */
    public Collection<TerminalData> getAllTerminals() {
        return terminals.values();
    }
    
    /**
     * 获取连接到指定网络的所有终端
     */
    public List<TerminalData> getTerminalsByNetwork(UUID networkId) {
        List<TerminalData> result = new ArrayList<>();
        for (TerminalData data : terminals.values()) {
            if (networkId.equals(data.networkId)) {
                result.add(data);
            }
        }
        return result;
    }
    
    /**
     * 删除网络时断开所有终端
     */
    public void removeTerminalsForNetwork(UUID networkId) {
        for (Map.Entry<String, TerminalData> entry : terminals.entrySet()) {
            if (networkId.equals(entry.getValue().networkId)) {
                // 更新方块颜色为未连接状态
                Location location = stringToLocation(entry.getKey());
                if (location != null) {
                    updateDeviceBlock(location, Material.CYAN_CONCRETE, false);
                }
                // 设置 networkId 为 null
                entry.getValue().networkId = null;
                // 保存到数据库
                plugin.getDatabaseManager().saveTerminalToDB(entry.getValue());
            }
        }
    }
    
    /**
     * 加载所有终端
     */
    public void loadAllTerminals() {
        plugin.getLogger().info("开始加载所有终端...");
        List<TerminalData> dataList = plugin.getDatabaseManager().loadAllTerminalsFromDB();
        for (TerminalData data : dataList) {
            terminals.put(data.location, data);
            // 恢复方块颜色
            Location location = stringToLocation(data.location);
            if (location != null) {
                updateDeviceBlock(location, Material.CYAN_CONCRETE, data.networkId != null);
            }
        }
        plugin.getLogger().info("终端加载完成，共 " + terminals.size() + " 个");
    }
    
    /**
     * 保存所有终端
     */
    public void saveAllTerminals() {
        plugin.getLogger().info("保存所有终端...");
        for (TerminalData data : terminals.values()) {
            plugin.getDatabaseManager().saveTerminalToDB(data);
        }
        plugin.getLogger().info("终端保存完成");
    }
    
    // ========== 外部存储总线管理 ==========
    
    private final Map<String, ExternalStorageBusData> externalStorageBuses = new ConcurrentHashMap<>();
    
    /**
     * 注册外部存储总线（指定UUID）
     */
    public void registerExternalStorageBus(UUID busUuid, Location location, UUID networkId, Location containerLocation, String containerType) {
        String locStr = locationToString(location);
        String containerLocStr = locationToString(containerLocation);
        
        ExternalStorageBusData data = new ExternalStorageBusData(busUuid, locStr, networkId, containerLocStr, containerType);
        externalStorageBuses.put(locStr, data);
        plugin.getDatabaseManager().saveExternalStorageBusToDB(data);
    }
    
    /**
     * 注销外部存储总线
     */
    public void unregisterExternalStorageBus(Location location) {
        String locStr = locationToString(location);
        externalStorageBuses.remove(locStr);
        plugin.getDatabaseManager().deleteExternalStorageBusFromDB(locStr);
    }
    
    /**
     * 获取外部存储总线数据（通过位置）
     */
    public ExternalStorageBusData getExternalStorageBus(Location location) {
        return externalStorageBuses.get(locationToString(location));
    }
    
    /**
     * 获取外部存储总线数据（通过UUID）
     */
    public ExternalStorageBusData getExternalStorageBusByUuid(UUID busUuid) {
        for (ExternalStorageBusData data : externalStorageBuses.values()) {
            if (data.busUuid.equals(busUuid)) {
                return data;
            }
        }
        return null;
    }
    
    /**
     * 检查是否是外部存储总线
     */
    public boolean isExternalStorageBus(Location location) {
        return externalStorageBuses.containsKey(locationToString(location));
    }
    
    /**
     * 获取所有连接到指定网络的外部存储总线
     */
    public List<ExternalStorageBusData> getExternalStorageBusesByNetwork(UUID networkId) {
        List<ExternalStorageBusData> result = new ArrayList<>();
        for (ExternalStorageBusData data : externalStorageBuses.values()) {
            if (networkId.equals(data.networkId)) {
                result.add(data);
            }
        }
        return result;
    }
    
    /**
     * 断开外部存储总线的网络连接
     */
    public void disconnectExternalStorageBus(Location location) {
        String locStr = locationToString(location);
        ExternalStorageBusData data = externalStorageBuses.get(locStr);
        if (data != null) {
            data.networkId = null;
            plugin.getDatabaseManager().saveExternalStorageBusToDB(data);
        }
    }
    
    /**
     * 删除网络时断开所有外部存储总线
     */
    public void removeExternalStorageBusesForNetwork(UUID networkId) {
        for (Map.Entry<String, ExternalStorageBusData> entry : externalStorageBuses.entrySet()) {
            if (networkId.equals(entry.getValue().networkId)) {
                entry.getValue().networkId = null;
                plugin.getDatabaseManager().saveExternalStorageBusToDB(entry.getValue());
            }
        }
    }
    
    /**
     * 加载所有外部存储总线
     */
    public void loadAllExternalStorageBuses() {
        plugin.getLogger().info("开始加载所有外部存储总线...");
        List<ExternalStorageBusData> dataList = plugin.getDatabaseManager().loadAllExternalStorageBusesFromDB();
        for (ExternalStorageBusData data : dataList) {
            externalStorageBuses.put(data.location, data);
        }
        plugin.getLogger().info("外部存储总线加载完成，共 " + externalStorageBuses.size() + " 个");
    }
    
    /**
     * 保存所有外部存储总线
     */
    public void saveAllExternalStorageBuses() {
        plugin.getLogger().info("保存所有外部存储总线...");
        for (ExternalStorageBusData data : externalStorageBuses.values()) {
            plugin.getDatabaseManager().saveExternalStorageBusToDB(data);
        }
        plugin.getLogger().info("外部存储总线保存完成");
    }
}