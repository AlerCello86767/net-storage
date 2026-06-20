package com.AlerCello86767.net_storage.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class BaseGUI implements InventoryHolder {

    protected final Player player;
    protected final UUID playerUUID;
    protected Inventory inventory;
    protected final int size;
    protected final String title;
    protected final Map<Integer, ClickAction> clickActions = new HashMap<>();
    protected boolean isOpen = false;

    @FunctionalInterface
    public interface ClickAction {
        void execute(Player player, ItemStack item, int slot, ClickType clickType);
    }

    public BaseGUI(Player player, int size, String title) {
        this.player = player;
        this.playerUUID = player.getUniqueId();
        this.size = size;
        this.title = title;
        this.inventory = Bukkit.createInventory(this, size, title);
    }

    public abstract void initialize();

    public void open() {
        if (inventory == null) {
            inventory = Bukkit.createInventory(this, size, title);
        }
        initialize();
        player.openInventory(inventory);
        isOpen = true;
        onOpen();
    }

    public void close() {
        if (isOpen) {
            player.closeInventory();
            isOpen = false;
            onClose();
        }
    }

    public void update() {
        if (isOpen) {
            initialize();
            player.updateInventory();
        }
    }

    public void setClickAction(int slot, ClickAction action) {
        clickActions.put(slot, action);
    }

    public void setClickActions(int[] slots, ClickAction action) {
        for (int slot : slots) {
            clickActions.put(slot, action);
        }
    }

    public void setRegionClickAction(int startSlot, int endSlot, ClickAction action) {
        for (int i = startSlot; i < endSlot; i++) {
            clickActions.put(i, action);
        }
    }

    public void removeClickAction(int slot) {
        clickActions.remove(slot);
    }

    public void clearClickActions() {
        clickActions.clear();
    }

    public void setItem(int slot, ItemStack item) {
        if (inventory != null && slot >= 0 && slot < inventory.getSize()) {
            inventory.setItem(slot, item);
        }
    }

    public void setBorder(ItemStack borderItem) {
        if (inventory == null) return;
        int rows = size / 9;
        int cols = 9;
        for (int i = 0; i < cols; i++) {
            inventory.setItem(i, borderItem);
            inventory.setItem((rows - 1) * 9 + i, borderItem);
        }
        for (int i = 1; i < rows - 1; i++) {
            inventory.setItem(i * 9, borderItem);
            inventory.setItem(i * 9 + cols - 1, borderItem);
        }
    }

    public void fillEmptySlots(ItemStack fillItem) {
        if (inventory == null) return;
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, fillItem);
            }
        }
    }

    protected void onOpen() {}
    protected void onClose() {}

    private ClickType convertClickType(org.bukkit.event.inventory.ClickType bukkitClickType) {
        if (bukkitClickType == null) return ClickType.UNKNOWN;
        switch (bukkitClickType) {
            case LEFT: return ClickType.LEFT_CLICK;
            case RIGHT: return ClickType.RIGHT_CLICK;
            case SHIFT_LEFT: return ClickType.SHIFT_LEFT_CLICK;
            case SHIFT_RIGHT: return ClickType.SHIFT_RIGHT_CLICK;
            case MIDDLE: return ClickType.MIDDLE_CLICK;
            case DOUBLE_CLICK: return ClickType.DOUBLE_CLICK;
            case DROP: return ClickType.DROP;
            case CONTROL_DROP: return ClickType.CONTROL_DROP;
            case NUMBER_KEY: return ClickType.NUMBER_KEY;
            default: return ClickType.UNKNOWN;
        }
    }

    // ========== 核心：点击处理 ==========

    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack item = event.getCurrentItem();
        ClickType clickType = convertClickType(event.getClick());

        // GUI 区域点击
        if (slot < inventory.getSize()) {
            ClickAction action = clickActions.get(slot);
            if (action != null) {
                action.execute(player, item, slot, clickType);
                event.setCancelled(true);
                return;
            }
            event.setCancelled(true);
            return;
        }

        // 背包区域 - 完全交给子类
        if (slot >= inventory.getSize()) {
            handlePlayerInventoryClick(event, slot, item, clickType);
        }
    }

    /**
     * 子类重写此方法处理背包点击
     * 必须调用 event.setCancelled(true/false)
     */
    protected void handlePlayerInventoryClick(InventoryClickEvent event, int slot, ItemStack item, ClickType clickType) {
        event.setCancelled(true);
    }

    // ========== 拖拽处理 ==========

    public void handleDrag(InventoryDragEvent event) {
        event.setCancelled(true);
    }

    public void handleClose(InventoryCloseEvent event) {
        isOpen = false;
        onClose();
    }

    // ========== Getters ==========

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public int getSize() {
        return size;
    }

    public String getTitle() {
        return title;
    }

    public boolean isGUISlot(int slot) {
        return slot < inventory.getSize();
    }

    public boolean isPlayerInventorySlot(int slot) {
        return slot >= inventory.getSize();
    }
}