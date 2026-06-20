package com.AlerCello86767.net_storage.gui.listeners;

import com.AlerCello86767.net_storage.Net_storage;
import com.AlerCello86767.net_storage.gui.BaseGUI;
import com.AlerCello86767.net_storage.gui.ClickType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InventoryClickListener implements Listener {

    private final Net_storage plugin;
    private final Map<UUID, ClickHandler> clickHandlers = new HashMap<>();

    public InventoryClickListener(Net_storage plugin) {
        this.plugin = plugin;
    }

    @FunctionalInterface
    public interface ClickHandler {
        boolean handle(Player player, ItemStack item, int slot, ClickType clickType, boolean isGUISlot);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();

        if (!(holder instanceof BaseGUI)) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        UUID uuid = player.getUniqueId();
        ClickHandler handler = clickHandlers.get(uuid);
        if (handler == null) {
            return;
        }

        int slot = event.getRawSlot();
        ItemStack item = event.getCurrentItem();
        ClickType clickType = convertClickType(event.getClick());
        boolean isGUISlot = slot < inventory.getSize();

        boolean cancelled = handler.handle(player, item, slot, clickType, isGUISlot);
        if (cancelled) {
            event.setCancelled(true);
        }
    }

    private ClickType convertClickType(org.bukkit.event.inventory.ClickType bukkitClickType) {
        if (bukkitClickType == null) {
            return ClickType.UNKNOWN;
        }

        switch (bukkitClickType) {
            case LEFT:
                return ClickType.LEFT_CLICK;
            case RIGHT:
                return ClickType.RIGHT_CLICK;
            case SHIFT_LEFT:
                return ClickType.SHIFT_LEFT_CLICK;
            case SHIFT_RIGHT:
                return ClickType.SHIFT_RIGHT_CLICK;
            case MIDDLE:
                return ClickType.MIDDLE_CLICK;
            case DOUBLE_CLICK:
                return ClickType.DOUBLE_CLICK;
            case DROP:
                return ClickType.DROP;
            case CONTROL_DROP:
                return ClickType.CONTROL_DROP;
            case NUMBER_KEY:
                return ClickType.NUMBER_KEY;
            default:
                return ClickType.UNKNOWN;
        }
    }

    public void registerHandler(Player player, ClickHandler handler) {
        clickHandlers.put(player.getUniqueId(), handler);
    }

    public void unregisterHandler(Player player) {
        clickHandlers.remove(player.getUniqueId());
    }

    public void clearHandlers() {
        clickHandlers.clear();
    }
}