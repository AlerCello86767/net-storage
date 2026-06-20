package com.AlerCello86767.net_storage.gui.listener;

import com.AlerCello86767.net_storage.Net_storage;
import com.AlerCello86767.net_storage.gui.BaseGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * GUI事件监听器
 */
public class GUIListener implements Listener {

    private final Net_storage plugin;

    public GUIListener(Net_storage plugin) {
        this.plugin = plugin;
    }

    /**
     * 监听物品点击事件
     * 优先级设置为HIGHEST，最后处理
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        // 检查是否为我们插件的GUI
        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();

        // 如果持有者是BaseGUI，处理点击
        if (holder instanceof BaseGUI baseGUI) {
            event.setCancelled(true); // 默认取消所有操作

            // 获取点击的玩家
            if (!(event.getWhoClicked() instanceof Player player)) {
                return;
            }

            // 处理点击
            baseGUI.handleClick(event);
        }
    }

    /**
     * 监听物品拖拽事件
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();

        if (holder instanceof BaseGUI baseGUI) {
            // 取消所有拖拽
            event.setCancelled(true);
            baseGUI.handleDrag(event);
        }
    }

    /**
     * 监听GUI关闭事件
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();

        if (holder instanceof BaseGUI baseGUI) {
            // 从管理器中移除
            plugin.getGuiManager().unregisterGUI(baseGUI.getPlayer());
            baseGUI.handleClose(event);
        }
    }
}