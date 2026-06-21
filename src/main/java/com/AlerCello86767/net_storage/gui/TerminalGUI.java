package com.AlerCello86767.net_storage.gui;

import com.AlerCello86767.net_storage.Net_storage;
import com.AlerCello86767.net_storage.controller.DiskManipulatorData;
import com.AlerCello86767.net_storage.controller.ExternalStorageBusData;
import com.AlerCello86767.net_storage.controller.TerminalData;
import com.AlerCello86767.net_storage.disk.DiskItem;
import com.AlerCello86767.net_storage.disk.DiskManager;
import com.AlerCello86767.net_storage.network.StorageNetwork;
import com.AlerCello86767.net_storage.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 终端 GUI
 * 提供统一的物品存取界面
 */
public class TerminalGUI extends BaseGUI {

    private final Net_storage plugin;
    private final TerminalData terminalData;
    private final DiskManager diskManager;
    private final UUID networkId;
    private final Location blockLocation;

    private int currentPage = 1;
    private int totalPages = 1;

    // 槽位定义
    private static final int INFO_SLOT = 0;
    private static final int ITEM_AREA_START = 9;
    private static final int ITEM_AREA_END = 44;
    private static final int ITEMS_PER_PAGE = 36;
    private static final int PREV_PAGE_SLOT = 45;
    private static final int PAGE_INFO_SLOT = 49;
    private static final int NEXT_PAGE_SLOT = 53;

    // 物品数据
    private Map<String, DiskItem> networkItemsMap = new ConcurrentHashMap<>();
    private List<DiskItem> displayItems = new ArrayList<>();
    private List<UUID> networkDisks = new ArrayList<>();

    private final AtomicBoolean isUpdating = new AtomicBoolean(false);
    private BukkitTask refreshTask;

    // ==================== 构造方法 ====================

    public TerminalGUI(Player player, Net_storage plugin, TerminalData terminalData, Location blockLocation) {
        super(player, 54, buildTitle(plugin, terminalData.networkId));

        this.plugin = plugin;
        this.terminalData = terminalData;
        this.diskManager = plugin.getDiskManager();
        this.networkId = terminalData.networkId;
        this.blockLocation = blockLocation;

        refreshNetworkData();
        totalPages = Math.max(1, (int) Math.ceil(displayItems.size() / (double) ITEMS_PER_PAGE));

        initialize();
        startRefreshTask();
    }

    // ==================== 生命周期 ====================

    private void startRefreshTask() {
        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (player.isOnline() && player.getOpenInventory().getTitle().equals(getTitle())) {
                refreshNetworkData();
                update();
            } else {
                stopRefreshTask();
            }
        }, 30L, 30L);
    }

    private void stopRefreshTask() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }

    private static String buildTitle(Net_storage plugin, UUID networkId) {
        String networkName = "未连接";
        if (networkId != null && plugin != null) {
            StorageNetwork network = plugin.getNetworkManager().getNetwork(networkId);
            if (network != null) {
                networkName = network.getName();
            }
        }
        return ChatColor.translateAlternateColorCodes('&', "&a终端 - " + networkName);
    }

    @Override
    public void close() {
        stopRefreshTask();
        super.close();
    }

    // ==================== 数据刷新 ====================

    public synchronized void refreshNetworkData() {
        isUpdating.set(true);

        try {
            networkItemsMap.clear();
            displayItems.clear();
            networkDisks.clear();

            if (networkId == null) return;

            // 获取所有磁盘操纵器
            List<DiskManipulatorData> manipulators = plugin.getControllerManager().getDiskManipulatorsByNetwork(networkId);

            for (DiskManipulatorData manipulator : manipulators) {
                if (manipulator.slots == null) continue;

                for (UUID diskUuid : manipulator.slots) {
                    if (diskUuid == null) continue;

                    networkDisks.add(diskUuid);
                    List<DiskItem> diskItems = diskManager.getDiskData(diskUuid);

                    for (DiskItem item : diskItems) {
                        item.setDiskUuid(diskUuid);
                        String key = diskUuid.toString() + "#" + item.getSerializedItem();
                        networkItemsMap.put(key, item);
                    }
                }
            }

            // 加载外部存储总线的容器物品
            List<ExternalStorageBusData> externalBuses = plugin.getControllerManager().getExternalStorageBusesByNetwork(networkId);
            for (ExternalStorageBusData busData : externalBuses) {
                try {
                    Location containerLoc = busData.getContainerLocation();
                    if (containerLoc == null) continue;

                    Block containerBlock = containerLoc.getBlock();
                    if (containerBlock == null) continue;

                    BlockState state = containerBlock.getState();
                    if (state == null) continue;

                    if (!(state instanceof org.bukkit.block.Container)) {
                        plugin.getLogger().warning("外部存储总线绑定的容器无效: " + busData.busUuid);
                        continue;
                    }

                    org.bukkit.block.Container container = (org.bukkit.block.Container) state;
                    org.bukkit.inventory.Inventory inventory = container.getInventory();
                    if (inventory == null) continue;

                    for (int i = 0; i < inventory.getSize(); i++) {
                        ItemStack item = inventory.getItem(i);
                        if (item == null || item.getType() == Material.AIR) continue;

                        DiskItem diskItem = DiskItem.fromItemStack(item, item.getAmount());
                        diskItem.setExternalBus(busData.busUuid.toString());
                        diskItem.setSlotIndex(i);

                        String key = "external#" + busData.busUuid + "#" + i;
                        networkItemsMap.put(key, diskItem);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("加载外部存储总线数据失败: " + busData.busUuid + " - " + e.getMessage());
                }
            }

            // 按 Material 名称排序
            displayItems = new ArrayList<>(networkItemsMap.values());
            displayItems.sort(Comparator.comparingInt(item -> item.getMaterial().ordinal()));
        } finally {
            isUpdating.set(false);
        }
    }

    // ==================== GUI 初始化 ====================

    @Override
    public void initialize() {
        setupInfoSlot();
        setupTopRowFiller();
        setupItems();
        setupPagination();
        setupBottomRowFiller();
    }

    private void setupInfoSlot() {
        String networkName = "未连接";
        String networkIdShort = "无";

        if (networkId != null) {
            StorageNetwork network = plugin.getNetworkManager().getNetwork(networkId);
            if (network != null) {
                networkName = network.getName();
                networkIdShort = networkId.toString().substring(0, 8) + "...";
            }
        }

        int manipulatorCount = plugin.getControllerManager().getDiskManipulatorsByNetwork(networkId).size();
        int diskCount = networkDisks.size();
        int externalBusCount = plugin.getControllerManager().getExternalStorageBusesByNetwork(networkId).size();

        int diskCapacity = 0;
        for (UUID diskUuid : networkDisks) {
            diskCapacity += diskManager.getDiskCapacity(diskUuid);
        }
        int diskUsedSpace = calculateUsedSpace();
        int diskRemaining = diskCapacity - diskUsedSpace;

        int containerRemaining = calculateExternalContainerRemainingSpace();
        int totalCapacity = diskCapacity;
        int totalRemaining = diskRemaining + containerRemaining;
        int totalUsed = diskUsedSpace;

        int percentage = totalCapacity > 0 ? (totalUsed * 100) / totalCapacity : 0;
        String progressBar = createProgressBar(percentage);

        ItemStack compass = new ItemBuilder(Material.COMPASS)
                .setName(ChatColor.GOLD + "网络存储信息")
                .setLore(
                        ChatColor.GRAY + "网络: " + ChatColor.WHITE + networkName,
                        ChatColor.GRAY + "网络ID: " + ChatColor.WHITE + networkIdShort,
                        "",
                        ChatColor.GRAY + "磁盘操纵器: " + ChatColor.WHITE + manipulatorCount,
                        ChatColor.GRAY + "已插入磁盘: " + ChatColor.WHITE + diskCount,
                        ChatColor.DARK_PURPLE + "外部存储总线: " + ChatColor.WHITE + externalBusCount,
                        "",
                        ChatColor.GRAY + "磁盘容量: " + ChatColor.WHITE + diskCapacity + " 物品",
                        ChatColor.GRAY + "磁盘已用: " + ChatColor.WHITE + diskUsedSpace + " 物品",
                        ChatColor.GRAY + "磁盘剩余: " + ChatColor.WHITE + diskRemaining + " 物品",
                        ChatColor.DARK_PURPLE + "容器剩余: " + ChatColor.WHITE + containerRemaining + " 物品",
                        "",
                        ChatColor.GRAY + "总剩余空间: " + ChatColor.WHITE + totalRemaining + " 物品",
                        ChatColor.GRAY + "使用率: " + ChatColor.WHITE + percentage + "%",
                        progressBar,
                        ChatColor.GRAY + "物品种类: " + ChatColor.WHITE + displayItems.size(),
                        "",
                        ChatColor.YELLOW + "点击物品取出 | 点击空位存入 | Shift+点击背包存入"
                )
                .build();

        setItem(INFO_SLOT, compass);
    }

    private int calculateUsedSpace() {
        int total = 0;
        for (UUID diskUuid : networkDisks) {
            List<DiskItem> items = diskManager.getDiskData(diskUuid);
            total += diskManager.getTotalItems(items);
        }
        return total;
    }

    private String createProgressBar(int percentage) {
        int filled = (percentage * 20) / 100;
        int remain = 20 - filled;

        ChatColor fillColor = percentage < 80 ? ChatColor.BLUE :
                percentage < 95 ? ChatColor.YELLOW :
                ChatColor.RED;

        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < filled; i++) bar.append(fillColor).append("|");
        for (int i = 0; i < remain; i++) bar.append(ChatColor.GRAY).append("|");
        return bar.toString();
    }

    private void setupTopRowFiller() {
        ItemStack glassPane = new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                .setName(" ")
                .build();
        for (int i = 1; i <= 8; i++) setItem(i, glassPane);
    }

    // ==================== 物品列表 ====================

    private void setupItems() {
        if (displayItems.isEmpty()) {
            ItemStack emptyPlaceholder = new ItemBuilder(Material.BARRIER)
                    .setName(ChatColor.GRAY + "网络存储为空")
                    .setLore(ChatColor.DARK_GRAY + "请先在磁盘操纵器中插入磁盘")
                    .build();
            for (int i = ITEM_AREA_START; i <= ITEM_AREA_END; i++) {
                setItem(i, emptyPlaceholder);
            }
            return;
        }

        int startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, displayItems.size());

        int slot = ITEM_AREA_START;
        for (int i = startIndex; i < endIndex && slot <= ITEM_AREA_END; i++) {
            DiskItem diskItem = displayItems.get(i);

            String displayName = diskItem.getDisplayName();
            if (displayName == null) {
                displayName = ChatColor.WHITE + diskItem.getMaterial().name();
            } else {
                displayName = ChatColor.WHITE + displayName;
            }

// ✅ 使用完整 ItemStack（包含 NBT）来显示
            ItemStack displayItem = diskItem.toItemStack();
            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.getLore() != null ? meta.getLore() : new ArrayList<>();
                lore.add("");
                lore.add(ChatColor.GRAY + "数量: " + ChatColor.WHITE + diskItem.getAmount() + " 个");
                lore.add("");
                lore.add(ChatColor.YELLOW + "左键/右键点击取出 1 个");
                lore.add(ChatColor.YELLOW + "Shift+点击取出 64 个");
                meta.setLore(lore);
                displayItem.setItemMeta(meta);
            }
            setItem(slot, displayItem);

            setItem(slot, displayItem);

            String itemKey = diskItem.isExternalItem()
                    ? "external#" + diskItem.getExternalBus() + "#" + diskItem.getSlotIndex()
                    : diskItem.getDiskUuid().toString() + "#" + diskItem.getSerializedItem();

            final String key = itemKey;
            setClickAction(slot, (p, item, slotNum, clickType) -> {
                handleItemClick(key, clickType);
            });

            slot++;
        }

        // 填充空位（这些空位也支持点击存入）
        ItemStack emptyPane = new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                .setName(ChatColor.GRAY + "点击放入物品")
                .setLore(ChatColor.DARK_GRAY + "将物品拖拽或点击空位存入")
                .build();
        while (slot <= ITEM_AREA_END) {
            setItem(slot, emptyPane);
            // ✅ 空槽位点击 = 存入鼠标上的物品
            final int finalSlot = slot;
            setClickAction(slot, (p, item, slotNum, clickType) -> {
                handleEmptySlotClick();
            });
            slot++;
        }
    }

    // ==================== 取出物品 ====================

    private void handleItemClick(String itemKey, ClickType clickType) {
        if (isUpdating.get()) {
            player.sendMessage(ChatColor.RED + "界面正在刷新，请稍后...");
            return;
        }

        DiskItem diskItem = networkItemsMap.get(itemKey);
        if (diskItem == null) {
            player.sendMessage(ChatColor.RED + "物品数据异常，请重新打开终端！");
            return;
        }

        if (diskItem.isExternalItem()) {
            handleExternalItemClick(diskItem, clickType);
            return;
        }

        UUID diskUuid = diskItem.getDiskUuid();
        if (diskUuid == null) {
            player.sendMessage(ChatColor.RED + "物品数据异常！");
            return;
        }

        // Shift+点击取出 64 个，普通点击取出 1 个
        int removeAmount = (clickType == ClickType.SHIFT_LEFT_CLICK || clickType == ClickType.SHIFT_RIGHT_CLICK)
                ? Math.min(64, diskItem.getAmount()) : 1;

        Material material = diskItem.getMaterial();
        int playerSpace = calculatePlayerSpace(player, material);
        int canTake = Math.min(removeAmount, playerSpace);

        if (canTake <= 0) {
            player.sendMessage(ChatColor.RED + "背包已满！");
            return;
        }

        // 获取磁盘数据并移除物品
        List<DiskItem> diskData = diskManager.getDiskData(diskUuid);

        DiskItem targetItem = null;
        for (DiskItem di : diskData) {
            if (di.getSerializedItem().equals(diskItem.getSerializedItem())) {
                targetItem = di;
                break;
            }
        }

        if (targetItem == null) {
            player.sendMessage(ChatColor.RED + "物品数据异常，请重新打开终端！");
            return;
        }

        int newAmount = targetItem.getAmount() - canTake;
        if (newAmount <= 0) {
            diskData.remove(targetItem);
        } else {
            targetItem.setAmount(newAmount);
        }

        diskManager.saveDiskData(diskUuid, diskData);

        ItemStack giveItem = diskItem.toItemStack();
        giveItem.setAmount(canTake);
        player.getInventory().addItem(giveItem);

        String itemName = diskItem.getDisplayName() != null ? diskItem.getDisplayName() : material.name();
        if (canTake < removeAmount) {
            player.sendMessage(ChatColor.YELLOW + "背包空间不足！已取出 " + ChatColor.WHITE + canTake + ChatColor.YELLOW + " 个 " + itemName);
        } else {
            player.sendMessage(ChatColor.GREEN + "已取出 " + ChatColor.WHITE + canTake + ChatColor.GREEN + " 个 " + itemName);
        }

        refreshAndUpdate();
    }

    private void handleExternalItemClick(DiskItem diskItem, ClickType clickType) {
        String busUuidStr = diskItem.getExternalBus();
        int slotIndex = diskItem.getSlotIndex();

        UUID busUuid;
        try {
            busUuid = UUID.fromString(busUuidStr);
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "外部存储总线UUID异常！");
            return;
        }

        ExternalStorageBusData busData = plugin.getControllerManager().getExternalStorageBusByUuid(busUuid);
        if (busData == null) {
            player.sendMessage(ChatColor.RED + "外部存储总线数据异常！");
            return;
        }

        Location containerLoc = busData.getContainerLocation();
        if (containerLoc == null) {
            player.sendMessage(ChatColor.RED + "容器位置异常！");
            return;
        }

        Block containerBlock = containerLoc.getBlock();
        if (containerBlock == null || !(containerBlock.getState() instanceof org.bukkit.block.Container)) {
            player.sendMessage(ChatColor.RED + "容器不存在或已被破坏！");
            return;
        }

        org.bukkit.block.Container container = (org.bukkit.block.Container) containerBlock.getState();
        org.bukkit.inventory.Inventory inventory = container.getInventory();

        ItemStack containerItem = inventory.getItem(slotIndex);
        if (containerItem == null || containerItem.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "容器物品已被移除！");
            refreshAndUpdate();
            return;
        }

        // Shift+点击取出 64 个
        int removeAmount = (clickType == ClickType.SHIFT_LEFT_CLICK || clickType == ClickType.SHIFT_RIGHT_CLICK)
                ? Math.min(64, containerItem.getAmount()) : 1;

        Material material = containerItem.getType();
        int playerSpace = calculatePlayerSpace(player, material);
        int canTake = Math.min(removeAmount, playerSpace);

        if (canTake <= 0) {
            player.sendMessage(ChatColor.RED + "背包已满！");
            return;
        }

        ItemStack giveItem = containerItem.clone();
        giveItem.setAmount(canTake);

        containerItem.setAmount(containerItem.getAmount() - canTake);
        if (containerItem.getAmount() <= 0) {
            inventory.setItem(slotIndex, null);
        }

        player.getInventory().addItem(giveItem);

        String itemName = containerItem.hasItemMeta() && containerItem.getItemMeta().hasDisplayName()
                ? containerItem.getItemMeta().getDisplayName() : material.name();
        player.sendMessage(ChatColor.GREEN + "已从容器取出 " + ChatColor.WHITE + canTake + ChatColor.GREEN + " 个 " + itemName);

        refreshAndUpdate();
    }

    private int calculatePlayerSpace(Player player, Material material) {
        int space = 0;
        int maxStackSize = material.getMaxStackSize();

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) {
                space += maxStackSize;
            } else if (item.isSimilar(new ItemStack(material))) {
                space += maxStackSize - item.getAmount();
            }
        }
        return space;
    }

    // ==================== 存入物品 ====================

    /**
     * 处理空槽位点击 - 存入鼠标上的物品
     */
    private void handleEmptySlotClick() {
        if (isUpdating.get()) {
            player.sendMessage(ChatColor.RED + "界面正在刷新，请稍后...");
            return;
        }

        ItemStack cursorItem = player.getItemOnCursor();
        if (cursorItem == null || cursorItem.getType() == Material.AIR) {
            player.sendMessage(ChatColor.YELLOW + "请先拿起要存入的物品！");
            return;
        }

        // 检查空间
        int remaining = getTotalRemainingSpace();
        if (remaining <= 0) {
            player.sendMessage(ChatColor.RED + "网络存储空间不足！");
            return;
        }

        int amount = Math.min(cursorItem.getAmount(), remaining);
        if (amount <= 0) {
            player.sendMessage(ChatColor.RED + "空间不足！");
            return;
        }

        // 执行存入
        isUpdating.set(true);
        try {
            int stored = storeItem(cursorItem.clone(), amount);

            if (stored > 0) {
                // 从鼠标移除物品
                int remainingInCursor = cursorItem.getAmount() - stored;
                if (remainingInCursor <= 0) {
                    player.setItemOnCursor(null);
                } else {
                    cursorItem.setAmount(remainingInCursor);
                    player.setItemOnCursor(cursorItem);
                }

                String itemName = cursorItem.hasItemMeta() && cursorItem.getItemMeta().hasDisplayName()
                        ? cursorItem.getItemMeta().getDisplayName()
                        : cursorItem.getType().name();

                player.sendMessage(ChatColor.GREEN + "已存入 " + ChatColor.WHITE + stored + ChatColor.GREEN + " 个 " + itemName);
                refreshAndUpdate();
            } else {
                player.sendMessage(ChatColor.RED + "存入失败！");
            }
        } finally {
            isUpdating.set(false);
        }
    }

    /**
     * 将物品存入网络存储
     */
    private int storeItem(ItemStack item, int amount) {
        if (item == null || item.getType() == Material.AIR || amount <= 0) {
            return 0;
        }

        int remaining = getTotalRemainingSpace();
        if (remaining <= 0) return 0;

        int toStore = Math.min(amount, remaining);
        return distributeItemToDisks(item, toStore);
    }

    private int distributeItemToDisks(ItemStack item, int amount) {
        if (amount <= 0) return 0;

        int stored = 0;
        Map<UUID, List<DiskItem>> modifiedDisks = new HashMap<>();

        // 第一步：优先堆叠到已有相同物品的磁盘
        for (UUID diskUuid : networkDisks) {
            if (amount - stored <= 0) break;

            List<DiskItem> diskItems = diskManager.getDiskData(diskUuid);
            int usedSpace = diskManager.getTotalItems(diskItems);
            int remainingSpace = diskManager.getDiskCapacity(diskUuid) - usedSpace;

            if (remainingSpace <= 0) continue;

            for (DiskItem diskItem : diskItems) {
                if (diskItem.matchesItemStack(item)) {
                    int canAdd = Math.min(remainingSpace, amount - stored);
                    if (canAdd > 0) {
                        diskItem.addAmount(canAdd);
                        stored += canAdd;
                        modifiedDisks.put(diskUuid, diskItems);
                    }
                    break;
                }
            }
        }

        // 第二步：分散存储到有空位的磁盘
        for (UUID diskUuid : networkDisks) {
            if (amount - stored <= 0) break;

            List<DiskItem> diskItems = diskManager.getDiskData(diskUuid);
            int usedSpace = diskManager.getTotalItems(diskItems);
            int remainingSpace = diskManager.getDiskCapacity(diskUuid) - usedSpace;

            if (remainingSpace <= 0) continue;

            int canAdd = Math.min(remainingSpace, amount - stored);
            if (canAdd > 0) {
                DiskItem newItem = DiskItem.fromItemStack(item, canAdd);
                newItem.setDiskUuid(diskUuid);
                diskItems.add(newItem);
                stored += canAdd;
                modifiedDisks.put(diskUuid, diskItems);
            }
        }

        // 保存修改过的磁盘
        for (Map.Entry<UUID, List<DiskItem>> entry : modifiedDisks.entrySet()) {
            diskManager.saveDiskData(entry.getKey(), entry.getValue());
        }

        // 第三步：存入外部容器
        if (amount - stored > 0) {
            stored += distributeItemToExternalContainers(item, amount - stored);
        }

        return stored;
    }

    private int distributeItemToExternalContainers(ItemStack item, int amount) {
        if (amount <= 0 || networkId == null) return 0;

        int stored = 0;
        List<ExternalStorageBusData> externalBuses = plugin.getControllerManager().getExternalStorageBusesByNetwork(networkId);

        for (ExternalStorageBusData busData : externalBuses) {
            if (amount - stored <= 0) break;

            Location containerLoc = busData.getContainerLocation();
            if (containerLoc == null) continue;

            Block containerBlock = containerLoc.getBlock();
            if (containerBlock == null || !(containerBlock.getState() instanceof org.bukkit.block.Container)) continue;

            org.bukkit.block.Container container = (org.bukkit.block.Container) containerBlock.getState();
            org.bukkit.inventory.Inventory inventory = container.getInventory();

            // 堆叠到已有物品
            for (int i = 0; i < inventory.getSize(); i++) {
                if (amount - stored <= 0) break;

                ItemStack slotItem = inventory.getItem(i);
                if (slotItem != null && slotItem.isSimilar(item) && slotItem.getAmount() < slotItem.getMaxStackSize()) {
                    int canAdd = Math.min(slotItem.getMaxStackSize() - slotItem.getAmount(), amount - stored);
                    slotItem.setAmount(slotItem.getAmount() + canAdd);
                    stored += canAdd;
                }
            }

            // 存入空槽位
            for (int i = 0; i < inventory.getSize(); i++) {
                if (amount - stored <= 0) break;

                ItemStack slotItem = inventory.getItem(i);
                if (slotItem == null || slotItem.getType() == Material.AIR) {
                    int canAdd = Math.min(item.getMaxStackSize(), amount - stored);
                    ItemStack newItem = item.clone();
                    newItem.setAmount(canAdd);
                    inventory.setItem(i, newItem);
                    stored += canAdd;
                }
            }
        }

        return stored;
    }

    public int getTotalRemainingSpace() {
        int diskCapacity = 0;
        for (UUID diskUuid : networkDisks) {
            diskCapacity += diskManager.getDiskCapacity(diskUuid);
        }
        int diskUsedSpace = calculateUsedSpace();
        int diskRemaining = diskCapacity - diskUsedSpace;
        int containerRemaining = calculateExternalContainerRemainingSpace();
        return diskRemaining + containerRemaining;
    }

    private int calculateExternalContainerRemainingSpace() {
        if (networkId == null) return 0;

        int remaining = 0;
        List<ExternalStorageBusData> externalBuses = plugin.getControllerManager().getExternalStorageBusesByNetwork(networkId);

        for (ExternalStorageBusData busData : externalBuses) {
            Location containerLoc = busData.getContainerLocation();
            if (containerLoc == null) continue;

            Block containerBlock = containerLoc.getBlock();
            if (containerBlock == null || !(containerBlock.getState() instanceof org.bukkit.block.Container)) continue;

            org.bukkit.block.Container container = (org.bukkit.block.Container) containerBlock.getState();
            org.bukkit.inventory.Inventory inventory = container.getInventory();

            for (int i = 0; i < inventory.getSize(); i++) {
                ItemStack slotItem = inventory.getItem(i);
                if (slotItem == null || slotItem.getType() == Material.AIR) {
                    remaining += 64;
                } else if (slotItem.getAmount() < slotItem.getMaxStackSize()) {
                    remaining += slotItem.getMaxStackSize() - slotItem.getAmount();
                }
            }
        }
        return remaining;
    }

    private void refreshAndUpdate() {
        refreshNetworkData();
        totalPages = Math.max(1, (int) Math.ceil(displayItems.size() / (double) ITEMS_PER_PAGE));
        if (currentPage > totalPages) currentPage = totalPages;
        update();
    }

    // ==================== 翻页控制 ====================

    private void setupPagination() {
        ItemStack prevPageItem;
        if (currentPage > 1) {
            prevPageItem = new ItemBuilder(Material.ARROW)
                    .setName(ChatColor.YELLOW + "上一页")
                    .setLore(ChatColor.GRAY + "当前页: " + currentPage + "/" + totalPages)
                    .build();
            setClickAction(PREV_PAGE_SLOT, (p, item, slot, clickType) -> {
                if (currentPage > 1) { currentPage--; update(); }
            });
        } else {
            prevPageItem = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                    .setName(ChatColor.DARK_GRAY + "已是第一页")
                    .build();
        }
        setItem(PREV_PAGE_SLOT, prevPageItem);

        ItemStack pageInfoItem = new ItemBuilder(Material.BELL)
                .setName(ChatColor.GOLD + "页面信息")
                .setLore(
                        ChatColor.GRAY + "当前页: " + ChatColor.WHITE + currentPage,
                        ChatColor.GRAY + "总页数: " + ChatColor.WHITE + totalPages
                )
                .build();
        setItem(PAGE_INFO_SLOT, pageInfoItem);

        ItemStack nextPageItem;
        if (currentPage < totalPages) {
            nextPageItem = new ItemBuilder(Material.ARROW)
                    .setName(ChatColor.YELLOW + "下一页")
                    .setLore(ChatColor.GRAY + "当前页: " + currentPage + "/" + totalPages)
                    .build();
            setClickAction(NEXT_PAGE_SLOT, (p, item, slot, clickType) -> {
                if (currentPage < totalPages) { currentPage++; update(); }
            });
        } else {
            nextPageItem = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                    .setName(ChatColor.DARK_GRAY + "已是最后一页")
                    .build();
        }
        setItem(NEXT_PAGE_SLOT, nextPageItem);
    }

    private void setupBottomRowFiller() {
        ItemStack glassPane = new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                .setName(" ")
                .build();
        for (int i = 45; i <= 53; i++) {
            if (i != PREV_PAGE_SLOT && i != PAGE_INFO_SLOT && i != NEXT_PAGE_SLOT) {
                setItem(i, glassPane);
            }
        }
    }

    // ==================== 拖拽处理 ====================

    @Override
    public void handleDrag(InventoryDragEvent event) {
        Set<Integer> guiSlots = new HashSet<>();
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

        // 检查是否拖拽到物品显示区域（槽位 9-44）
        boolean hasItemAreaSlot = false;
        for (int slot : guiSlots) {
            if (slot >= ITEM_AREA_START && slot <= ITEM_AREA_END) {
                hasItemAreaSlot = true;
                break;
            }
        }

        if (!hasItemAreaSlot) {
            event.setCancelled(true);
            return;
        }

        ItemStack dragged = event.getOldCursor();
        if (dragged == null || dragged.getType() == Material.AIR) {
            event.setCancelled(false);
            return;
        }

        // ✅ 拖拽到物品区域 = 存入物品
        event.setCancelled(true);

        int remaining = getTotalRemainingSpace();
        if (remaining <= 0) {
            player.sendMessage(ChatColor.RED + "网络存储空间不足！");
            return;
        }

        int amount = Math.min(dragged.getAmount(), remaining);
        if (amount <= 0) {
            player.sendMessage(ChatColor.RED + "空间不足！");
            return;
        }

        isUpdating.set(true);
        try {
            int stored = storeItem(dragged.clone(), amount);

            if (stored > 0) {
                int remainingInCursor = dragged.getAmount() - stored;
                if (remainingInCursor <= 0) {
                    event.setCursor(null);
                } else {
                    ItemStack newCursor = dragged.clone();
                    newCursor.setAmount(remainingInCursor);
                    event.setCursor(newCursor);
                }

                String itemName = dragged.hasItemMeta() && dragged.getItemMeta().hasDisplayName()
                        ? dragged.getItemMeta().getDisplayName()
                        : dragged.getType().name();

                player.sendMessage(ChatColor.GREEN + "已存入 " + ChatColor.WHITE + stored +
                        ChatColor.GREEN + " 个 " + itemName);

                refreshAndUpdate();
            } else {
                player.sendMessage(ChatColor.RED + "存入失败！");
            }
        } finally {
            isUpdating.set(false);
        }
    }

    // ==================== 背包点击处理（Shift+点击存入） ====================

    @Override
    protected void handlePlayerInventoryClick(InventoryClickEvent event, int slot, ItemStack item, ClickType clickType) {
        // GUI 区域的点击由 handleClick 处理
        if (slot < inventory.getSize()) {
            event.setCancelled(true);
            return;
        }

        // 背包区域点击
        if (item == null || item.getType() == Material.AIR) {
            event.setCancelled(false);
            return;
        }

        // ✅ Shift+点击背包物品 → 存入全部
        if (clickType == ClickType.SHIFT_LEFT_CLICK || clickType == ClickType.SHIFT_RIGHT_CLICK) {
            event.setCancelled(true);

            // 检查空间
            int remaining = getTotalRemainingSpace();
            if (remaining <= 0) {
                player.sendMessage(ChatColor.RED + "网络存储空间不足！");
                return;
            }

            // ✅ 存入全部（整组）
            int amount = item.getAmount();
            if (amount > remaining) {
                player.sendMessage(ChatColor.YELLOW + "空间不足！剩余: " + remaining + "，已存入 " + remaining + " 个");
                amount = remaining;
            }

            isUpdating.set(true);
            try {
                int stored = storeItem(item.clone(), amount);

                if (stored > 0) {
                    // 从背包移除物品
                    if (item.getAmount() <= stored) {
                        event.setCurrentItem(null);
                    } else {
                        item.setAmount(item.getAmount() - stored);
                        event.setCurrentItem(item);
                    }

                    String itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                            ? item.getItemMeta().getDisplayName()
                            : item.getType().name();

                    player.sendMessage(ChatColor.GREEN + "已存入 " + ChatColor.WHITE + stored +
                            ChatColor.GREEN + " 个 " + itemName);

                    refreshAndUpdate();
                } else {
                    player.sendMessage(ChatColor.RED + "存入失败！");
                }
            } finally {
                isUpdating.set(false);
            }
            return;
        }

        // ✅ 普通点击（左键/右键）→ 让玩家正常操作背包
        event.setCancelled(false);
    }
    // ==================== 更新 ====================

    @Override
    public synchronized void update() {
        if (isUpdating.get() || !isOpen()) return;

        isUpdating.set(true);
        try {
            inventory.clear();
            clickActions.clear();
            initialize();
            player.updateInventory();
        } finally {
            isUpdating.set(false);
        }
    }
}