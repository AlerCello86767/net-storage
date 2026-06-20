package com.AlerCello86767.net_storage.controller;

import com.AlerCello86767.net_storage.Net_storage;
import com.AlerCello86767.net_storage.commands.ControllerCommand;
import com.AlerCello86767.net_storage.network.StorageNetwork;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * 监听连接工具相关事件
 * - 玩家切换物品时保持 Action Bar 显示
 * - 玩家退出时清除选择状态
 */
public class ConnectToolListener implements Listener {

    private final Net_storage plugin;

    public ConnectToolListener(Net_storage plugin) {
        this.plugin = plugin;
    }

    /**
     * 玩家切换物品时，如果手持连接工具且已选择网络，更新 Action Bar
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        
        // 检查是否已选择网络
        if (!plugin.getControllerManager().hasSelectedNetwork(player.getUniqueId())) {
            return;
        }

        // 检查新手持物品是否是连接工具
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        String itemType = ControllerCommand.getItemType(plugin, newItem);

        if ("connect_tool".equals(itemType)) {
            // 手持连接工具，显示已选择的网络
            UUID networkId = plugin.getControllerManager().getSelectedNetwork(player.getUniqueId());
            StorageNetwork network = plugin.getNetworkManager().getNetwork(networkId);
            if (network != null) {
                player.sendActionBar(ChatColor.GREEN + "已选择网络: " + ChatColor.WHITE + network.getName());
            }
        }
    }

    /**
     * 玩家退出时清除选择状态
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getControllerManager().clearSelectedNetwork(player.getUniqueId());
    }
}