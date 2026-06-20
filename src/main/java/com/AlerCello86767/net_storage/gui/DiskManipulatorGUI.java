package com.AlerCello86767.net_storage.gui;

import com.AlerCello86767.net_storage.Net_storage;
import com.AlerCello86767.net_storage.controller.DiskManipulatorData;
import com.AlerCello86767.net_storage.disk.DiskItem;
import com.AlerCello86767.net_storage.disk.DiskManager;
import com.AlerCello86767.net_storage.network.StorageNetwork;
import com.AlerCello86767.net_storage.utils.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
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
    
    // 槽位定义
    private static final int INFO_SLOT = 0;        // 指南针
    private static final int DISK_SLOT_START = 1; // 磁盘槽开始
    private static final int DISK_SLOT_END = 8;   // 磁盘槽结束
    
    // 磁盘槽索引映射：槽位1-8对应索引0-7
    private static final int SLOT_TO_INDEX_OFFSET = 1;

    public DiskManipulatorGUI(Player player, Net_storage plugin, 
                             DiskManipulatorData manipulatorData, Location blockLocation) {
        super(player, 9, ChatColor.translateAlternateColorCodes('&', "&b磁盘操纵器"));
        this.plugin = plugin;
        this.manipulatorData = manipulatorData;
        this.diskManager = plugin.getDiskManager();
        this.blockLocation = blockLocation;
        
        initialize();
    }

    @Override
    public void initialize() {
        setupInfoSlot();
        setupDiskSlots();
    }

    /**
     * 设置指南针信息槽位
     */
    private void setupInfoSlot() {
        // 获取网络信息
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
        
        // 计算磁盘容量信息（根据磁盘类型）
        int insertedDisks = manipulatorData.getInsertedDiskCount();
        int totalCapacity = 0;
        for (UUID diskUuid : manipulatorData.slots) {
            if (diskUuid != null) {
                totalCapacity += diskManager.getDiskCapacity(diskUuid);
            }
        }
        
        // 计算已用空间
        int usedSpace = 0;
        for (UUID diskUuid : manipulatorData.slots) {
            if (diskUuid != null) {
                List<DiskItem> items = diskManager.getDiskData(diskUuid);
                usedSpace += diskManager.getTotalItems(items);
            }
        }
        
        // 计算使用率
        int percentage = totalCapacity > 0 ? (usedSpace * 100) / totalCapacity : 0;
        
        // 构建进度条
        String progressBar = createProgressBar(percentage);
        
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
                        progressBar,
                        "",
                        ChatColor.GRAY + "状态: " + status
                )
                .build();
        
        setItem(INFO_SLOT, compass);
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
     * 设置磁盘槽位
     */
    private void setupDiskSlots() {
        for (int slotIndex = DISK_SLOT_START; slotIndex <= DISK_SLOT_END; slotIndex++) {
            int diskIndex = slotIndex - SLOT_TO_INDEX_OFFSET;
            UUID diskUuid = manipulatorData.slots[diskIndex];
            
            if (diskUuid != null) {
                // 显示磁盘物品
                ItemStack diskItem = diskManager.getDiskItemFromUUID(diskUuid);
                if (diskItem != null) {
                    // 添加磁盘显示信息
                    List<DiskItem> items = diskManager.getDiskData(diskUuid);
                    int itemCount = diskManager.getTotalItems(items);
                    int maxCap = diskManager.getDiskCapacity(diskUuid);
                    int usagePercent = (itemCount * 100) / maxCap;
                    
                    ItemStack displayItem = new ItemBuilder(diskItem.clone())
                            .setName(ChatColor.WHITE + "磁盘 #" + (diskIndex + 1))
                            .addLore(ChatColor.GRAY + "容量: " + ChatColor.WHITE + itemCount + "/" + maxCap)
                            .addLore(ChatColor.GRAY + "使用率: " + ChatColor.WHITE + usagePercent + "%")
                            .addLore("")
                            .addLore(ChatColor.YELLOW + "点击取出磁盘")
                            .build();
                    
                    setItem(slotIndex, displayItem);
                } else {
                    // 磁盘数据不存在，显示空槽
                    setItem(slotIndex, createEmptySlot(slotIndex));
                }
            } else {
                // 空槽位
                setItem(slotIndex, createEmptySlot(slotIndex));
            }
            
            // 设置点击动作
            setClickAction(slotIndex, (p, item, slot, clickType) -> {
                handleDiskSlotClick(slot);
            });
        }
    }

    /**
     * 创建空槽位显示
     */
    private ItemStack createEmptySlot(int slotIndex) {
        int diskIndex = slotIndex - SLOT_TO_INDEX_OFFSET + 1;
        return new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                .setName(ChatColor.GRAY + "空磁盘槽 #" + diskIndex)
                .setLore(
                        ChatColor.DARK_GRAY + "点击放入磁盘",
                        ChatColor.DARK_GRAY + "只接受磁盘物品"
                )
                .build();
    }

    /**
     * 处理磁盘槽点击
     */
    private void handleDiskSlotClick(int slotIndex) {
        int diskIndex = slotIndex - SLOT_TO_INDEX_OFFSET;
        UUID diskUuid = manipulatorData.slots[diskIndex];
        
        if (diskUuid != null) {
            // 槽位有磁盘，取出磁盘
            removeDisk(diskIndex);
        } else {
            // 槽位为空，放入磁盘
            insertDisk(slotIndex);
        }
    }

    /**
     * 从槽位取出磁盘
     */
    private void removeDisk(int diskIndex) {
        UUID diskUuid = manipulatorData.removeDisk(diskIndex);
        if (diskUuid != null) {
            // 保存数据
            plugin.getDatabaseManager().saveDiskManipulatorToDB(manipulatorData);
            
            // 给予磁盘物品
            ItemStack diskItem = diskManager.getDiskItemFromUUID(diskUuid);
            if (diskItem != null) {
                player.getWorld().dropItemNaturally(player.getLocation(), diskItem);
                player.sendMessage(ChatColor.GREEN + "已取出磁盘！");
            }
            
            // 刷新界面
            update();
        }
    }

    /**
     * 放入磁盘到槽位
     */
    private void insertDisk(int slotIndex) {
        // 获取玩家手中的物品
        ItemStack handItem = player.getInventory().getItemInMainHand();
        
        if (!diskManager.isDisk(handItem)) {
            player.sendMessage(ChatColor.RED + "只能放入磁盘物品！");
            return;
        }
        
        // 获取磁盘UUID
        UUID diskUuid = diskManager.getDiskUuidFromItem(handItem);
        if (diskUuid == null) {
            player.sendMessage(ChatColor.RED + "磁盘数据无效！");
            return;
        }
        
        // 插入磁盘
        int diskIndex = slotIndex - SLOT_TO_INDEX_OFFSET;
        manipulatorData.slots[diskIndex] = diskUuid;
        
        // 保存数据
        plugin.getDatabaseManager().saveDiskManipulatorToDB(manipulatorData);
        
        // 移除玩家手中的磁盘 - 正确更新背包
        int newAmount = handItem.getAmount() - 1;
        if (newAmount <= 0) {
            player.getInventory().setItemInMainHand(null);
        } else {
            ItemStack newItem = handItem.clone();
            newItem.setAmount(newAmount);
            player.getInventory().setItemInMainHand(newItem);
        }
        
        player.sendMessage(ChatColor.GREEN + "已插入磁盘！");
        
        // 刷新界面
        update();
    }
    
    /**
     * 处理玩家背包点击事件 - 允许放入磁盘
     */
    @Override
    protected void handlePlayerInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event, int slot, ItemStack item, com.AlerCello86767.net_storage.gui.ClickType clickType) {
        // 检查点击的物品是否是磁盘
        if (item != null && diskManager.isDisk(item)) {
            // 查找第一个空槽位
            int emptySlot = manipulatorData.getFirstEmptySlot();
            if (emptySlot >= 0) {
                // 获取磁盘UUID
                UUID diskUuid = diskManager.getDiskUuidFromItem(item);
                if (diskUuid != null) {
                    // 放入磁盘
                    manipulatorData.slots[emptySlot] = diskUuid;
                    // 保存数据
                    plugin.getDatabaseManager().saveDiskManipulatorToDB(manipulatorData);
                    // 移除背包中的物品
                    int newAmount = item.getAmount() - 1;
                    if (newAmount <= 0) {
                        event.setCurrentItem(null);
                    } else {
                        ItemStack newItem = item.clone();
                        newItem.setAmount(newAmount);
                        event.setCurrentItem(newItem);
                    }
                    // 刷新界面
                    update();
                    player.sendMessage(ChatColor.GREEN + "已插入磁盘！");
                }
            } else {
                player.sendMessage(ChatColor.RED + "磁盘操纵器已满！");
            }
        }
        // 始终取消事件，防止物品被移动
        event.setCancelled(true);
    }

    /**
     * 更新界面
     */
    public void update() {
        inventory.clear();
        clickActions.clear();
        initialize();
        player.updateInventory();
    }
}
