package com.AlerCello86767.net_storage.gui;

import com.AlerCello86767.net_storage.Net_storage;
import com.AlerCello86767.net_storage.controller.ControllerManager;
import com.AlerCello86767.net_storage.controller.DiskManipulatorData;
import com.AlerCello86767.net_storage.controller.ExternalStorageBusData;
import com.AlerCello86767.net_storage.controller.InputBusData;
import com.AlerCello86767.net_storage.controller.OutputBusData;
import com.AlerCello86767.net_storage.controller.TerminalData;
import com.AlerCello86767.net_storage.network.StorageNetwork;
import com.AlerCello86767.net_storage.utils.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 控制器GUI - 显示网络信息和设备列表
 */
public class ControllerGUI extends BaseGUI {

    private final Net_storage plugin;
    private final StorageNetwork network;
    private int currentPage = 1;
    private int totalPages = 1;
    
    /**
     * 设备类型枚举
     */
    private enum DeviceType {
        DEBUG_DEVICE,
        DISK_MANIPULATOR,
        TERMINAL,
        EXTERNAL_STORAGE_BUS,
        INPUT_BUS,
        OUTPUT_BUS
    }
    
    /**
     * 设备信息
     */
    private static class DeviceInfo {
        String location;
        DeviceType type;
        Object data;
        
        DeviceInfo(String location, DeviceType type, Object data) {
            this.location = location;
            this.type = type;
            this.data = data;
        }
    }
    
    private List<DeviceInfo> devices = new ArrayList<>();

    // 设备显示区域：槽位9-44（共36格）
    private static final int DEVICE_AREA_START = 9;
    private static final int DEVICE_AREA_END = 44;
    private static final int DEVICES_PER_PAGE = 36;

    // 特殊槽位
    private static final int INFO_SLOT = 0;           // 指南针（网络介绍）
    private static final int PREV_PAGE_SLOT = 45;     // 上一页
    private static final int PAGE_INFO_SLOT = 49;     // 钟（页数显示）
    private static final int NEXT_PAGE_SLOT = 53;     // 下一页

    public ControllerGUI(Player player, Net_storage plugin, StorageNetwork network) {
        super(player, 54, ChatColor.DARK_AQUA + "网络控制器 - " + network.getName());
        this.plugin = plugin;
        this.network = network;
    }

    @Override
    public void initialize() {
        inventory.clear();
        clearClickActions();

        // 第一排：指南针 + 玻璃板
        setupFirstRow();

        // 第2-5排：设备显示区域
        setupDeviceArea();

        // 第六排：翻页控制
        setupLastRow();
    }

    /**
     * 设置第一排：指南针 + 玻璃板
     */
    private void setupFirstRow() {
        // 指南针 - 网络介绍
        ItemStack infoItem = createNetworkInfoItem();
        setItem(INFO_SLOT, infoItem);

        // 淡灰色玻璃板填充剩余槽位
        ItemStack glassPane = new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                .setName(" ")
                .build();

        for (int i = 1; i <= 8; i++) {
            setItem(i, glassPane);
        }
    }

    /**
     * 创建网络介绍物品（指南针）
     */
    private ItemStack createNetworkInfoItem() {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "网络ID: " + ChatColor.WHITE + network.getNetworkId().toString().substring(0, 8) + "...");
        lore.add(ChatColor.GRAY + "创建者: " + ChatColor.WHITE + network.getCreatorName());
        lore.add(ChatColor.GRAY + "状态: " + (network.isPublic() ? ChatColor.GREEN + "公开" : ChatColor.RED + "私有"));
        
        if (network.getDescription() != null && !network.getDescription().isEmpty()) {
            lore.add(ChatColor.GRAY + "描述: " + ChatColor.WHITE + network.getDescription());
        }
        
        lore.add("");
        
        // 获取连接到该网络的磁盘操纵器数量和磁盘数量
        List<DiskManipulatorData> manipulators = plugin.getControllerManager().getDiskManipulatorsByNetwork(network.getNetworkId());
        int manipulatorCount = manipulators.size();
        int diskCount = 0;
        for (DiskManipulatorData manipulator : manipulators) {
            if (manipulator.slots != null) {
                for (UUID diskUuid : manipulator.slots) {
                    if (diskUuid != null) {
                        diskCount++;
                    }
                }
            }
        }
        
        lore.add(ChatColor.GRAY + "磁盘操纵器: " + ChatColor.YELLOW + manipulatorCount + " 个");
        lore.add(ChatColor.GRAY + "已插入磁盘: " + ChatColor.YELLOW + diskCount + " 个");
        lore.add(ChatColor.GRAY + "节点数量: " + ChatColor.YELLOW + network.getNodeCount());
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        lore.add(ChatColor.GRAY + "创建时间: " + ChatColor.WHITE + sdf.format(new Date(network.getCreatedTime())));
        lore.add(ChatColor.GRAY + "最后修改: " + ChatColor.WHITE + sdf.format(new Date(network.getLastModifiedTime())));

        return new ItemBuilder(Material.COMPASS)
                .setName(ChatColor.GOLD + "网络信息")
                .setLore(lore.toArray(new String[0]))
                .build();
    }

    /**
     * 设置设备显示区域（第2-5排）
     */
    private void setupDeviceArea() {
        // 收集所有连接到该网络的设备
        devices.clear();
        
        // 1. 调试子设备
        Map<String, UUID> debugDevices = plugin.getControllerManager().getAllDebugDevices();
        for (Map.Entry<String, UUID> entry : debugDevices.entrySet()) {
            if (entry.getValue() != null && entry.getValue().equals(network.getNetworkId())) {
                devices.add(new DeviceInfo(entry.getKey(), DeviceType.DEBUG_DEVICE, null));
            }
        }
        
        // 2. 磁盘操纵器
        List<DiskManipulatorData> manipulators = plugin.getControllerManager().getDiskManipulatorsByNetwork(network.getNetworkId());
        for (DiskManipulatorData manipulator : manipulators) {
            devices.add(new DeviceInfo(manipulator.location, DeviceType.DISK_MANIPULATOR, manipulator));
        }
        
        // 3. 终端
        List<TerminalData> terminals = plugin.getControllerManager().getTerminalsByNetwork(network.getNetworkId());
        for (TerminalData terminal : terminals) {
            devices.add(new DeviceInfo(terminal.location, DeviceType.TERMINAL, terminal));
        }
        
        // 4. 外部存储总线
        List<ExternalStorageBusData> externalBuses = plugin.getControllerManager().getExternalStorageBusesByNetwork(network.getNetworkId());
        for (ExternalStorageBusData bus : externalBuses) {
            devices.add(new DeviceInfo(bus.location, DeviceType.EXTERNAL_STORAGE_BUS, bus));
        }
        
        // 5. 输入总线
        List<InputBusData> inputBuses = plugin.getControllerManager().getInputBusesByNetwork(network.getNetworkId());
        for (InputBusData bus : inputBuses) {
            devices.add(new DeviceInfo(bus.location, DeviceType.INPUT_BUS, bus));
        }
        
        // 6. 输出总线
        List<OutputBusData> outputBuses = plugin.getControllerManager().getOutputBusesByNetwork(network.getNetworkId());
        for (OutputBusData bus : outputBuses) {
            devices.add(new DeviceInfo(bus.location, DeviceType.OUTPUT_BUS, bus));
        }

        // 计算总页数
        totalPages = Math.max(1, (int) Math.ceil(devices.size() / (double) DEVICES_PER_PAGE));

        if (devices.isEmpty()) {
            // 暂无设备
            ItemStack placeholder = new ItemBuilder(Material.BARRIER)
                    .setName(ChatColor.GRAY + "暂无设备")
                    .setLore(ChatColor.DARK_GRAY + "放置设备并连接到网络")
                    .build();

            for (int i = DEVICE_AREA_START; i <= DEVICE_AREA_END; i++) {
                setItem(i, placeholder);
            }
            return;
        }

        // 显示设备列表
        int startIndex = (currentPage - 1) * DEVICES_PER_PAGE;
        int endIndex = Math.min(startIndex + DEVICES_PER_PAGE, devices.size());

        int slot = DEVICE_AREA_START;
        for (int i = startIndex; i < endIndex && slot <= DEVICE_AREA_END; i++) {
            DeviceInfo device = devices.get(i);
            Location location = plugin.getControllerManager().stringToLocation(device.location);

            if (location != null) {
                ItemStack deviceItem = createDeviceItem(device, location);
                setItem(slot, deviceItem);

                // 点击事件：显示详情
                final DeviceInfo finalDevice = device;
                final Location finalLocation = location;
                setClickAction(slot, (p, item, s, clickType) -> {
                    showDeviceDetail(p, finalDevice, finalLocation);
                });
            }
            slot++;
        }

        // 填充剩余空位
        ItemStack emptyPane = new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                .setName(" ")
                .build();
        while (slot <= DEVICE_AREA_END) {
            setItem(slot, emptyPane);
            slot++;
        }
    }
    
    /**
     * 根据设备类型创建显示物品
     */
    private ItemStack createDeviceItem(DeviceInfo device, Location location) {
        Material material;
        String name;
        List<String> lore = new ArrayList<>();
        
        switch (device.type) {
            case DEBUG_DEVICE:
                material = Material.GREEN_CONCRETE;
                name = ChatColor.GREEN + "调试子设备";
                break;
            case DISK_MANIPULATOR:
                material = Material.ORANGE_CONCRETE;
                name = ChatColor.GOLD + "磁盘操纵器";
                // 显示磁盘数量
                DiskManipulatorData manipulator = (DiskManipulatorData) device.data;
                int diskCount = 0;
                if (manipulator.slots != null) {
                    for (UUID diskUuid : manipulator.slots) {
                        if (diskUuid != null) diskCount++;
                    }
                }
                lore.add(ChatColor.GRAY + "磁盘数量: " + ChatColor.WHITE + diskCount + "/8");
                break;
            case TERMINAL:
                material = Material.CYAN_CONCRETE;
                name = ChatColor.AQUA + "终端";
                break;
            case EXTERNAL_STORAGE_BUS:
                material = Material.END_ROD;
                name = ChatColor.LIGHT_PURPLE + "外部存储总线";
                // 显示容器类型
                ExternalStorageBusData bus = (ExternalStorageBusData) device.data;
                lore.add(ChatColor.GRAY + "绑定容器: " + ChatColor.WHITE + bus.getContainerDisplayName());
                break;
            case INPUT_BUS:
                material = Material.PLAYER_HEAD;
                name = ChatColor.LIGHT_PURPLE + "输入总线";
                // 显示容器类型
                InputBusData inputBus = (InputBusData) device.data;
                lore.add(ChatColor.GRAY + "绑定容器: " + ChatColor.WHITE + inputBus.getContainerDisplayName());
                lore.add(ChatColor.GRAY + "功能: " + ChatColor.GREEN + "自动提取物品");
                break;
            default:
                material = Material.STONE;
                name = ChatColor.GRAY + "未知设备";
        }
        
        // 通用信息
        lore.add(0, ChatColor.GRAY + "位置: " + ChatColor.WHITE + 
                location.getWorld().getName() + " " +
                location.getBlockX() + "," + 
                location.getBlockY() + "," + 
                location.getBlockZ());
        lore.add(ChatColor.GRAY + "状态: " + ChatColor.GREEN + "已连接");
        lore.add("");
        lore.add(ChatColor.YELLOW + "点击查看详情");
        
        return new ItemBuilder(material)
                .setName(name)
                .setLore(lore.toArray(new String[0]))
                .build();
    }

    /**
     * 显示设备详情
     */
    private void showDeviceDetail(Player player, DeviceInfo device, Location location) {
        player.closeInventory();

        String deviceTypeName;
        switch (device.type) {
            case DEBUG_DEVICE:
                deviceTypeName = "调试子设备";
                break;
            case DISK_MANIPULATOR:
                deviceTypeName = "磁盘操纵器";
                break;
            case TERMINAL:
                deviceTypeName = "终端";
                break;
            case EXTERNAL_STORAGE_BUS:
                deviceTypeName = "外部存储总线";
                break;
            case INPUT_BUS:
                deviceTypeName = "输入总线";
                break;
            default:
                deviceTypeName = "未知设备";
        }
        
        player.sendMessage(ChatColor.GREEN + "===== " + deviceTypeName + "详情 =====");
        player.sendMessage(ChatColor.GRAY + "位置: " + ChatColor.WHITE + 
                location.getWorld().getName() + " " +
                location.getBlockX() + "," + 
                location.getBlockY() + "," + 
                location.getBlockZ());
        player.sendMessage(ChatColor.GRAY + "网络: " + ChatColor.WHITE + network.getName());
        player.sendMessage(ChatColor.GRAY + "状态: " + ChatColor.GREEN + "已连接");
        
        // 磁盘操纵器显示额外信息
        if (device.type == DeviceType.DISK_MANIPULATOR) {
            DiskManipulatorData manipulator = (DiskManipulatorData) device.data;
            int diskCount = 0;
            if (manipulator.slots != null) {
                for (UUID diskUuid : manipulator.slots) {
                    if (diskUuid != null) diskCount++;
                }
            }
            player.sendMessage(ChatColor.GRAY + "磁盘数量: " + ChatColor.WHITE + diskCount + "/8");
        }
        
        // 外部存储总线显示额外信息
        if (device.type == DeviceType.EXTERNAL_STORAGE_BUS) {
            ExternalStorageBusData bus = (ExternalStorageBusData) device.data;
            player.sendMessage(ChatColor.GRAY + "绑定容器: " + ChatColor.WHITE + bus.getContainerDisplayName());
            Location containerLoc = bus.getContainerLocation();
            if (containerLoc != null) {
                player.sendMessage(ChatColor.GRAY + "容器位置: " + ChatColor.WHITE + 
                        containerLoc.getWorld().getName() + " " +
                        containerLoc.getBlockX() + "," + 
                        containerLoc.getBlockY() + "," + 
                        containerLoc.getBlockZ());
            }
        }
        
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "使用连接工具右键点击此设备可断开连接");
    }

    /**
     * 设置最后一排：翻页控制
     */
    private void setupLastRow() {
        // 淡灰色玻璃板填充
        ItemStack glassPane = new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                .setName(" ")
                .build();

        for (int i = 45; i <= 53; i++) {
            if (i != PREV_PAGE_SLOT && i != PAGE_INFO_SLOT && i != NEXT_PAGE_SLOT) {
                setItem(i, glassPane);
            }
        }

        // 上一页按钮
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

        // 页数显示（钟）
        ItemStack pageInfoItem = new ItemBuilder(Material.BELL)
                .setName(ChatColor.GOLD + "页面信息")
                .setLore(
                        ChatColor.GRAY + "当前页: " + ChatColor.YELLOW + currentPage,
                        ChatColor.GRAY + "总页数: " + ChatColor.YELLOW + totalPages
                )
                .build();
        setItem(PAGE_INFO_SLOT, pageInfoItem);

        // 下一页按钮
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

    @Override
    // 修复 handlePlayerInventoryClick 方法
    protected void handlePlayerInventoryClick(InventoryClickEvent event, int slot, ItemStack item, ClickType clickType) {
        event.setCancelled(true);
    }
    @Override
    protected void onOpen() {
        player.sendMessage(ChatColor.GREEN + "已打开网络控制器界面");
    }

    @Override
    protected void onClose() {
        // 关闭时不需要特殊处理
    }

    /**
     * 获取网络
     */
    public StorageNetwork getNetwork() {
        return network;
    }

    /**
     * 获取当前页
     */
    public int getCurrentPage() {
        return currentPage;
    }

    /**
     * 设置当前页
     */
    public void setCurrentPage(int page) {
        this.currentPage = Math.max(1, Math.min(page, totalPages));
    }
}