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
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 终端 GUI
 * 提供统一的物品存取界面
 * 所有数据通过 DiskManager 获取，不使用独立缓存
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
    
    // 网络的所有物品（聚合自所有磁盘）
    // 使用 Map<diskUuid#serializedKey, DiskItem> 便于快速查找
    private Map<String, DiskItem> networkItemsMap = new ConcurrentHashMap<>();
    
    // 用于显示的物品列表
    private List<DiskItem> displayItems = new ArrayList<>();
    
    // 网络中的磁盘 UUID 列表（用于快速访问）
    private List<UUID> networkDisks = new ArrayList<>();
    
    // 更新锁，防止刷新时点击导致问题
    private final AtomicBoolean isUpdating = new AtomicBoolean(false);
    
    // 定时刷新任务
    private BukkitTask refreshTask;

    public TerminalGUI(Player player, Net_storage plugin, TerminalData terminalData, Location blockLocation) {
        // super() 必须是第一条语句，在参数中计算标题
        super(player, 54, buildTitle(plugin, terminalData.networkId));
        
        this.plugin = plugin;
        this.terminalData = terminalData;
        this.diskManager = plugin.getDiskManager();
        this.networkId = terminalData.networkId;
        this.blockLocation = blockLocation;
        
        // 加载网络物品
        refreshNetworkData();
        
        // 计算总页数
        totalPages = Math.max(1, (int) Math.ceil(displayItems.size() / (double) ITEMS_PER_PAGE));
        
        initialize();
        
        // 启动定时刷新任务（每1.5秒 = 30 ticks）
        startRefreshTask();
    }
    
    /**
     * 启动定时刷新任务
     */
    private void startRefreshTask() {
        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // 只在玩家打开界面时刷新
            if (player.isOnline() && player.getOpenInventory().getTitle().equals(getTitle())) {
                refreshNetworkData();
                update();
            } else {
                // 玩家已关闭界面，停止刷新任务
                stopRefreshTask();
            }
        }, 30L, 30L); // 30 ticks = 1.5秒
    }
    
    /**
     * 停止定时刷新任务
     */
    private void stopRefreshTask() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }
    
    /**
     * 构建终端标题
     */
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

    /**
     * 刷新网络数据 - 从 DiskManager 获取最新数据
     */
    public void refreshNetworkData() {
        // 标记开始更新
        isUpdating.set(true);
        
        try {
            networkItemsMap.clear();
            displayItems.clear();
            networkDisks.clear();
            
            if (networkId == null) {
                return;
            }
            
            // 获取所有连接到该网络的磁盘操纵器
            List<DiskManipulatorData> manipulators = plugin.getControllerManager().getDiskManipulatorsByNetwork(networkId);
            
            for (DiskManipulatorData manipulator : manipulators) {
                if (manipulator.slots == null) continue;
                
                for (UUID diskUuid : manipulator.slots) {
                    if (diskUuid == null) continue;
                    
                    // 记录磁盘 UUID
                    networkDisks.add(diskUuid);
                    
                    // 从 DiskManager 获取磁盘数据（使用缓存）
                    List<DiskItem> diskItems = diskManager.getDiskData(diskUuid);
                    
                    // 添加物品到网络列表
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
                    
                    // 安全检查：确保是容器
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
                        
                        // 创建 DiskItem 表示容器物品
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
            
            // 更新显示列表
            displayItems = new ArrayList<>(networkItemsMap.values());
            
        } finally {
            // 标记更新完成
            isUpdating.set(false);
        }
    }

    @Override
    public void initialize() {
        setupInfoSlot();
        setupTopRowFiller();
        setupItems();
        setupPagination();
        setupBottomRowFiller();
    }

    /**
     * 设置指南针信息槽位
     */
    private void setupInfoSlot() {
        // 获取网络信息
        String networkName = "未连接";
        String networkIdShort = "无";
        
        if (networkId != null) {
            StorageNetwork network = plugin.getNetworkManager().getNetwork(networkId);
            if (network != null) {
                networkName = network.getName();
                networkIdShort = networkId.toString().substring(0, 8) + "...";
            }
        }
        
        // 计算磁盘操纵器数量和磁盘数量
        int manipulatorCount = plugin.getControllerManager().getDiskManipulatorsByNetwork(networkId).size();
        int diskCount = networkDisks.size();
        
        // 计算外部存储总线数量
        int externalBusCount = plugin.getControllerManager().getExternalStorageBusesByNetwork(networkId).size();
        
        // 计算磁盘容量（根据磁盘类型）
        int diskCapacity = 0;
        for (UUID diskUuid : networkDisks) {
            diskCapacity += diskManager.getDiskCapacity(diskUuid);
        }
        int diskUsedSpace = calculateUsedSpace();
        int diskRemaining = diskCapacity - diskUsedSpace;
        
        // 计算外部容器容量
        int containerRemaining = calculateExternalContainerRemainingSpace();
        
        // 总容量和总剩余
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
                        ChatColor.YELLOW + "点击物品取出 | 点击背包物品存入"
                )
                .build();
        
        setItem(INFO_SLOT, compass);
    }

    /**
     * 计算已用空间 - 从所有磁盘累加
     */
    private int calculateUsedSpace() {
        int total = 0;
        for (UUID diskUuid : networkDisks) {
            List<DiskItem> items = diskManager.getDiskData(diskUuid);
            total += diskManager.getTotalItems(items);
        }
        return total;
    }

    /**
     * 创建进度条
     */
    private String createProgressBar(int percentage) {
        int filled = (percentage * 20) / 100;
        int remain = 20 - filled;
        
        ChatColor fillColor;
        if (percentage < 80) {
            fillColor = ChatColor.BLUE;
        } else if (percentage < 95) {
            fillColor = ChatColor.YELLOW;
        } else {
            fillColor = ChatColor.RED;
        }
        
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < filled; i++) {
            bar.append(fillColor).append("|");
        }
        for (int i = 0; i < remain; i++) {
            bar.append(ChatColor.GRAY).append("|");
        }
        
        return bar.toString();
    }

    /**
     * 设置第一排填充物
     */
    private void setupTopRowFiller() {
        ItemStack glassPane = new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                .setName(" ")
                .build();
        
        for (int i = 1; i <= 8; i++) {
            setItem(i, glassPane);
        }
    }

    /**
     * 设置物品列表
     */
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
            
            ItemStack displayItem = new ItemBuilder(diskItem.getMaterial())
                    .setName(displayName)
                    .setLore(
                            ChatColor.GRAY + "数量: " + ChatColor.WHITE + diskItem.getAmount() + " 个",
                            "",
                            ChatColor.YELLOW + "左键/右键点击取出 1 个",
                            ChatColor.YELLOW + "Shift+左键/右键取出全部"
                    )
                    .build();
            
            setItem(slot, displayItem);
            
            // 使用唯一键来标识物品，而不是索引
            String itemKey;
            if (diskItem.isExternalItem()) {
                // 外部容器物品使用 busUuid 作为键
                itemKey = "external#" + diskItem.getExternalBus() + "#" + diskItem.getSlotIndex();
            } else {
                // 磁盘物品使用 diskUuid 作为键
                itemKey = diskItem.getDiskUuid().toString() + "#" + diskItem.getSerializedItem();
            }
            
            final String key = itemKey;
            setClickAction(slot, (p, item, slotNum, clickType) -> {
                handleItemClick(key, clickType);
            });
            
            slot++;
        }
        
        // 填充空位
        ItemStack emptyPane = new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                .setName(" ")
                .build();
        
        while (slot <= ITEM_AREA_END) {
            setItem(slot, emptyPane);
            slot++;
        }
    }

    /**
     * 处理物品点击 - 取出物品
     */
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
        
        // 检查是否是外部容器物品
        if (diskItem.isExternalItem()) {
            handleExternalItemClick(diskItem, clickType);
            return;
        }
        
        UUID diskUuid = diskItem.getDiskUuid();
        if (diskUuid == null) {
            player.sendMessage(ChatColor.RED + "物品数据异常！");
            return;
        }
        
        // 计算取出数量
        int removeAmount = (clickType == ClickType.SHIFT_LEFT_CLICK || clickType == ClickType.SHIFT_RIGHT_CLICK) 
                ? diskItem.getAmount() : 1;
        
        // 检查背包空间
        Material material = diskItem.getMaterial();
        int maxStackSize = material.getMaxStackSize();
        int playerSpace = calculatePlayerSpace(player, material);
        
        int canTake = Math.min(removeAmount, playerSpace);
        
        if (canTake <= 0) {
            player.sendMessage(ChatColor.RED + "背包已满！");
            return;
        }
        
        // 获取磁盘数据并移除物品
        List<DiskItem> diskData = diskManager.getDiskData(diskUuid);
        
        boolean found = false;
        int remainingInDisk = 0;
        DiskItem targetItem = null;
        for (DiskItem di : diskData) {
            if (di.getSerializedItem().equals(diskItem.getSerializedItem())) {
                remainingInDisk = di.getAmount();
                targetItem = di;
                found = true;
                break;
            }
        }
        
        if (!found || targetItem == null) {
            player.sendMessage(ChatColor.RED + "物品数据异常，请重新打开终端！");
            return;
        }
        
        // 更新物品数量
        int newAmount = remainingInDisk - canTake;
        targetItem.setAmount(newAmount);
        
        // 如果数量为0或负数，移除该物品条目
        if (newAmount <= 0) {
            diskData.remove(targetItem);
        }
        
        // 保存磁盘数据
        diskManager.saveDiskData(diskUuid, diskData);
        
        // 使用 DiskItem 的完整数据创建物品（包括 NBT）
        ItemStack giveItem = diskItem.toItemStack();
        giveItem.setAmount(canTake);
        player.getInventory().addItem(giveItem);
        
        String itemName = diskItem.getDisplayName() != null ? diskItem.getDisplayName() : material.name();
        String msg = canTake < removeAmount 
                ? ChatColor.YELLOW + "背包空间不足！已取出 " + ChatColor.WHITE + canTake + ChatColor.YELLOW + " 个 " + itemName
                : ChatColor.GREEN + "已取出 " + ChatColor.WHITE + canTake + ChatColor.GREEN + " 个 " + itemName;
        player.sendMessage(msg);
        
        // 刷新界面
        refreshNetworkData();
        totalPages = Math.max(1, (int) Math.ceil(displayItems.size() / (double) ITEMS_PER_PAGE));
        if (currentPage > totalPages) {
            currentPage = totalPages;
        }
        update();
    }
    
    /**
     * 处理外部容器物品取出
     */
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
            refreshNetworkData();
            update();
            return;
        }
        
        // 计算取出数量
        int removeAmount = (clickType == ClickType.SHIFT_LEFT_CLICK || clickType == ClickType.SHIFT_RIGHT_CLICK) 
                ? containerItem.getAmount() : 1;
        
        // 检查背包空间
        Material material = containerItem.getType();
        int playerSpace = calculatePlayerSpace(player, material);
        
        int canTake = Math.min(removeAmount, playerSpace);
        
        if (canTake <= 0) {
            player.sendMessage(ChatColor.RED + "背包已满！");
            return;
        }
        
        // 从容器中取出物品
        ItemStack giveItem = containerItem.clone();
        giveItem.setAmount(canTake);
        
        containerItem.setAmount(containerItem.getAmount() - canTake);
        if (containerItem.getAmount() <= 0) {
            inventory.setItem(slotIndex, null);
        }
        
        // 给玩家物品
        player.getInventory().addItem(giveItem);
        
        String itemName = containerItem.hasItemMeta() && containerItem.getItemMeta().hasDisplayName() 
                ? containerItem.getItemMeta().getDisplayName() : material.name();
        player.sendMessage(ChatColor.GREEN + "已从容器取出 " + ChatColor.WHITE + canTake + ChatColor.GREEN + " 个 " + itemName);
        
        // 刷新界面
        refreshNetworkData();
        totalPages = Math.max(1, (int) Math.ceil(displayItems.size() / (double) ITEMS_PER_PAGE));
        if (currentPage > totalPages) {
            currentPage = totalPages;
        }
        update();
    }

    /**
     * 计算玩家背包中特定物品的剩余空间
     */
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

    /**
     * 设置翻页控制
     */
    private void setupPagination() {
        ItemStack prevPageItem;
        if (currentPage > 1) {
            prevPageItem = new ItemBuilder(Material.ARROW)
                    .setName(ChatColor.YELLOW + "上一页")
                    .setLore(ChatColor.GRAY + "当前页: " + currentPage + "/" + totalPages)
                    .build();
            
            setClickAction(PREV_PAGE_SLOT, (p, item, slot, clickType) -> {
                if (currentPage > 1) {
                    currentPage--;
                    update();
                }
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
                if (currentPage < totalPages) {
                    currentPage++;
                    update();
                }
            });
        } else {
            nextPageItem = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                    .setName(ChatColor.DARK_GRAY + "已是最后一页")
                    .build();
        }
        setItem(NEXT_PAGE_SLOT, nextPageItem);
    }

    /**
     * 设置底部填充
     */
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

    /**
     * 获取网络总剩余空间 - 从所有磁盘累加
     */
    public int getTotalRemainingSpace() {
        // 磁盘容量（根据磁盘类型）
        int diskCapacity = 0;
        for (UUID diskUuid : networkDisks) {
            diskCapacity += diskManager.getDiskCapacity(diskUuid);
        }
        int diskUsedSpace = calculateUsedSpace();
        int diskRemaining = diskCapacity - diskUsedSpace;
        
        // 外部容器容量
        int containerRemaining = calculateExternalContainerRemainingSpace();
        
        return diskRemaining + containerRemaining;
    }
    
    /**
     * 计算外部容器剩余空间
     */
    private int calculateExternalContainerRemainingSpace() {
        if (networkId == null) {
            return 0;
        }
        
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
                    // 空槽位，按最大堆叠数计算
                    remaining += 64; // 默认最大堆叠数
                } else if (slotItem.getAmount() < slotItem.getMaxStackSize()) {
                    // 有物品但未满，计算剩余空间
                    remaining += slotItem.getMaxStackSize() - slotItem.getAmount();
                }
            }
        }
        
        return remaining;
    }

    /**
     * 检查物品是否能存入
     */
    public boolean canStoreItem(ItemStack item, int amount) {
        return getTotalRemainingSpace() >= amount;
    }

    /**
     * 处理玩家背包点击 - 存入物品
     * @param item 玩家手中的物品
     * @param amount 要存入的数量
     * @return 实际存入的数量
     */
    public int storeItem(ItemStack item, int amount) {
        if (item == null || item.getType() == Material.AIR) {
            return 0;
        }
        
        // 检查空间
        int remaining = getTotalRemainingSpace();
        if (remaining <= 0) {
            return 0;
        }
        
        // 限制存入数量
        int toStore = Math.min(amount, remaining);
        
        // 智能分布存入
        return distributeItemToDisks(item, toStore);
    }

    /**
     * 分布存储物品到磁盘 - 直接修改 DiskManager 缓存
     * 优先堆叠相同物品，然后分散存储
     */
    private int distributeItemToDisks(ItemStack item, int amount) {
        if (amount <= 0) {
            return 0;
        }
        
        int stored = 0;
        
        // 用于记录哪些磁盘被修改了
        Map<UUID, List<DiskItem>> modifiedDisks = new HashMap<>();
        
        // 第一步：优先堆叠到已有相同物品的磁盘
        for (UUID diskUuid : networkDisks) {
            if (amount - stored <= 0) break;
            
            List<DiskItem> diskItems = diskManager.getDiskData(diskUuid);
            int usedSpace = diskManager.getTotalItems(diskItems);
            int remainingSpace = diskManager.getDiskCapacity(diskUuid) - usedSpace;
            
            if (remainingSpace <= 0) continue;
            
            // 查找该磁盘是否有相同物品
            for (DiskItem diskItem : diskItems) {
                if (diskItem.matchesItemStack(item)) {
                    int canAdd = Math.min(remainingSpace, amount - stored);
                    if (canAdd > 0) {
                        diskItem.addAmount(canAdd);
                        stored += canAdd;
                        modifiedDisks.put(diskUuid, diskItems);
                    }
                    break; // 每个磁盘只堆叠一次
                }
            }
        }
        
        // 第二步：分散存储剩余物品到有空位的磁盘
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
        
        // 第三步：保存所有修改过的磁盘
        for (Map.Entry<UUID, List<DiskItem>> entry : modifiedDisks.entrySet()) {
            diskManager.saveDiskData(entry.getKey(), entry.getValue());
        }
        
        // 第四步：如果还有剩余物品，存入外部容器
        if (amount - stored > 0) {
            stored += distributeItemToExternalContainers(item, amount - stored);
        }
        
        return stored;
    }
    
    /**
     * 将物品存入外部容器
     */
    private int distributeItemToExternalContainers(ItemStack item, int amount) {
        if (amount <= 0 || networkId == null) {
            return 0;
        }
        
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
            
            // 第一步：优先堆叠到已有相同物品的槽位
            for (int i = 0; i < inventory.getSize(); i++) {
                if (amount - stored <= 0) break;
                
                ItemStack slotItem = inventory.getItem(i);
                if (slotItem != null && slotItem.isSimilar(item) && slotItem.getAmount() < slotItem.getMaxStackSize()) {
                    int canAdd = Math.min(slotItem.getMaxStackSize() - slotItem.getAmount(), amount - stored);
                    slotItem.setAmount(slotItem.getAmount() + canAdd);
                    stored += canAdd;
                }
            }
            
            // 第二步：存入空槽位
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

    /**
     * 处理玩家背包区域的点击事件
     */
    @Override
    protected void handlePlayerInventoryClick(InventoryClickEvent event, int slot, ItemStack item, ClickType clickType) {
        // 检查是否正在更新
        if (isUpdating.get()) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "界面正在刷新，请稍后...");
            return;
        }
        
        // 只处理玩家背包区域
        if (slot < inventory.getSize()) {
            return;
        }
        
        // 检查是否是空物品
        if (item == null || item.getType() == Material.AIR) {
            event.setCancelled(true);
            return;
        }
        
        // 计算存入数量
        int amount = (clickType == ClickType.SHIFT_LEFT_CLICK || clickType == ClickType.SHIFT_RIGHT_CLICK) 
                ? item.getAmount() : 1;
        
        // 检查空间
        int remaining = getTotalRemainingSpace();
        if (remaining <= 0) {
            player.sendMessage(ChatColor.RED + "网络存储空间不足！剩余: 0");
            event.setCancelled(true);
            return;
        }
        
        if (amount > remaining) {
            player.sendMessage(ChatColor.YELLOW + "空间不足！网络剩余: " + remaining + "，尝试存入 " + remaining + " 个");
            amount = remaining;
        }
        
        // 标记开始存入
        isUpdating.set(true);
        
        try {
            // 执行存入
            int stored = storeItem(item.clone(), amount);
            
            if (stored > 0) {
                // 从玩家背包移除物品
                int remainingInHand = item.getAmount() - stored;
                if (remainingInHand <= 0) {
                    event.setCurrentItem(null);
                } else {
                    item.setAmount(remainingInHand);
                    event.setCurrentItem(item);
                }
                
                String itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName() 
                        ? item.getItemMeta().getDisplayName() 
                        : item.getType().name();
                
                if (stored < amount) {
                    player.sendMessage(ChatColor.YELLOW + "空间不足！已存入 " + ChatColor.WHITE + stored + 
                            ChatColor.YELLOW + " 个 " + itemName);
                } else {
                    player.sendMessage(ChatColor.GREEN + "已存入 " + ChatColor.WHITE + stored + 
                            ChatColor.GREEN + " 个 " + itemName);
                }
                
                // 刷新界面
                refreshNetworkData();
                totalPages = Math.max(1, (int) Math.ceil(displayItems.size() / (double) ITEMS_PER_PAGE));
                if (currentPage > totalPages) {
                    currentPage = totalPages;
                }
                update();
            } else {
                player.sendMessage(ChatColor.RED + "存入失败！");
            }
        } finally {
            // 标记存入完成
            isUpdating.set(false);
        }
        
        event.setCancelled(true);
    }

    /**
     * 更新界面
     */
    public void update() {
        if (isUpdating.get()) {
            return;
        }
        
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
    
    /**
     * 关闭界面
     */
    @Override
    public void close() {
        stopRefreshTask();
        super.close();
    }
}
