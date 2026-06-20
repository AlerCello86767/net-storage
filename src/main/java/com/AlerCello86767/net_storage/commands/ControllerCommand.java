package com.AlerCello86767.net_storage.commands;

import com.AlerCello86767.net_storage.Net_storage;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ControllerCommand implements CommandExecutor {

    private final Net_storage plugin;
    private final NamespacedKey ITEM_TYPE_KEY;

    public ControllerCommand(Net_storage plugin) {
        this.plugin = plugin;
        this.ITEM_TYPE_KEY = new NamespacedKey(plugin, "item_type");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令！");
            return true;
        }

        if (!player.hasPermission("netstorage.admin")) {
            player.sendMessage(ChatColor.RED + "你没有权限使用此命令！");
            return true;
        }

        if (args.length < 2 || !args[0].equalsIgnoreCase("give")) {
            player.sendMessage(ChatColor.RED + "用法: /netdebug give <类型> [数量]");
            player.sendMessage(ChatColor.GRAY + "可用类型: controller");
            return true;
        }

        String type = args[1].toLowerCase();
        int amount = 1;
        if (args.length > 2) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount <= 0 || amount > 64) {
                    amount = 1;
                }
            } catch (NumberFormatException e) {
                amount = 1;
            }
        }

        ItemStack item = switch (type) {
            case "controller" -> createControllerItem();
            case "connect_tool" -> createConnectToolItem();
            case "debug_device" -> createDebugDeviceItem();
            case "disk_manipulator" -> createDiskManipulatorItem();
            case "terminal" -> createTerminalItem();
            case "external_storage_bus" -> createExternalStorageBusItem();
            case "input_bus" -> createInputBusItem();
            case "disk_1k", "disk_4k", "disk_16k" -> plugin.getDiskManager().createDiskItem(type);
            default -> null;
        };

        if (item == null) {
            player.sendMessage(ChatColor.RED + "未知类型: " + type);
            player.sendMessage(ChatColor.GRAY + "可用类型: controller, connect_tool, debug_device, disk_manipulator, terminal, external_storage_bus, disk_1k, disk_4k, disk_16k");
            return true;
        }

        item.setAmount(amount);
        player.getInventory().addItem(item);
        player.sendMessage(ChatColor.GREEN + "已获得 " + amount + " 个 " + type + "！");

        return true;
    }

    private ItemStack createConnectToolItem() {
        ItemStack item = new ItemStack(Material.STICK);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) return item;

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&a网络连接工具"));

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "右键网络控制器选择网络");
        lore.add(ChatColor.GRAY + "右键子设备进行连接");
        lore.add(ChatColor.DARK_GRAY + "类型: connect_tool");
        meta.setLore(lore);

        meta.getPersistentDataContainer().set(ITEM_TYPE_KEY, PersistentDataType.STRING, "connect_tool");

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createDebugDeviceItem() {
        ItemStack item = new ItemStack(Material.RED_CONCRETE);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) return item;

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&c调试子设备"));

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "放置后自动检测附近控制器");
        lore.add(ChatColor.GRAY + "未连接: 红色 | 已连接: 绿色");
        lore.add(ChatColor.DARK_GRAY + "类型: debug_device");
        meta.setLore(lore);

        meta.getPersistentDataContainer().set(ITEM_TYPE_KEY, PersistentDataType.STRING, "debug_device");

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createControllerItem() {
        ItemStack item = new ItemStack(Material.HONEYCOMB_BLOCK);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) return item;

        String itemName = plugin.getConfigManager().getConfig()
                .getString("controller.item-name", "&e网络控制器");
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', itemName));

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "放置后自动创建存储网络");
        lore.add(ChatColor.GRAY + "破坏时自动删除网络");
        lore.add(ChatColor.DARK_GRAY + "类型: controller");
        meta.setLore(lore);

        meta.getPersistentDataContainer().set(ITEM_TYPE_KEY, PersistentDataType.STRING, "controller");

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createDiskManipulatorItem() {
        ItemStack item = new ItemStack(Material.FURNACE);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) return item;

        String itemName = plugin.getConfigManager().getConfig()
                .getString("disk.manipulator.item-name", "&b磁盘操纵器");
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', itemName));

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "用于插入和管理磁盘");
        lore.add(ChatColor.GRAY + "右键打开磁盘管理界面");
        lore.add(ChatColor.GRAY + "最多插入8个磁盘");
        lore.add(ChatColor.DARK_GRAY + "类型: disk_manipulator");
        meta.setLore(lore);

        meta.getPersistentDataContainer().set(ITEM_TYPE_KEY, PersistentDataType.STRING, "disk_manipulator");

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createTerminalItem() {
        ItemStack item = new ItemStack(Material.BARREL);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) return item;

        String itemName = plugin.getConfigManager().getConfig()
                .getString("disk.terminal.item-name", "&a终端");
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', itemName));

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "统一的网络存储界面");
        lore.add(ChatColor.GRAY + "可存储和取出所有磁盘中的物品");
        lore.add(ChatColor.GRAY + "需要连接磁盘操纵器使用");
        lore.add(ChatColor.DARK_GRAY + "类型: terminal");
        meta.setLore(lore);

        meta.getPersistentDataContainer().set(ITEM_TYPE_KEY, PersistentDataType.STRING, "terminal");

        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createExternalStorageBusItem() {
        ItemStack item = new ItemStack(Material.END_ROD);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) return item;

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&d外部存储总线"));

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "将原版容器连接到网络");
        lore.add(ChatColor.GRAY + "底座对准容器放置");
        lore.add(ChatColor.GRAY + "支持: 箱子、漏斗、熔炉等");
        lore.add(ChatColor.DARK_GRAY + "类型: external_storage_bus");
        meta.setLore(lore);

        meta.getPersistentDataContainer().set(ITEM_TYPE_KEY, PersistentDataType.STRING, "external_storage_bus");

        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createInputBusItem() {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) return item;

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&d输入总线"));

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "自动传输容器物品到网络");
        lore.add(ChatColor.GRAY + "底座对准容器放置");
        lore.add(ChatColor.GRAY + "支持: 箱子、漏斗、熔炉等");
        lore.add(ChatColor.DARK_GRAY + "类型: input_bus");
        meta.setLore(lore);

        meta.getPersistentDataContainer().set(ITEM_TYPE_KEY, PersistentDataType.STRING, "input_bus");

        item.setItemMeta(meta);
        return item;
    }

    public static String getItemType(Net_storage plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        NamespacedKey key = new NamespacedKey(plugin, "item_type");
        return meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }
}