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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class GUIListener implements Listener {

    private final Net_storage plugin;

    public GUIListener(Net_storage plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();

        if (holder instanceof BaseGUI baseGUI) {
            baseGUI.handleClick(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();

        if (holder instanceof BaseGUI baseGUI) {
            baseGUI.handleDrag(event);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();

        if (holder instanceof BaseGUI baseGUI) {
            plugin.getGuiManager().unregisterGUI(baseGUI.getPlayer());
            baseGUI.handleClose(event);
        }
    }
}