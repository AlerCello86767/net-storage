package com.AlerCello86767.net_storage.task;

import com.AlerCello86767.net_storage.Net_storage;
import com.AlerCello86767.net_storage.controller.ExternalStorageBusData;
import com.AlerCello86767.net_storage.controller.InputBusData;
import com.AlerCello86767.net_storage.disk.DiskItem;
import com.AlerCello86767.net_storage.disk.DiskManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 输入总线定时传输任务
 * 每 9 ticks 从容器提取物品并传输到网络存储
 */
public class InputBusTask implements Runnable {
    
    private final Net_storage plugin;
    
    public InputBusTask(Net_storage plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void run() {
        // 在主线程执行
        Bukkit.getScheduler().runTask(plugin, () -> {
            processAllInputBuses();
        });
    }
    
    /**
     * 处理所有已连接的输入总线
     */
    private void processAllInputBuses() {
        // 获取所有输入总线
        for (InputBusData inputBus : plugin.getControllerManager().getAllInputBuses()) {
            // 跳过未连接的输入总线
            if (inputBus.networkId == null) {
                continue;
            }
            
            // 处理传输
            processInputBus(inputBus);
        }
    }
    
    /**
     * 处理单个输入总线的传输
     */
    private void processInputBus(InputBusData inputBus) {
        Location containerLoc = inputBus.getContainerLocation();
        if (containerLoc == null) {
            return;
        }
        
        Block containerBlock = containerLoc.getBlock();
        if (containerBlock == null || !(containerBlock.getState() instanceof Container)) {
            return;
        }
        
        Container container = (Container) containerBlock.getState();
        org.bukkit.inventory.Inventory inventory = container.getInventory();
        
        // 遍历容器槽位，找到第一个非空物品
        ItemStack sourceItem = null;
        int sourceSlot = -1;
        
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                sourceItem = item.clone();
                sourceItem.setAmount(1); // 每次只取 1 个
                sourceSlot = i;
                break;
            }
        }
        
        if (sourceItem == null || sourceSlot < 0) {
            return; // 容器为空
        }
        
        // 尝试存入网络
        int stored = storeItemToNetwork(inputBus.networkId, sourceItem);
        
        // 如果存储成功，从容器移除物品
        if (stored > 0) {
            ItemStack remaining = inventory.getItem(sourceSlot);
            if (remaining != null) {
                if (remaining.getAmount() <= 1) {
                    inventory.setItem(sourceSlot, null);
                } else {
                    remaining.setAmount(remaining.getAmount() - 1);
                }
            }
        }
    }
    
    /**
     * 将物品存入网络存储
     * @param networkId 网络ID
     * @param item 要存入的物品
     * @return 实际存入的数量
     */
    private int storeItemToNetwork(UUID networkId, ItemStack item) {
        DiskManager diskManager = plugin.getDiskManager();
        
        // 获取网络的所有磁盘
        List<UUID> networkDisks = getNetworkDisks(networkId);
        
        if (networkDisks.isEmpty()) {
            // 没有磁盘，尝试存入外部存储总线容器
            return storeItemToExternalContainers(networkId, item);
        }
        
        int stored = 0;
        Map<UUID, List<DiskItem>> modifiedDisks = new HashMap<>();
        
        // 第一步：优先堆叠到已有相同物品的磁盘
        for (UUID diskUuid : networkDisks) {
            if (stored > 0) break;
            
            List<DiskItem> diskItems = diskManager.getDiskData(diskUuid);
            int usedSpace = diskManager.getTotalItems(diskItems);
            int remainingSpace = diskManager.getDiskCapacity(diskUuid) - usedSpace;
            
            if (remainingSpace <= 0) continue;
            
            // 查找该磁盘是否有相同物品
            for (DiskItem diskItem : diskItems) {
                if (diskItem.matchesItemStack(item)) {
                    int canAdd = Math.min(remainingSpace, 1);
                    if (canAdd > 0) {
                        diskItem.addAmount(canAdd);
                        stored += canAdd;
                        modifiedDisks.put(diskUuid, diskItems);
                    }
                    break;
                }
            }
        }
        
        // 第二步：如果没有堆叠成功，存入有空位的磁盘
        if (stored == 0) {
            for (UUID diskUuid : networkDisks) {
                if (stored > 0) break;
                
                List<DiskItem> diskItems = diskManager.getDiskData(diskUuid);
                int usedSpace = diskManager.getTotalItems(diskItems);
                int remainingSpace = diskManager.getDiskCapacity(diskUuid) - usedSpace;
                
                if (remainingSpace <= 0) continue;
                
                int canAdd = Math.min(remainingSpace, 1);
                if (canAdd > 0) {
                    DiskItem newItem = DiskItem.fromItemStack(item, canAdd);
                    newItem.setDiskUuid(diskUuid);
                    diskItems.add(newItem);
                    stored += canAdd;
                    modifiedDisks.put(diskUuid, diskItems);
                }
            }
        }
        
        // 保存所有修改过的磁盘
        for (Map.Entry<UUID, List<DiskItem>> entry : modifiedDisks.entrySet()) {
            diskManager.saveDiskData(entry.getKey(), entry.getValue());
        }
        
        // 第三步：如果还有剩余，存入外部存储总线容器
        if (stored == 0) {
            stored = storeItemToExternalContainers(networkId, item);
        }
        
        return stored;
    }
    
    /**
     * 将物品存入外部存储总线容器
     */
    private int storeItemToExternalContainers(UUID networkId, ItemStack item) {
        List<ExternalStorageBusData> externalBuses = plugin.getControllerManager().getExternalStorageBusesByNetwork(networkId);
        
        for (ExternalStorageBusData busData : externalBuses) {
            Location containerLoc = busData.getContainerLocation();
            if (containerLoc == null) continue;
            
            Block containerBlock = containerLoc.getBlock();
            if (containerBlock == null || !(containerBlock.getState() instanceof Container)) continue;
            
            Container container = (Container) containerBlock.getState();
            org.bukkit.inventory.Inventory inventory = container.getInventory();
            
            // 第一步：优先堆叠到已有相同物品的槽位
            for (int i = 0; i < inventory.getSize(); i++) {
                ItemStack slotItem = inventory.getItem(i);
                if (slotItem != null && slotItem.isSimilar(item) && slotItem.getAmount() < slotItem.getMaxStackSize()) {
                    slotItem.setAmount(slotItem.getAmount() + 1);
                    return 1;
                }
            }
            
            // 第二步：存入空槽位
            for (int i = 0; i < inventory.getSize(); i++) {
                ItemStack slotItem = inventory.getItem(i);
                if (slotItem == null || slotItem.getType() == Material.AIR) {
                    ItemStack newItem = item.clone();
                    newItem.setAmount(1);
                    inventory.setItem(i, newItem);
                    return 1;
                }
            }
        }
        
        return 0; // 存储失败
    }
    
    /**
     * 获取网络的所有磁盘UUID
     */
    private List<UUID> getNetworkDisks(UUID networkId) {
        List<UUID> diskUuids = new java.util.ArrayList<>();
        
        // 获取网络的所有磁盘操纵器
        var manipulators = plugin.getControllerManager().getDiskManipulatorsByNetwork(networkId);
        
        for (var manipulator : manipulators) {
            if (manipulator.slots != null) {
                for (UUID diskUuid : manipulator.slots) {
                    if (diskUuid != null) {
                        diskUuids.add(diskUuid);
                    }
                }
            }
        }
        
        return diskUuids;
    }
}
