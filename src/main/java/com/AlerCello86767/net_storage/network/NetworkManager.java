package com.AlerCello86767.net_storage.network;

import com.AlerCello86767.net_storage.Net_storage;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.stream.Collectors;

public class NetworkManager {

    private final Net_storage plugin;
    private final Map<UUID, StorageNetwork> networks = new HashMap<>();

    public NetworkManager(Net_storage plugin) {
        this.plugin = plugin;
    }

    public StorageNetwork createNetwork(String name, Player creator) {
        int maxNetworks = plugin.getConfigManager().getConfig().getInt("network.max-networks-per-player", 5);
        if (maxNetworks > 0) {
            long playerNetworkCount = networks.values().stream()
                    .filter(net -> net.getCreatorUUID().equals(creator.getUniqueId()))
                    .count();
            if (playerNetworkCount >= maxNetworks) {
                return null;
            }
        }

        StorageNetwork network = new StorageNetwork(name, creator);
        networks.put(network.getNetworkId(), network);
        plugin.getLogger().info("玩家 " + creator.getName() + " 创建了网络: " + name);

        saveNetworkAsync(network);
        return network;
    }

    public StorageNetwork getNetwork(UUID uuid) {
        return networks.get(uuid);
    }

    public StorageNetwork getNetworkByName(String name) {
        for (StorageNetwork net : networks.values()) {
            if (net.getName().equalsIgnoreCase(name)) {
                return net;
            }
        }
        return null;
    }

    public boolean deleteNetwork(UUID uuid) {
        StorageNetwork network = networks.get(uuid);
        if (network == null) {
            return false;
        }

        // 先删除该网络的所有控制器
        plugin.getControllerManager().removeControllersForNetwork(uuid);

        // 断开所有调试子设备连接，将方块变为红色
        plugin.getControllerManager().disconnectDebugDevicesForNetwork(uuid);
        
        // 断开所有磁盘操纵器连接，将方块变为橙色陶瓦
        plugin.getControllerManager().removeDiskManipulatorsForNetwork(uuid);
        
        // 断开所有终端连接，将方块变为青色陶瓦
        plugin.getControllerManager().removeTerminalsForNetwork(uuid);
        
        // 断开所有外部存储总线连接
        plugin.getControllerManager().removeExternalStorageBusesForNetwork(uuid);

        // 从内存中移除网络
        networks.remove(uuid);
        plugin.getLogger().info("删除网络: " + network.getName() + " (ID: " + uuid + ")");

        // 异步删除数据库记录
        deleteNetworkAsync(uuid);
        return true;
    }

    public List<StorageNetwork> getPlayerNetworks(Player player) {
        return networks.values().stream()
                .filter(net -> net.getCreatorUUID().equals(player.getUniqueId()))
                .collect(Collectors.toList());
    }

    public Map<UUID, StorageNetwork> getNetworks() {
        return networks;
    }

    public List<StorageNetwork> getPublicNetworks() {
        return networks.values().stream()
                .filter(StorageNetwork::isPublic)
                .collect(Collectors.toList());
    }

    public void loadAllNetworks() {
        plugin.getLogger().info("开始加载所有网络...");
        try {
            List<StorageNetwork> loadedNetworks = plugin.getDatabaseManager().loadAllNetworksFromDB();
            for (StorageNetwork network : loadedNetworks) {
                networks.put(network.getNetworkId(), network);
            }
            plugin.getLogger().info("网络加载完成，共 " + networks.size() + " 个网络");
        } catch (Exception e) {
            plugin.getLogger().severe("加载网络时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveAllNetworks() {
        plugin.getLogger().info("保存所有网络...");
        for (StorageNetwork network : networks.values()) {
            if (network.isDirty()) {
                plugin.getDatabaseManager().saveNetworkToDB(network);
            }
        }
        plugin.getLogger().info("保存完成");
    }

    /**
     * 强制保存所有网络，用于服务器关闭时
     */
    public void forceSaveAllNetworks() {
        plugin.getLogger().info("强制保存所有网络...");
        for (StorageNetwork network : networks.values()) {
            plugin.getDatabaseManager().saveNetworkToDB(network);
        }
        plugin.getLogger().info("强制保存完成，共保存 " + networks.size() + " 个网络");
    }

    public void saveNetwork(StorageNetwork network) {
        if (network != null) {
            plugin.getDatabaseManager().saveNetworkToDB(network);
        }
    }

    public void saveNetworkAsync(StorageNetwork network) {
        if (network != null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    plugin.getDatabaseManager().saveNetworkToDB(network);
                }
            }.runTaskAsynchronously(plugin);
        }
    }

    private void deleteNetworkAsync(UUID networkId) {
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getDatabaseManager().deleteNetworkFromDB(networkId);
            }
        }.runTaskAsynchronously(plugin);
    }

    public void saveAllNetworksAsync() {
        new BukkitRunnable() {
            @Override
            public void run() {
                saveAllNetworks();
            }
        }.runTaskAsynchronously(plugin);
    }
}