package com.AlerCello86767.net_storage.controller;

import com.AlerCello86767.net_storage.Net_storage;
import com.AlerCello86767.net_storage.commands.ControllerCommand;
import com.AlerCello86767.net_storage.gui.ControllerGUI;
import com.AlerCello86767.net_storage.gui.DiskManipulatorGUI;
import com.AlerCello86767.net_storage.gui.InputBusGUI;
import com.AlerCello86767.net_storage.gui.OutputBusGUI;
import com.AlerCello86767.net_storage.gui.TerminalGUI;
import com.AlerCello86767.net_storage.network.StorageNetwork;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class ControllerListener implements Listener {

    private final Net_storage plugin;

    public ControllerListener(Net_storage plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();
        String itemType = ControllerCommand.getItemType(plugin, item);

        if (itemType == null) return;

        Block block = event.getBlockPlaced();
        Location location = block.getLocation();

        switch (itemType) {
            case "controller" -> handleControllerPlace(player, location, item);
            case "debug_device" -> handleDebugDevicePlace(player, location);
            case "disk_manipulator" -> handleDiskManipulatorPlace(player, location);
            case "terminal" -> handleTerminalPlace(player, location);
            case "external_storage_bus" -> handleExternalStorageBusPlace(player, location, block, event);
            case "input_bus" -> handleInputBusPlace(player, location, block, event);
            case "output_bus" -> handleOutputBusPlace(player, location, block, event);
        }
    }

    private void handleControllerPlace(Player player, Location location, ItemStack item) {
        if (plugin.getControllerManager().isController(location)) {
            player.sendMessage(ChatColor.RED + "该位置已有控制器！");
            return;
        }

        String prefix = plugin.getConfigManager().getConfig()
                .getString("controller.network-name-prefix", "控制器网络-");
        String baseName = prefix + player.getName();
        String networkName = generateUniqueNetworkName(baseName, player.getUniqueId());

        StorageNetwork network = plugin.getNetworkManager().createNetwork(networkName, player);

        if (network == null) {
            player.sendMessage(ChatColor.RED + "网络创建失败！可能已达到最大网络数量限制。");
            return;
        }

        plugin.getNetworkManager().saveNetwork(network);
        plugin.getControllerManager().registerController(location, network.getNetworkId());
        plugin.getControllerManager().saveController(location, network.getNetworkId(), player.getUniqueId());

        player.sendMessage(ChatColor.GREEN + "网络控制器放置成功！");
        player.sendMessage(ChatColor.AQUA + "网络名称: " + ChatColor.WHITE + network.getName());
        player.sendMessage(ChatColor.AQUA + "网络ID: " + ChatColor.WHITE + network.getNetworkId());
        player.sendMessage(ChatColor.GRAY + "使用 /network info " + network.getName() + " 查看详情");
    }

    private void handleDebugDevicePlace(Player player, Location location) {
        if (plugin.getControllerManager().isDebugDevice(location)) {
            player.sendMessage(ChatColor.RED + "该位置已有调试子设备！");
            return;
        }

        // 扫描周围6格是否有控制器
        UUID nearbyNetworkId = plugin.getControllerManager().scanNearbyController(location);

        if (nearbyNetworkId != null) {
            // 自动连接
            StorageNetwork network = plugin.getNetworkManager().getNetwork(nearbyNetworkId);
            if (network != null) {
                plugin.getControllerManager().registerDebugDevice(location, nearbyNetworkId);
                plugin.getControllerManager().saveDebugDeviceToDB(location, nearbyNetworkId);
                plugin.getControllerManager().updateDebugDeviceBlock(location, true);

                player.sendMessage(ChatColor.GREEN + "调试子设备放置成功！");
                player.sendMessage(ChatColor.AQUA + "已自动连接到网络: " + ChatColor.WHITE + network.getName());
                player.sendActionBar(ChatColor.GREEN + "已连接到网络: " + network.getName());
                return;
            }
        }

        // 未检测到控制器，保持红色，等待手动连接
        plugin.getControllerManager().registerDebugDevice(location, null);
        plugin.getControllerManager().saveDebugDeviceToDB(location, null);
        player.sendMessage(ChatColor.YELLOW + "调试子设备放置成功！");
        player.sendMessage(ChatColor.GRAY + "未检测到附近的控制器，请使用连接工具手动连接。");
    }

    private void handleDiskManipulatorPlace(Player player, Location location) {
        if (plugin.getControllerManager().isDiskManipulator(location)) {
            player.sendMessage(ChatColor.RED + "该位置已有磁盘操纵器！");
            return;
        }

        // 扫描周围6格是否有控制器
        UUID nearbyNetworkId = plugin.getControllerManager().scanNearbyController(location);

        if (nearbyNetworkId != null) {
            // 自动连接
            StorageNetwork network = plugin.getNetworkManager().getNetwork(nearbyNetworkId);
            if (network != null) {
                plugin.getControllerManager().registerDiskManipulator(location, nearbyNetworkId);
                plugin.getControllerManager().updateDeviceBlock(location, Material.ORANGE_CONCRETE, true);

                player.sendMessage(ChatColor.GREEN + "磁盘操纵器放置成功！");
                player.sendMessage(ChatColor.AQUA + "已自动连接到网络: " + ChatColor.WHITE + network.getName());
                player.sendActionBar(ChatColor.GREEN + "已连接到网络: " + network.getName());
                return;
            }
        }

        // 未检测到控制器，保持橙色，等待手动连接
        plugin.getControllerManager().registerDiskManipulator(location, null);
        plugin.getControllerManager().updateDeviceBlock(location, Material.ORANGE_CONCRETE, false);
        player.sendMessage(ChatColor.YELLOW + "磁盘操纵器放置成功！");
        player.sendMessage(ChatColor.GRAY + "未检测到附近的控制器，请使用连接工具手动连接。");
    }

    private void handleTerminalPlace(Player player, Location location) {
        if (plugin.getControllerManager().isTerminal(location)) {
            player.sendMessage(ChatColor.RED + "该位置已有终端！");
            return;
        }

        // 扫描周围6格是否有控制器
        UUID nearbyNetworkId = plugin.getControllerManager().scanNearbyController(location);

        if (nearbyNetworkId != null) {
            // 自动连接
            StorageNetwork network = plugin.getNetworkManager().getNetwork(nearbyNetworkId);
            if (network != null) {
                plugin.getControllerManager().registerTerminal(location, nearbyNetworkId);
                plugin.getControllerManager().updateDeviceBlock(location, Material.CYAN_CONCRETE, true);

                player.sendMessage(ChatColor.GREEN + "终端放置成功！");
                player.sendMessage(ChatColor.AQUA + "已自动连接到网络: " + ChatColor.WHITE + network.getName());
                player.sendActionBar(ChatColor.GREEN + "已连接到网络: " + network.getName());
                return;
            }
        }

        // 未检测到控制器，保持青色，等待手动连接
        plugin.getControllerManager().registerTerminal(location, null);
        plugin.getControllerManager().updateDeviceBlock(location, Material.CYAN_CONCRETE, false);
        player.sendMessage(ChatColor.YELLOW + "终端放置成功！");
        player.sendMessage(ChatColor.GRAY + "未检测到附近的控制器，请使用连接工具手动连接。");
    }
    
    private void handleExternalStorageBusPlace(Player player, Location location, Block block, BlockPlaceEvent event) {
        if (plugin.getControllerManager().isExternalStorageBus(location)) {
            player.sendMessage(ChatColor.RED + "该位置已有外部存储总线！");
            return;
        }
        
        org.bukkit.block.data.Directional directionalData = (org.bukkit.block.data.Directional) block.getBlockData();
        org.bukkit.block.BlockFace facing = directionalData.getFacing();
        
        Block containerBlock = block.getRelative(facing.getOppositeFace());
        
        if (!isContainer(containerBlock)) {
            player.sendMessage(ChatColor.RED + "请将底座对准容器放置！");
            player.sendMessage(ChatColor.GRAY + "支持的容器: 箱子、陷阱箱、漏斗、熔炉、高炉、烟熏炉、发射器、投掷器、潜影盒");
            event.setCancelled(true);
            return;
        }
        
        // 放置时生成新的UUID
        UUID busUuid = UUID.randomUUID();
        
        plugin.getControllerManager().registerExternalStorageBus(
                busUuid,
                location, 
                null, 
                containerBlock.getLocation(), 
                containerBlock.getType().name()
        );
        
        player.sendMessage(ChatColor.GREEN + "外部存储总线放置成功！");
        player.sendMessage(ChatColor.AQUA + "已绑定容器: " + ChatColor.WHITE + getContainerDisplayName(containerBlock.getType().name()));
        player.sendMessage(ChatColor.GRAY + "总线UUID: " + ChatColor.DARK_GRAY + busUuid.toString());
        player.sendMessage(ChatColor.GRAY + "使用连接工具连接到网络");
    }
    
    private void handleInputBusPlace(Player player, Location location, Block block, BlockPlaceEvent event) {
        if (plugin.getControllerManager().isInputBus(location)) {
            player.sendMessage(ChatColor.RED + "该位置已有输入总线！");
            return;
        }
        
        org.bukkit.block.data.Directional directionalData = (org.bukkit.block.data.Directional) block.getBlockData();
        org.bukkit.block.BlockFace facing = directionalData.getFacing();
        
        Block containerBlock = block.getRelative(facing.getOppositeFace());
        
        if (!isContainer(containerBlock)) {
            player.sendMessage(ChatColor.RED + "请将底座对准容器放置！");
            player.sendMessage(ChatColor.GRAY + "支持的容器: 箱子、陷阱箱、漏斗、熔炉、高炉、烟熏炉、发射器、投掷器、潜影盒");
            event.setCancelled(true);
            return;
        }
        
        // 放置时生成新的UUID
        UUID busUuid = UUID.randomUUID();
        
        plugin.getControllerManager().registerInputBus(
                busUuid,
                location, 
                null, 
                containerBlock.getLocation(), 
                containerBlock.getType().name()
        );
        
        player.sendMessage(ChatColor.GREEN + "输入总线放置成功！");
        player.sendMessage(ChatColor.AQUA + "已绑定容器: " + ChatColor.WHITE + getContainerDisplayName(containerBlock.getType().name()));
        player.sendMessage(ChatColor.GRAY + "总线UUID: " + ChatColor.DARK_GRAY + busUuid.toString());
        player.sendMessage(ChatColor.GRAY + "使用连接工具连接到网络");
    }
    
    private void handleOutputBusPlace(Player player, Location location, Block block, BlockPlaceEvent event) {
        if (plugin.getControllerManager().isOutputBus(location)) {
            player.sendMessage(ChatColor.RED + "该位置已有输出总线！");
            return;
        }
        
        org.bukkit.block.data.Directional directionalData = (org.bukkit.block.data.Directional) block.getBlockData();
        org.bukkit.block.BlockFace facing = directionalData.getFacing();
        
        Block containerBlock = block.getRelative(facing.getOppositeFace());
        
        if (!isContainer(containerBlock)) {
            player.sendMessage(ChatColor.RED + "请将底座对准容器放置！");
            player.sendMessage(ChatColor.GRAY + "支持的容器: 箱子、陷阱箱、漏斗、熔炉、高炉、烟熏炉、发射器、投掷器、潜影盒");
            event.setCancelled(true);
            return;
        }
        
        // 放置时生成新的UUID
        UUID busUuid = UUID.randomUUID();
        
        plugin.getControllerManager().registerOutputBus(
                busUuid,
                location, 
                null, 
                containerBlock.getLocation(), 
                containerBlock.getType().name()
        );
        
        player.sendMessage(ChatColor.GREEN + "输出总线放置成功！");
        player.sendMessage(ChatColor.AQUA + "已绑定容器: " + ChatColor.WHITE + getContainerDisplayName(containerBlock.getType().name()));
        player.sendMessage(ChatColor.GRAY + "总线UUID: " + ChatColor.DARK_GRAY + busUuid.toString());
        player.sendMessage(ChatColor.GRAY + "使用连接工具连接到网络");
    }
    
    private boolean isContainer(Block block) {
        Material type = block.getType();
        return type == Material.CHEST 
                || type == Material.TRAPPED_CHEST 
                || type == Material.ENDER_CHEST 
                || type == Material.HOPPER 
                || type == Material.FURNACE 
                || type == Material.BLAST_FURNACE 
                || type == Material.SMOKER 
                || type == Material.DISPENSER 
                || type == Material.DROPPER 
                || type == Material.SHULKER_BOX;
    }
    
    private String getContainerDisplayName(String containerType) {
        switch (containerType) {
            case "CHEST":
                return "箱子";
            case "TRAPPED_CHEST":
                return "陷阱箱";
            case "ENDER_CHEST":
                return "末影箱";
            case "HOPPER":
                return "漏斗";
            case "FURNACE":
                return "熔炉";
            case "BLAST_FURNACE":
                return "高炉";
            case "SMOKER":
                return "烟熏炉";
            case "DISPENSER":
                return "发射器";
            case "DROPPER":
                return "投掷器";
            case "SHULKER_BOX":
                return "潜影盒";
            default:
                return containerType;
        }
    }

    private String generateUniqueNetworkName(String baseName, UUID playerUUID) {
        String networkName = baseName;
        int counter = 1;

        while (plugin.getNetworkManager().getNetworkByName(networkName) != null) {
            networkName = baseName + "#" + counter;
            counter++;
        }

        return networkName;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location location = block.getLocation();

        // 检查是否是控制器
        if (plugin.getControllerManager().isController(location)) {
            handleControllerBreak(player, location, event);
            return;
        }

        // 检查是否是调试子设备
        if (plugin.getControllerManager().isDebugDevice(location)) {
            handleDebugDeviceBreak(player, location, event);
            return;
        }
        
        // 检查是否是磁盘操纵器
        if (plugin.getControllerManager().isDiskManipulator(location)) {
            handleDiskManipulatorBreak(player, location, event);
            return;
        }
        
        // 检查是否是终端
        if (plugin.getControllerManager().isTerminal(location)) {
            handleTerminalBreak(player, location, event);
            return;
        }
        
        // 检查是否是外部存储总线
        if (plugin.getControllerManager().isExternalStorageBus(location)) {
            handleExternalStorageBusBreak(player, location, event);
            return;
        }
        
        // 检查是否是输入总线
        if (plugin.getControllerManager().isInputBus(location)) {
            handleInputBusBreak(player, location, event);
            return;
        }
        
        // 检查是否是输出总线
        if (plugin.getControllerManager().isOutputBus(location)) {
            handleOutputBusBreak(player, location, event);
            return;
        }
    }

    private void handleControllerBreak(Player player, Location location, BlockBreakEvent event) {
        UUID networkId = plugin.getControllerManager().getNetworkId(location);
        StorageNetwork network = plugin.getNetworkManager().getNetwork(networkId);

        if (network == null) {
            plugin.getLogger().warning("控制器位置 " + location + " 对应的网络不存在");
            plugin.getControllerManager().deleteController(location);
            return;
        }

        if (!network.hasManagePermission(player)) {
            player.sendMessage(ChatColor.RED + "你无权删除此网络！");
            player.sendMessage(ChatColor.GRAY + "只有网络创建者或管理员才能删除网络。");
            event.setDropItems(false);
            event.setCancelled(true);
            return;
        }

        plugin.getNetworkManager().deleteNetwork(networkId);
        plugin.getControllerManager().deleteController(location);

        player.sendMessage(ChatColor.GREEN + "网络控制器已破坏！");
        player.sendMessage(ChatColor.RED + "网络 '" + network.getName() + "' 已删除");
    }

    private void handleDebugDeviceBreak(Player player, Location location, BlockBreakEvent event) {
        UUID networkId = plugin.getControllerManager().getDebugDeviceNetwork(location);
        plugin.getControllerManager().unregisterDebugDevice(location);
        plugin.getControllerManager().deleteDebugDeviceFromDB(location);

        if (networkId != null) {
            StorageNetwork network = plugin.getNetworkManager().getNetwork(networkId);
            if (network != null) {
                player.sendMessage(ChatColor.GREEN + "调试子设备已破坏！");
                player.sendMessage(ChatColor.GRAY + "已从网络 '" + network.getName() + "' 断开连接");
            } else {
                player.sendMessage(ChatColor.GREEN + "调试子设备已破坏！");
            }
        } else {
            player.sendMessage(ChatColor.GREEN + "调试子设备已破坏！");
        }
    }
    
    private void handleDiskManipulatorBreak(Player player, Location location, BlockBreakEvent event) {
        DiskManipulatorData manipulator = plugin.getControllerManager().getDiskManipulator(location);
        
        // 取出磁盘并掉落
        if (manipulator != null && manipulator.slots != null) {
            for (int i = 0; i < manipulator.slots.length; i++) {
                UUID diskUuid = manipulator.slots[i];
                if (diskUuid != null) {
                    ItemStack diskItem = plugin.getDiskManager().getDiskItemFromUUID(diskUuid);
                    if (diskItem != null) {
                        location.getWorld().dropItemNaturally(location, diskItem);
                    }
                }
            }
        }
        
        UUID networkId = manipulator != null ? manipulator.networkId : null;
        plugin.getControllerManager().unregisterDiskManipulator(location);
        
        if (networkId != null) {
            StorageNetwork network = plugin.getNetworkManager().getNetwork(networkId);
            if (network != null) {
                player.sendMessage(ChatColor.GREEN + "磁盘操纵器已破坏！");
                player.sendMessage(ChatColor.GRAY + "已从网络 '" + network.getName() + "' 断开连接");
                player.sendMessage(ChatColor.YELLOW + "磁盘已掉落，请注意捡取！");
            } else {
                player.sendMessage(ChatColor.GREEN + "磁盘操纵器已破坏！");
            }
        } else {
            player.sendMessage(ChatColor.GREEN + "磁盘操纵器已破坏！");
        }
    }
    
    private void handleTerminalBreak(Player player, Location location, BlockBreakEvent event) {
        TerminalData terminal = plugin.getControllerManager().getTerminal(location);
        UUID networkId = terminal != null ? terminal.networkId : null;
        
        plugin.getControllerManager().unregisterTerminal(location);
        
        if (networkId != null) {
            StorageNetwork network = plugin.getNetworkManager().getNetwork(networkId);
            if (network != null) {
                player.sendMessage(ChatColor.GREEN + "终端已破坏！");
                player.sendMessage(ChatColor.GRAY + "已从网络 '" + network.getName() + "' 断开连接");
            } else {
                player.sendMessage(ChatColor.GREEN + "终端已破坏！");
            }
        } else {
            player.sendMessage(ChatColor.GREEN + "终端已破坏！");
        }
    }
    
    private void handleExternalStorageBusBreak(Player player, Location location, BlockBreakEvent event) {
        ExternalStorageBusData data = plugin.getControllerManager().getExternalStorageBus(location);
        plugin.getControllerManager().unregisterExternalStorageBus(location);
        
        if (data != null && data.networkId != null) {
            StorageNetwork network = plugin.getNetworkManager().getNetwork(data.networkId);
            if (network != null) {
                player.sendMessage(ChatColor.GREEN + "外部存储总线已破坏！");
                player.sendMessage(ChatColor.GRAY + "已从网络 '" + network.getName() + "' 断开连接");
                player.sendMessage(ChatColor.AQUA + "绑定容器: " + data.getContainerDisplayName());
            } else {
                player.sendMessage(ChatColor.GREEN + "外部存储总线已破坏！");
            }
        } else {
            player.sendMessage(ChatColor.GREEN + "外部存储总线已破坏！");
        }
    }
    
    private void handleInputBusBreak(Player player, Location location, BlockBreakEvent event) {
        InputBusData data = plugin.getControllerManager().getInputBus(location);
        plugin.getControllerManager().unregisterInputBus(location);
        
        if (data != null && data.networkId != null) {
            StorageNetwork network = plugin.getNetworkManager().getNetwork(data.networkId);
            if (network != null) {
                player.sendMessage(ChatColor.GREEN + "输入总线已破坏！");
                player.sendMessage(ChatColor.GRAY + "已从网络 '" + network.getName() + "' 断开连接");
                player.sendMessage(ChatColor.AQUA + "绑定容器: " + data.getContainerDisplayName());
            } else {
                player.sendMessage(ChatColor.GREEN + "输入总线已破坏！");
            }
        } else {
            player.sendMessage(ChatColor.GREEN + "输入总线已破坏！");
        }
    }
    
    private void handleOutputBusBreak(Player player, Location location, BlockBreakEvent event) {
        OutputBusData data = plugin.getControllerManager().getOutputBus(location);
        plugin.getControllerManager().unregisterOutputBus(location);
        
        if (data != null && data.networkId != null) {
            StorageNetwork network = plugin.getNetworkManager().getNetwork(data.networkId);
            if (network != null) {
                player.sendMessage(ChatColor.GREEN + "输出总线已破坏！");
                player.sendMessage(ChatColor.GRAY + "已从网络 '" + network.getName() + "' 断开连接");
                player.sendMessage(ChatColor.AQUA + "绑定容器: " + data.getContainerDisplayName());
            } else {
                player.sendMessage(ChatColor.GREEN + "输出总线已破坏！");
            }
        } else {
            player.sendMessage(ChatColor.GREEN + "输出总线已破坏！");
        }
    }
    
    private void showInputBusInfo(Player player, Location location) {
        InputBusData data = plugin.getControllerManager().getInputBus(location);
        if (data == null) {
            player.sendMessage(ChatColor.RED + "输入总线数据不存在！");
            return;
        }
        
        player.sendMessage(ChatColor.LIGHT_PURPLE + "===== 输入总线信息 =====");
        player.sendMessage(ChatColor.GRAY + "总线UUID: " + ChatColor.WHITE + data.busUuid);
        
        if (data.networkId != null) {
            StorageNetwork network = plugin.getNetworkManager().getNetwork(data.networkId);
            if (network != null) {
                player.sendMessage(ChatColor.GRAY + "网络: " + ChatColor.AQUA + network.getName());
            } else {
                player.sendMessage(ChatColor.GRAY + "网络: " + ChatColor.RED + "已断开");
            }
        } else {
            player.sendMessage(ChatColor.GRAY + "网络: " + ChatColor.YELLOW + "未连接");
        }
        
        player.sendMessage(ChatColor.GRAY + "绑定容器: " + ChatColor.WHITE + data.getContainerDisplayName());
        player.sendMessage(ChatColor.GRAY + "状态: " + (data.networkId != null ? ChatColor.GREEN + "运行中" : ChatColor.YELLOW + "等待连接"));
        player.sendMessage(ChatColor.DARK_GRAY + "使用连接工具连接到网络");
    }
    
    private void showOutputBusInfo(Player player, Location location) {
        OutputBusData data = plugin.getControllerManager().getOutputBus(location);
        if (data == null) {
            player.sendMessage(ChatColor.RED + "输出总线数据不存在！");
            return;
        }
        
        player.sendMessage(ChatColor.LIGHT_PURPLE + "===== 输出总线信息 =====");
        player.sendMessage(ChatColor.GRAY + "总线UUID: " + ChatColor.WHITE + data.busUuid);
        
        if (data.networkId != null) {
            StorageNetwork network = plugin.getNetworkManager().getNetwork(data.networkId);
            if (network != null) {
                player.sendMessage(ChatColor.GRAY + "网络: " + ChatColor.AQUA + network.getName());
            } else {
                player.sendMessage(ChatColor.GRAY + "网络: " + ChatColor.RED + "已断开");
            }
        } else {
            player.sendMessage(ChatColor.GRAY + "网络: " + ChatColor.YELLOW + "未连接");
        }
        
        player.sendMessage(ChatColor.GRAY + "绑定容器: " + ChatColor.WHITE + data.getContainerDisplayName());
        player.sendMessage(ChatColor.GRAY + "状态: " + (data.networkId != null ? ChatColor.GREEN + "运行中" : ChatColor.YELLOW + "等待连接"));
        player.sendMessage(ChatColor.DARK_GRAY + "使用连接工具连接到网络");
    }
    
    private void showExternalStorageBusInfo(Player player, Location location) {
        ExternalStorageBusData data = plugin.getControllerManager().getExternalStorageBus(location);
        if (data == null) {
            player.sendMessage(ChatColor.RED + "外部存储总线数据不存在！");
            return;
        }
        
        player.sendMessage(ChatColor.DARK_PURPLE + "===== 外部存储总线信息 =====");
        player.sendMessage(ChatColor.GRAY + "总线UUID: " + ChatColor.WHITE + data.busUuid);
        
        if (data.networkId != null) {
            StorageNetwork network = plugin.getNetworkManager().getNetwork(data.networkId);
            if (network != null) {
                player.sendMessage(ChatColor.GRAY + "网络: " + ChatColor.AQUA + network.getName());
            } else {
                player.sendMessage(ChatColor.GRAY + "网络: " + ChatColor.RED + "已断开");
            }
        } else {
            player.sendMessage(ChatColor.GRAY + "网络: " + ChatColor.YELLOW + "未连接");
        }
        
        player.sendMessage(ChatColor.GRAY + "绑定容器: " + ChatColor.WHITE + data.getContainerDisplayName());
        player.sendMessage(ChatColor.GRAY + "状态: " + (data.networkId != null ? ChatColor.GREEN + "运行中" : ChatColor.YELLOW + "等待连接"));
        player.sendMessage(ChatColor.DARK_GRAY + "使用连接工具连接到网络");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // 如果事件已被取消，直接返回
        if (event.isCancelled()) return;
        
        Player player = event.getPlayer();
        ItemStack handItem = player.getInventory().getItemInMainHand();
        String itemType = ControllerCommand.getItemType(plugin, handItem);

        // 连接工具处理
        if ("connect_tool".equals(itemType)) {
            handleConnectToolInteraction(event, player);
            return;
        }

        // 方块右键交互
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block == null) return;

            Location location = block.getLocation();
            
            // 控制器右键打开GUI
            if (plugin.getControllerManager().isController(location)) {
                event.setCancelled(true);
                openControllerGUI(player, location);
                return;
            }
            
            // 磁盘操纵器右键打开GUI
            if (plugin.getControllerManager().isDiskManipulator(location)) {
                event.setCancelled(true);
                openDiskManipulatorGUI(player, location);
                return;
            }
            
            // 终端右键打开GUI
            if (plugin.getControllerManager().isTerminal(location)) {
                event.setCancelled(true);
                openTerminalGUI(player, location);
                return;
            }
            
            // 输入总线右键打开GUI
            if (plugin.getControllerManager().isInputBus(location)) {
                event.setCancelled(true);
                openInputBusGUI(player, location);
                return;
            }
            
            // 输出总线右键打开GUI
            if (plugin.getControllerManager().isOutputBus(location)) {
                event.setCancelled(true);
                openOutputBusGUI(player, location);
                return;
            }
            
            // 外部存储总线右键显示信息
            if (plugin.getControllerManager().isExternalStorageBus(location)) {
                event.setCancelled(true);
                showExternalStorageBusInfo(player, location);
                return;
            }
        }
    }

    private void handleConnectToolInteraction(PlayerInteractEvent event, Player player) {
        Action action = event.getAction();
        Block block = event.getClickedBlock();

        // 潜行+右键：取消选择
        if (player.isSneaking() && action == Action.RIGHT_CLICK_AIR || 
            player.isSneaking() && action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            if (plugin.getControllerManager().hasSelectedNetwork(player.getUniqueId())) {
                plugin.getControllerManager().clearSelectedNetwork(player.getUniqueId());
                player.sendActionBar(ChatColor.YELLOW + "已取消网络选择");
                player.sendMessage(ChatColor.YELLOW + "已取消网络选择");
            }
            return;
        }

        // 左键点击控制器：选择网络
        if (action == Action.LEFT_CLICK_BLOCK && block != null) {
            Location location = block.getLocation();
            if (plugin.getControllerManager().isController(location)) {
                event.setCancelled(true);
                handleConnectToolSelectNetwork(player, location);
            }
            return;
        }

        // 右键点击子设备：连接网络
        if (action == Action.RIGHT_CLICK_BLOCK && block != null) {
            Location location = block.getLocation();
            if (plugin.getControllerManager().isDebugDevice(location)) {
                event.setCancelled(true);
                handleConnectToolConnectDevice(player, location, "调试子设备");
            } else if (plugin.getControllerManager().isDiskManipulator(location)) {
                event.setCancelled(true);
                handleConnectToolConnectDevice(player, location, "磁盘操纵器");
            } else if (plugin.getControllerManager().isTerminal(location)) {
                event.setCancelled(true);
                handleConnectToolConnectDevice(player, location, "终端");
            } else if (plugin.getControllerManager().isExternalStorageBus(location)) {
                event.setCancelled(true);
                handleConnectToolConnectDevice(player, location, "外部存储总线");
            } else if (plugin.getControllerManager().isInputBus(location)) {
                event.setCancelled(true);
                handleConnectToolConnectDevice(player, location, "输入总线");
            } else if (plugin.getControllerManager().isOutputBus(location)) {
                event.setCancelled(true);
                handleConnectToolConnectDevice(player, location, "输出总线");
            }
        }
    }

    private void handleConnectToolSelectNetwork(Player player, Location location) {
        UUID networkId = plugin.getControllerManager().getNetworkId(location);
        StorageNetwork network = plugin.getNetworkManager().getNetwork(networkId);

        if (network == null) {
            player.sendActionBar(ChatColor.RED + "该控制器对应的网络不存在！");
            return;
        }

        plugin.getControllerManager().setSelectedNetwork(player.getUniqueId(), networkId);
        player.sendMessage(ChatColor.GREEN + "已选择网络: " + ChatColor.WHITE + network.getName());
        player.sendMessage(ChatColor.GRAY + "右键点击方块进行连接，潜行+右键取消选择");
    }

    private void handleConnectToolConnectDevice(Player player, Location location, String deviceName) {
        UUID selectedNetworkId = plugin.getControllerManager().getSelectedNetwork(player.getUniqueId());

        if (selectedNetworkId == null) {
            player.sendActionBar(ChatColor.RED + "请先左键控制器选择网络");
            player.sendMessage(ChatColor.RED + "请先使用连接工具左键控制器选择网络！");
            return;
        }

        StorageNetwork network = plugin.getNetworkManager().getNetwork(selectedNetworkId);
        if (network == null) {
            player.sendActionBar(ChatColor.RED + "选中的网络不存在！");
            plugin.getControllerManager().clearSelectedNetwork(player.getUniqueId());
            return;
        }

        // 连接设备
        if ("调试子设备".equals(deviceName)) {
            plugin.getControllerManager().registerDebugDevice(location, selectedNetworkId);
            plugin.getControllerManager().saveDebugDeviceToDB(location, selectedNetworkId);
            plugin.getControllerManager().updateDebugDeviceBlock(location, true);
        } else if ("磁盘操纵器".equals(deviceName)) {
            plugin.getControllerManager().registerDiskManipulator(location, selectedNetworkId);
            plugin.getControllerManager().updateDeviceBlock(location, Material.ORANGE_CONCRETE, true);
        } else if ("终端".equals(deviceName)) {
            plugin.getControllerManager().registerTerminal(location, selectedNetworkId);
            plugin.getControllerManager().updateDeviceBlock(location, Material.CYAN_CONCRETE, true);
        } else if ("外部存储总线".equals(deviceName)) {
            ExternalStorageBusData data = plugin.getControllerManager().getExternalStorageBus(location);
            if (data != null) {
                data.networkId = selectedNetworkId;
                plugin.getDatabaseManager().saveExternalStorageBusToDB(data);
            }
        } else if ("输入总线".equals(deviceName)) {
            InputBusData data = plugin.getControllerManager().getInputBus(location);
            if (data != null) {
                data.networkId = selectedNetworkId;
                plugin.getDatabaseManager().saveInputBusToDB(data);
            }
        } else if ("输出总线".equals(deviceName)) {
            OutputBusData data = plugin.getControllerManager().getOutputBus(location);
            if (data != null) {
                data.networkId = selectedNetworkId;
                plugin.getDatabaseManager().saveOutputBusToDB(data);
            }
        }

        // 清除选择状态
        plugin.getControllerManager().clearSelectedNetwork(player.getUniqueId());

        player.sendActionBar(ChatColor.GREEN + "已连接到网络: " + ChatColor.WHITE + network.getName());
        player.sendMessage(ChatColor.GREEN + deviceName + "已成功连接到网络: " + ChatColor.WHITE + network.getName());
    }

    private void openControllerGUI(Player player, Location location) {
        UUID networkId = plugin.getControllerManager().getNetworkId(location);
        StorageNetwork network = plugin.getNetworkManager().getNetwork(networkId);

        if (network == null) {
            player.sendMessage(ChatColor.RED + "该控制器对应的网络不存在！");
            return;
        }

        ControllerGUI gui = new ControllerGUI(player, plugin, network);
        plugin.getGuiManager().registerGUI(player, gui);
        gui.open();
    }
    
    private void openDiskManipulatorGUI(Player player, Location location) {
        DiskManipulatorData manipulator = plugin.getControllerManager().getDiskManipulator(location);
        
        if (manipulator == null) {
            player.sendMessage(ChatColor.RED + "该磁盘操纵器数据不存在！");
            return;
        }
        
        DiskManipulatorGUI gui = new DiskManipulatorGUI(player, plugin, manipulator, location);
        plugin.getGuiManager().registerGUI(player, gui);
        gui.open();
    }
    
    private void openTerminalGUI(Player player, Location location) {
        TerminalData terminal = plugin.getControllerManager().getTerminal(location);
        
        if (terminal == null) {
            player.sendMessage(ChatColor.RED + "该终端数据不存在！");
            return;
        }
        
        if (terminal.networkId == null) {
            player.sendMessage(ChatColor.RED + "终端未连接到任何网络！");
            return;
        }
        
        TerminalGUI gui = new TerminalGUI(player, plugin, terminal, location);
        plugin.getGuiManager().registerGUI(player, gui);
        gui.open();
    }
    
    private void openInputBusGUI(Player player, Location location) {
        InputBusData busData = plugin.getControllerManager().getInputBus(location);
        
        if (busData == null) {
            player.sendMessage(ChatColor.RED + "该输入总线数据不存在！");
            return;
        }
        
        InputBusGUI gui = new InputBusGUI(player, plugin, busData);
        plugin.getGuiManager().registerGUI(player, gui);
        gui.open();
    }
    
    private void openOutputBusGUI(Player player, Location location) {
        OutputBusData busData = plugin.getControllerManager().getOutputBus(location);
        
        if (busData == null) {
            player.sendMessage(ChatColor.RED + "该输出总线数据不存在！");
            return;
        }
        
        OutputBusGUI gui = new OutputBusGUI(player, plugin, busData);
        plugin.getGuiManager().registerGUI(player, gui);
        gui.open();
    }
}