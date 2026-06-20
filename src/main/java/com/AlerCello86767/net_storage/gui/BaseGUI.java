package com.AlerCello86767.net_storage.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
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

/**
 * 所有GUI的基类
 */
public abstract class BaseGUI implements InventoryHolder {

    protected final Player player;
    protected final UUID playerUUID;
    protected Inventory inventory;
    protected final int size;
    protected final String title;
    protected final Map<Integer, ClickAction> clickActions = new HashMap<>();
    protected boolean isOpen = false;

    /**
     * 点击动作接口
     */
    @FunctionalInterface
    public interface ClickAction {
        void execute(Player player, ItemStack item, int slot, com.AlerCello86767.net_storage.gui.ClickType clickType);
    }

    public BaseGUI(Player player, int size, String title) {
        this.player = player;
        this.playerUUID = player.getUniqueId();
        this.size = size;
        this.title = title;
        this.inventory = Bukkit.createInventory(this, size, title);
    }

    /**
     * 初始化GUI内容（子类实现）
     */
    public abstract void initialize();

    /**
     * 打开GUI
     */
    public void open() {
        if (inventory == null) {
            inventory = Bukkit.createInventory(this, size, title);
        }
        initialize();
        player.openInventory(inventory);
        isOpen = true;
        onOpen();
    }

    /**
     * 关闭GUI
     */
    public void close() {
        if (isOpen) {
            player.closeInventory();
            isOpen = false;
            onClose();
        }
    }

    /**
     * 更新GUI
     */
    public void update() {
        if (isOpen) {
            initialize();
            player.updateInventory();
        }
    }

    /**
     * 设置点击动作
     */
    public void setClickAction(int slot, ClickAction action) {
        clickActions.put(slot, action);
    }

    /**
     * 设置多个槽位的点击动作
     */
    public void setClickActions(int[] slots, ClickAction action) {
        for (int slot : slots) {
            clickActions.put(slot, action);
        }
    }

    /**
     * 设置区域点击动作（从startSlot到endSlot，不包含边界）
     */
    public void setRegionClickAction(int startSlot, int endSlot, ClickAction action) {
        for (int i = startSlot; i < endSlot; i++) {
            clickActions.put(i, action);
        }
    }

    /**
     * 移除点击动作
     */
    public void removeClickAction(int slot) {
        clickActions.remove(slot);
    }

    /**
     * 清除所有点击动作
     */
    public void clearClickActions() {
        clickActions.clear();
    }

    /**
     * 设置物品
     */
    public void setItem(int slot, ItemStack item) {
        if (inventory != null && slot >= 0 && slot < inventory.getSize()) {
            inventory.setItem(slot, item);
        }
    }

    /**
     * 设置边框（填充指定物品）
     */
    public void setBorder(ItemStack borderItem) {
        if (inventory == null) return;

        int rows = size / 9;
        int cols = 9;

        // 第一行和最后一行
        for (int i = 0; i < cols; i++) {
            inventory.setItem(i, borderItem);
            inventory.setItem((rows - 1) * 9 + i, borderItem);
        }

        // 第一列和最后一列
        for (int i = 1; i < rows - 1; i++) {
            inventory.setItem(i * 9, borderItem);
            inventory.setItem(i * 9 + cols - 1, borderItem);
        }
    }

    /**
     * 填充所有空槽位
     */
    public void fillEmptySlots(ItemStack fillItem) {
        if (inventory == null) return;
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, fillItem);
            }
        }
    }

    // ========== 事件回调方法 ==========

    /**
     * GUI打开时调用
     */
    protected void onOpen() {
        // 子类可重写
    }

    /**
     * GUI关闭时调用
     */
    protected void onClose() {
        // 子类可重写
    }

    /**
     * 将Bukkit的ClickType转换为自定义ClickType
     */
    private com.AlerCello86767.net_storage.gui.ClickType convertClickType(ClickType bukkitClickType) {
        if (bukkitClickType == null) {
            return com.AlerCello86767.net_storage.gui.ClickType.UNKNOWN;
        }

        switch (bukkitClickType) {
            case LEFT:
                return com.AlerCello86767.net_storage.gui.ClickType.LEFT_CLICK;
            case RIGHT:
                return com.AlerCello86767.net_storage.gui.ClickType.RIGHT_CLICK;
            case SHIFT_LEFT:
                return com.AlerCello86767.net_storage.gui.ClickType.SHIFT_LEFT_CLICK;
            case SHIFT_RIGHT:
                return com.AlerCello86767.net_storage.gui.ClickType.SHIFT_RIGHT_CLICK;
            case MIDDLE:
                return com.AlerCello86767.net_storage.gui.ClickType.MIDDLE_CLICK;
            case DOUBLE_CLICK:
                return com.AlerCello86767.net_storage.gui.ClickType.DOUBLE_CLICK;
            case DROP:
                return com.AlerCello86767.net_storage.gui.ClickType.DROP;
            case CONTROL_DROP:
                return com.AlerCello86767.net_storage.gui.ClickType.CONTROL_DROP;
            case NUMBER_KEY:
                return com.AlerCello86767.net_storage.gui.ClickType.NUMBER_KEY;
            default:
                return com.AlerCello86767.net_storage.gui.ClickType.UNKNOWN;
        }
    }

    /**
     * 点击事件处理
     */
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack item = event.getCurrentItem();
        com.AlerCello86767.net_storage.gui.ClickType clickType = convertClickType(event.getClick());

        // 如果slot在标题栏内（容器槽位）
        if (slot < inventory.getSize()) {
            ClickAction action = clickActions.get(slot);
            if (action != null) {
                action.execute(player, item, slot, clickType);
            }
        }

        // 玩家背包点击（不在标题栏内）
        if (slot >= inventory.getSize()) {
            handlePlayerInventoryClick(event, slot, item, clickType);
        }
    }

    /**
     * 处理玩家背包点击（子类可重写）
     */
    protected void handlePlayerInventoryClick(InventoryClickEvent event, int slot, ItemStack item, com.AlerCello86767.net_storage.gui.ClickType clickType) {
        // 默认取消玩家背包的点击
        event.setCancelled(true);
    }

    /**
     * 拖拽事件处理
     */
    public void handleDrag(InventoryDragEvent event) {
        // 默认取消所有拖拽
        event.setCancelled(true);
    }

    /**
     * 关闭事件处理
     */
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

    /**
     * 判断指定槽位是否在GUI区域内（标题栏）
     */
    public boolean isGUISlot(int slot) {
        return slot < inventory.getSize();
    }

    /**
     * 判断指定槽位是否在玩家背包区域
     */
    public boolean isPlayerInventorySlot(int slot) {
        return slot >= inventory.getSize();
    }
}