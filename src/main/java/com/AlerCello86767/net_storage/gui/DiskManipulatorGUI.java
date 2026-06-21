package com.AlerCello86767.net_storage.gui;

import com.AlerCello86767.net_storage.Net_storage;
import com.AlerCello86767.net_storage.controller.DiskManipulatorData;
import com.AlerCello86767.net_storage.disk.DiskItem;
import com.AlerCello86767.net_storage.disk.DiskManager;
import com.AlerCello86767.net_storage.network.StorageNetwork;
import com.AlerCello86767.net_storage.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 磁盘操纵器 GUI
 * 用于管理磁盘插入和拔出
 */
public class DiskManipulatorGUI extends BaseGUI {

    private final Net_storage plugin;
    private final DiskManipulatorData manipulatorData;
    private final DiskManager diskManager;
    private final Location blockLocation;

    private BukkitTask refreshTask;

    private static final int INFO_SLOT = 0;
    private static final int DISK_SLOT_START = 1;
    private static final int DISK_SLOT_END = 8;
    private static final int SLOT_TO_INDEX_OFFSET = 1;
    private static final long REFRESH_INTERVAL = 60L;

    public DiskManipulatorGUI(Player player, Net_storage plugin,
                              DiskManipulatorData manipulatorData, Location blockLocation) {
        super(player, 9, ChatColor.translateAlternateColorCodes('&', "&b磁盘操纵器"));
        this.plugin = plugin;
        this.manipulatorData = manipulatorData;
        this.diskManager = plugin.getDiskManager();
        this.blockLocation = blockLocation;

        initialize();
        startRefreshTask();
    }

    // ==================== 生命周期 ====================

    private void startRefreshTask() {
        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (player.isOnline() && isOpen()) {
                update();
            } else {
                stopRefreshTask();
            }
        }, REFRESH_INTERVAL, REFRESH_INTERVAL);
    }

    private void stopRefreshTask() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }

    @Override
    protected void onClose() {
        stopRefreshTask();
    }

    // ==================== GUI 初始化 ====================

    @Override
    public void initialize() {
        inventory.clear();
        clickActions.clear();
        setupInfoSlot();
        setupDiskSlots();
    }

    private void setupInfoSlot() {
        String networkName = "未连接";
        String networkIdShort = "无";
        String status = ChatColor.RED + "未连接";

        if (manipulatorData.networkId != null) {
            StorageNetwork network = plugin.getNetworkManager().getNetwork(manipulatorData.networkId);
            if (network != null) {
                networkName = network.getName();
                networkIdShort = manipulatorData.networkId.toString().substring(0, 8) + "...";
                status = ChatColor.GREEN + "已连接";
            }
        }

        int insertedDisks = manipulatorData.getInsertedDiskCount();
        int totalCapacity = 0;
        int usedSpace = 0;

        for (UUID diskUuid : manipulatorData.slots) {
            if (diskUuid != null) {
                totalCapacity += diskManager.getDiskCapacity(diskUuid);
                List<DiskItem> items = diskManager.getDiskData(diskUuid);
                usedSpace += diskManager.getTotalItems(items);
            }
        }

        int percentage = totalCapacity > 0 ? (usedSpace * 100) / totalCapacity : 0;

        ItemStack compass = new ItemBuilder(Material.COMPASS)
                .setName(ChatColor.GOLD + "磁盘操纵器信息")
                .setLore(
                        ChatColor.GRAY + "已连接网络: " + ChatColor.WHITE + networkName,
                        ChatColor.GRAY + "网络ID: " + ChatColor.WHITE + networkIdShort,
                        "",
                        ChatColor.GRAY + "磁盘数量: " + ChatColor.WHITE + insertedDisks + "/8",
                        ChatColor.GRAY + "总容量: " + ChatColor.WHITE + totalCapacity + " 物品",
                        ChatColor.GRAY + "已用空间: " + ChatColor.WHITE + usedSpace + " 物品",
                        ChatColor.GRAY + "使用率: " + ChatColor.WHITE + percentage + "%",
                        createProgressBar(percentage),
                        "",
                        ChatColor.GRAY + "状态: " + status
                )
                .build();

        setItem(INFO_SLOT, compass);
    }

    private String createProgressBar(int percentage) {
        int filled = Math.min(percentage * 20 / 100, 20);
        int remain = 20 - filled;

        ChatColor fillColor = percentage < 80 ? ChatColor.BLUE :
                percentage < 95 ? ChatColor.YELLOW :
                ChatColor.RED;

        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < filled; i++) bar.append(fillColor).append("|");
        for (int i = 0; i < remain; i++) bar.append(ChatColor.GRAY).append("|");
        return bar.toString();
    }

    private void setupDiskSlots() {
        for (int slotIndex = DISK_SLOT_START; slotIndex <= DISK_SLOT_END; slotIndex++) {
            int diskIndex = slotIndex - SLOT_TO_INDEX_OFFSET;
            UUID diskUuid = manipulatorData.slots[diskIndex];

            if (diskUuid != null) {
                setDiskItem(slotIndex, diskIndex, diskUuid);
            } else {
                setEmptySlot(slotIndex, diskIndex);
            }

            // 点击磁盘槽：取出或放入
            setClickAction(slotIndex, (p, item, slot, clickType) -> {
                handleDiskSlotClick(slot);
            });
        }
    }

    private void setDiskItem(int slotIndex, int diskIndex, UUID diskUuid) {
        ItemStack diskItem = diskManager.getDiskItemFromUUID(diskUuid);
        if (diskItem == null) {
            setEmptySlot(slotIndex, diskIndex);
            return;
        }

        List<DiskItem> items = diskManager.getDiskData(diskUuid);
        int itemCount = diskManager.getTotalItems(items);
        int maxCap = diskManager.getDiskCapacity(diskUuid);
        int usagePercent = maxCap > 0 ? (itemCount * 100) / maxCap : 0;

        ItemStack displayItem = new ItemBuilder(diskItem.clone())
                .setName(ChatColor.WHITE + "磁盘 #" + (diskIndex + 1))
                .addLore(ChatColor.GRAY + "容量: " + ChatColor.WHITE + itemCount + "/" + maxCap)
                .addLore(ChatColor.GRAY + "使用率: " + ChatColor.WHITE + usagePercent + "%")
                .addLore("")
                .addLore(ChatColor.YELLOW + "点击取出磁盘")
                .build();

        setItem(slotIndex, displayItem);
    }

    private void setEmptySlot(int slotIndex, int diskIndex) {
        ItemStack emptySlot = new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                .setName(ChatColor.GRAY + "空磁盘槽 #" + (diskIndex + 1))
                .setLore(
                        ChatColor.DARK_GRAY + "点击放入磁盘",
                        ChatColor.DARK_GRAY + "Shift+点击背包磁盘快速放入"
                )
                .build();
        setItem(slotIndex, emptySlot);
    }

    // ==================== 磁盘槽点击处理 ====================

    private void handleDiskSlotClick(int slotIndex) {
        int diskIndex = slotIndex - SLOT_TO_INDEX_OFFSET;
        UUID diskUuid = manipulatorData.slots[diskIndex];

        if (diskUuid != null) {
            // 有磁盘 → 取出到背包
            removeDiskToInventory(diskIndex);
        } else {
            // 空槽 → 从鼠标或主手放入
            insertDiskFromCursorOrHand(diskIndex);
        }
    }

    /**
     * 取出磁盘到玩家背包（不掉落在地上）
     */
    private void removeDiskToInventory(int diskIndex) {
        UUID diskUuid = manipulatorData.removeDisk(diskIndex);
        if (diskUuid == null) return;

        // 先保存数据
        plugin.getDatabaseManager().saveDiskManipulatorToDB(manipulatorData);

        // 创建磁盘物品
        ItemStack diskItem = diskManager.getDiskItemFromUUID(diskUuid);
        if (diskItem == null) {
            player.sendMessage(ChatColor.RED + "磁盘数据异常！");
            return;
        }

        // 尝试放入玩家背包
        int remaining = addItemToInventory(diskItem);

        if (remaining > 0) {
            // 背包满了，掉落在地上
            player.getWorld().dropItemNaturally(player.getLocation(), diskItem);
            player.sendMessage(ChatColor.YELLOW + "背包已满！磁盘已掉落在地上");
        } else {
            player.sendMessage(ChatColor.GREEN + "已取出磁盘到背包！");
        }

        update();
    }

    /**
     * 将物品加入玩家背包，返回剩余数量
     */
    private int addItemToInventory(ItemStack item) {
        if (item == null) return 0;

        // 先尝试堆叠到已有物品
        int remaining = item.getAmount();
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack slotItem = player.getInventory().getItem(i);
            if (slotItem != null && slotItem.isSimilar(item) && slotItem.getAmount() < slotItem.getMaxStackSize()) {
                int canAdd = Math.min(slotItem.getMaxStackSize() - slotItem.getAmount(), remaining);
                slotItem.setAmount(slotItem.getAmount() + canAdd);
                remaining -= canAdd;
                if (remaining <= 0) return 0;
            }
        }

        // 放入空槽位
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            if (player.getInventory().getItem(i) == null || player.getInventory().getItem(i).getType() == Material.AIR) {
                ItemStack toAdd = item.clone();
                toAdd.setAmount(Math.min(remaining, toAdd.getMaxStackSize()));
                player.getInventory().setItem(i, toAdd);
                remaining -= toAdd.getAmount();
                if (remaining <= 0) return 0;
            }
        }

        return remaining;
    }

    /**
     * 从鼠标或主手插入磁盘到空槽
     */
    private void insertDiskFromCursorOrHand(int diskIndex) {
        // 1. 优先检查鼠标上的物品（左键拿起后）
        ItemStack sourceItem = player.getItemOnCursor();

        // 2. 如果鼠标上没有，检查主手
        if (sourceItem == null || sourceItem.getType() == Material.AIR) {
            sourceItem = player.getInventory().getItemInMainHand();
        }

        // 3. 还是没有 → 提示
        if (sourceItem == null || sourceItem.getType() == Material.AIR) {
            player.sendMessage(ChatColor.YELLOW + "请先手持或拿起磁盘！");
            return;
        }

        // 4. 检查是否是磁盘
        if (!diskManager.isDisk(sourceItem)) {
            player.sendMessage(ChatColor.RED + "只能放入磁盘物品！");
            return;
        }

        // 5. 获取磁盘UUID
        UUID diskUuid = diskManager.getDiskUuidFromItem(sourceItem);
        if (diskUuid == null) {
            player.sendMessage(ChatColor.RED + "磁盘数据无效！");
            return;
        }

        // 6. 插入磁盘
        manipulatorData.slots[diskIndex] = diskUuid;
        plugin.getDatabaseManager().saveDiskManipulatorToDB(manipulatorData);

        // 7. 减少物品数量（从鼠标或主手）
        removeOneItemFromSource(sourceItem);

        player.sendMessage(ChatColor.GREEN + "已插入磁盘！");
        update();
    }

    /**
     * 从物品来源减少一个数量（鼠标或主手）
     */
    private void removeOneItemFromSource(ItemStack item) {
        // 检查是否在鼠标上
        ItemStack cursorItem = player.getItemOnCursor();
        if (cursorItem != null && cursorItem.isSimilar(item) && cursorItem.getAmount() == item.getAmount()) {
            // 物品在鼠标上
            int newAmount = cursorItem.getAmount() - 1;
            if (newAmount <= 0) {
                player.setItemOnCursor(null);
            } else {
                cursorItem.setAmount(newAmount);
                player.setItemOnCursor(cursorItem);
            }
            return;
        }

        // 否则从主手移除
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem != null && handItem.isSimilar(item)) {
            int newAmount = handItem.getAmount() - 1;
            if (newAmount <= 0) {
                player.getInventory().setItemInMainHand(null);
            } else {
                handItem.setAmount(newAmount);
            }
        }
    }

    // ==================== 背包点击处理（Shift+点击快速放入） ====================

    @Override
    protected void handlePlayerInventoryClick(InventoryClickEvent event, int slot, ItemStack item, ClickType clickType) {
        // 只处理背包区域（slots >= inventory.getSize()）
        if (slot < inventory.getSize()) {
            event.setCancelled(true);
            return;
        }

        // 点击的物品必须有效
        if (item == null || item.getType() == Material.AIR) {
            event.setCancelled(false);
            return;
        }

        // 检查点击的物品是否是磁盘
        if (!diskManager.isDisk(item)) {
            // 非磁盘物品，放行让玩家正常操作
            event.setCancelled(false);
            return;
        }

        // === Shift + 点击磁盘：快速放入第一个空槽 ===
        if (clickType == ClickType.SHIFT_LEFT_CLICK || clickType == ClickType.SHIFT_RIGHT_CLICK) {
            event.setCancelled(true);

            // 找第一个空槽位
            int emptySlot = manipulatorData.getFirstEmptySlot();
            if (emptySlot == -1) {
                player.sendMessage(ChatColor.RED + "磁盘操纵器已满！");
                return;
            }

            UUID diskUuid = diskManager.getDiskUuidFromItem(item);
            if (diskUuid == null) {
                player.sendMessage(ChatColor.RED + "磁盘数据无效！");
                return;
            }

            // 放入磁盘
            manipulatorData.slots[emptySlot] = diskUuid;
            plugin.getDatabaseManager().saveDiskManipulatorToDB(manipulatorData);

            // 从背包移除一个磁盘
            if (item.getAmount() <= 1) {
                event.setCurrentItem(null);
            } else {
                item.setAmount(item.getAmount() - 1);
                event.setCurrentItem(item);
            }

            player.sendMessage(ChatColor.GREEN + "已插入磁盘！");
            update();
            return;
        }

        // === 普通点击（左键/右键） ===
        // 鼠标拿起磁盘，让玩家可以点击磁盘槽放入
        event.setCancelled(false);
    }

    // ==================== 拖拽处理 ====================

    @Override
    public void handleDrag(InventoryDragEvent event) {
        // 检查是否涉及 GUI 槽位
        Set<Integer> guiSlots = new java.util.HashSet<>();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < inventory.getSize()) {
                guiSlots.add(rawSlot);
            }
        }

        // 只在背包内拖拽 → 放行
        if (guiSlots.isEmpty()) {
            event.setCancelled(false);
            return;
        }

        ItemStack dragged = event.getOldCursor();

        // 检查拖拽物品是否是磁盘
        if (dragged == null || !diskManager.isDisk(dragged)) {
            event.setCancelled(true);
            if (dragged != null && dragged.getType() != Material.AIR) {
                player.sendMessage(ChatColor.RED + "只能拖拽磁盘到磁盘槽！");
            }
            return;
        }

        // 过滤出有效的空磁盘槽
        Set<Integer> validSlots = new java.util.HashSet<>();
        for (int slot : guiSlots) {
            if (slot < DISK_SLOT_START || slot > DISK_SLOT_END) continue;
            int diskIndex = slot - SLOT_TO_INDEX_OFFSET;
            if (manipulatorData.slots[diskIndex] == null) {
                validSlots.add(slot);
            }
        }

        if (validSlots.isEmpty()) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.YELLOW + "没有可用的空磁盘槽位！");
            return;
        }

        // 执行拖拽放置
        event.setCancelled(true);

        UUID diskUuid = diskManager.getDiskUuidFromItem(dragged);
        if (diskUuid == null) {
            player.sendMessage(ChatColor.RED + "磁盘数据无效！");
            return;
        }

        int placed = 0;
        for (int slot : validSlots) {
            if (placed >= dragged.getAmount()) break;
            int diskIndex = slot - SLOT_TO_INDEX_OFFSET;
            manipulatorData.slots[diskIndex] = diskUuid;
            placed++;
        }

        // 更新光标物品
        int remaining = dragged.getAmount() - placed;
        if (remaining <= 0) {
            event.setCursor(null);
        } else {
            ItemStack newCursor = dragged.clone();
            newCursor.setAmount(remaining);
            event.setCursor(newCursor);
        }

        plugin.getDatabaseManager().saveDiskManipulatorToDB(manipulatorData);
        player.sendMessage(ChatColor.GREEN + "已插入 " + placed + " 个磁盘！");
        update();
    }

    // ==================== 更新 ====================

    @Override
    public void update() {
        if (!isOpen()) return;
        inventory.clear();
        clickActions.clear();
        initialize();
        player.updateInventory();
    }
}