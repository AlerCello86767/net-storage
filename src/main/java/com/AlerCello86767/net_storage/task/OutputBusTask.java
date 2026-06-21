package com.AlerCello86767.net_storage.task;

import com.AlerCello86767.net_storage.Net_storage;
import com.AlerCello86767.net_storage.controller.OutputBusData;
import com.AlerCello86767.net_storage.disk.DiskItem;
import com.AlerCello86767.net_storage.network.StorageNetwork;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 输出总线定时任务
 * 从网络存储中取出物品并输出到绑定的容器
 */
public class OutputBusTask implements Runnable {

    private final Net_storage plugin;

    public OutputBusTask(Net_storage plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        // 在主线程执行
        Bukkit.getScheduler().runTask(plugin, () -> {
            processAllOutputBuses();
        });
    }

    private void processAllOutputBuses() {
        java.util.Collection<OutputBusData> allBuses = plugin.getControllerManager().getAllOutputBuses();
        
        for (OutputBusData outputBus : allBuses) {
            if (outputBus.networkId == null) continue;
            processOutputBus(outputBus);
        }
    }

    private void processOutputBus(OutputBusData outputBus) {
        Location containerLoc = outputBus.getContainerLocation();
        if (containerLoc == null) {
            plugin.getLogger().warning("输出总线容器位置为空！");
            return;
        }

        Block containerBlock = containerLoc.getBlock();
        if (!(containerBlock.getState() instanceof Container)) {
            plugin.getLogger().warning("容器方块不是有效的容器类型！");
            return;
        }

        Container container = (Container) containerBlock.getState();
        Inventory inventory = container.getInventory();

        int emptySlot = inventory.firstEmpty();
        if (emptySlot == -1) {
            return;
        }

        StorageNetwork network = plugin.getNetworkManager().getNetwork(outputBus.networkId);
        if (network == null) {
            plugin.getLogger().warning("网络不存在: " + outputBus.networkId);
            return;
        }

        List<UUID> networkDisks = getNetworkDisks(outputBus.networkId);
        if (networkDisks.isEmpty()) return;

        for (UUID diskUuid : networkDisks) {
            List<DiskItem> diskItems = plugin.getDiskManager().getDiskData(diskUuid);
            
            for (DiskItem diskItem : diskItems) {
                ItemStack itemStack = diskItem.toItemStack();
                
                if (!outputBus.shouldOutput(itemStack)) continue;

                ItemStack item = diskItem.toItemStack();
                if (item == null || item.getType() == Material.AIR) continue;

                int stackableSlot = findStackableSlot(inventory, item);
                if (stackableSlot != -1) {
                    ItemStack existing = inventory.getItem(stackableSlot);
                    int maxStack = Math.min(item.getType().getMaxStackSize(), item.getMaxStackSize());
                    int canAdd = maxStack - existing.getAmount();
                    if (canAdd > 0) {
                        // 每次只输出 1 个物品
                        int toAdd = 1;
                        if (diskItem.getAmount() < toAdd) {
                            toAdd = diskItem.getAmount();
                        }
                        
                        existing.setAmount(existing.getAmount() + toAdd);
                        inventory.setItem(stackableSlot, existing);

                        diskItem.reduceAmount(toAdd);
                        if (diskItem.getAmount() <= 0) {
                            diskItems.remove(diskItem);
                        }

                        plugin.getDiskManager().saveDiskData(diskUuid, diskItems);
                        container.update();
                        
                        return;
                    }
                }

                int empty = inventory.firstEmpty();
                if (empty == -1) return;

                // 每次只输出 1 个物品
                int toAdd = 1;
                if (diskItem.getAmount() < toAdd) {
                    toAdd = diskItem.getAmount();
                }
                
                ItemStack toInsert = item.clone();
                toInsert.setAmount(toAdd);

                inventory.setItem(empty, toInsert);

                diskItem.reduceAmount(toAdd);
                if (diskItem.getAmount() <= 0) {
                    diskItems.remove(diskItem);
                }

                plugin.getDiskManager().saveDiskData(diskUuid, diskItems);
                container.update();
                
                return;
            }
        }
    }

    /**
     * 获取网络的所有磁盘UUID
     */
    private List<UUID> getNetworkDisks(UUID networkId) {
        List<UUID> diskUuids = new ArrayList<>();

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

    /**
     * 查找可堆叠的槽位
     */
    private int findStackableSlot(Inventory inventory, ItemStack item) {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack existing = inventory.getItem(i);
            if (existing != null && existing.getType() != Material.AIR) {
                if (existing.isSimilar(item)) {
                    int maxStack = Math.min(item.getType().getMaxStackSize(), item.getMaxStackSize());
                    if (existing.getAmount() < maxStack) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }
}