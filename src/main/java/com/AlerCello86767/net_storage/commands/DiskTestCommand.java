package com.AlerCello86767.net_storage.commands;

import com.AlerCello86767.net_storage.Net_storage;
import com.AlerCello86767.net_storage.disk.DiskItem;
import com.AlerCello86767.net_storage.disk.DiskManager;
import com.AlerCello86767.net_storage.gui.DiskGUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * 磁盘测试命令处理
 * /netdebug disktest <子命令> [参数]
 */
public class DiskTestCommand implements CommandExecutor {

    private final Net_storage plugin;

    public DiskTestCommand(Net_storage plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "此命令只能由玩家执行！");
            return true;
        }

        Player player = (Player) sender;

        // 检查权限
        if (!player.hasPermission("netstorage.admin")) {
            player.sendMessage(ChatColor.RED + "你没有权限执行此命令！");
            return true;
        }

        // 检查参数
        if (args.length < 1) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "add" -> handleAdd(player, args);
            case "remove" -> handleRemove(player, args);
            case "clean" -> handleClean(player);
            case "check" -> handleCheck(player);
            case "checkgui" -> handleCheckGUI(player);
            default -> sendHelp(player);
        }

        return true;
    }

    /**
     * 添加物品到磁盘
     * /netdebug disktest add <物品名称> [数量]
     */
    private void handleAdd(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /netdebug disktest add <物品名称> [数量]");
            return;
        }

        ItemStack diskItem = player.getInventory().getItemInMainHand();
        DiskManager diskManager = plugin.getDiskManager();

        // 检查是否手持磁盘
        if (!diskManager.isDisk(diskItem)) {
            player.sendMessage(ChatColor.RED + "请手持磁盘！");
            return;
        }

        // 获取磁盘 UUID
        UUID diskUuid = diskManager.getDiskUuidFromItem(diskItem);
        if (diskUuid == null) {
            player.sendMessage(ChatColor.RED + "磁盘数据无效！");
            return;
        }

        // 获取物品名称参数
        String itemName = args[1];

        // 尝试解析为 Material
        Material material = Material.matchMaterial(itemName);
        if (material == null) {
            player.sendMessage(ChatColor.RED + "无效的物品名称: " + itemName);
            return;
        }

        // 检查是否是空气
        if (material == Material.AIR) {
            player.sendMessage(ChatColor.RED + "不能添加空气到磁盘！");
            return;
        }

        // 获取数量参数（默认为1）
        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount <= 0) {
                    player.sendMessage(ChatColor.RED + "数量必须大于0！");
                    return;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "无效的数量: " + args[2]);
                return;
            }
        }

        // 创建物品
        ItemStack itemToAdd = new ItemStack(material, Math.min(amount, material.getMaxStackSize()));

        // 获取磁盘数据
        List<DiskItem> diskData = diskManager.getDiskData(diskUuid);

        // 检查容量
        int currentTotal = diskManager.getTotalItems(diskData);
        int maxCapacity = diskManager.getMaxCapacity();

        if (currentTotal + amount > maxCapacity) {
            player.sendMessage(ChatColor.RED + "磁盘空间不足！当前: " + currentTotal + "/" + maxCapacity);
            return;
        }

        // 添加物品
        int added = diskManager.addItem(diskData, itemToAdd, amount);
        if (added < 0) {
            player.sendMessage(ChatColor.RED + "添加失败！");
            return;
        }

        // 保存磁盘数据
        diskManager.saveDiskData(diskUuid, diskData);

        // 更新磁盘物品的 Lore
        diskManager.updateDiskLore(diskItem, diskData);

        // 发送成功消息
        int newTotal = diskManager.getTotalItems(diskData);
        player.sendMessage(ChatColor.GREEN + "成功添加 " + ChatColor.WHITE + itemName + 
                ChatColor.GREEN + " x" + amount + " 到磁盘！ (使用量: " + newTotal + "/" + maxCapacity + ")");
    }

    /**
     * 从磁盘移除物品
     * /netdebug disktest remove <物品名称> [数量]
     */
    private void handleRemove(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /netdebug disktest remove <物品名称> [数量]");
            return;
        }

        ItemStack diskItem = player.getInventory().getItemInMainHand();
        DiskManager diskManager = plugin.getDiskManager();

        // 检查是否手持磁盘
        if (!diskManager.isDisk(diskItem)) {
            player.sendMessage(ChatColor.RED + "请手持磁盘！");
            return;
        }

        // 获取磁盘 UUID
        UUID diskUuid = diskManager.getDiskUuidFromItem(diskItem);
        if (diskUuid == null) {
            player.sendMessage(ChatColor.RED + "磁盘数据无效！");
            return;
        }

        // 获取物品名称参数
        String itemName = args[1];

        // 获取数量参数（默认为1）
        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount <= 0) {
                    player.sendMessage(ChatColor.RED + "数量必须大于0！");
                    return;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "无效的数量: " + args[2]);
                return;
            }
        }

        // 获取磁盘数据
        List<DiskItem> diskData = diskManager.getDiskData(diskUuid);

        // 查找物品
        DiskItem existing = diskManager.findItemByName(diskData, itemName);
        if (existing == null) {
            player.sendMessage(ChatColor.RED + "磁盘中未找到该物品: " + itemName);
            return;
        }

        // 检查数量
        int removeAmount = Math.min(amount, existing.getAmount());
        int remaining = existing.getAmount() - removeAmount;

        if (remaining <= 0) {
            // 完全移除物品
            diskData.remove(existing);
        } else {
            // 减少数量
            existing.setAmount(remaining);
        }

        // 保存磁盘数据
        diskManager.saveDiskData(diskUuid, diskData);

        // 更新磁盘物品的 Lore
        diskManager.updateDiskLore(diskItem, diskData);

        // 发送成功消息
        int newTotal = diskManager.getTotalItems(diskData);
        int maxCapacity = diskManager.getMaxCapacity();
        player.sendMessage(ChatColor.GREEN + "已从磁盘移除 " + ChatColor.WHITE + itemName + 
                ChatColor.GREEN + " x" + removeAmount + "! (使用量: " + newTotal + "/" + maxCapacity + ")");
    }

    /**
     * 清空磁盘
     * /netdebug disktest clean
     */
    private void handleClean(Player player) {
        ItemStack diskItem = player.getInventory().getItemInMainHand();
        DiskManager diskManager = plugin.getDiskManager();

        // 检查是否手持磁盘
        if (!diskManager.isDisk(diskItem)) {
            player.sendMessage(ChatColor.RED + "请手持磁盘！");
            return;
        }

        // 获取磁盘 UUID
        UUID diskUuid = diskManager.getDiskUuidFromItem(diskItem);
        if (diskUuid == null) {
            player.sendMessage(ChatColor.RED + "磁盘数据无效！");
            return;
        }

        // 获取磁盘数据
        List<DiskItem> diskData = diskManager.getDiskData(diskUuid);

        // 清空磁盘
        diskManager.clearDisk(diskData);

        // 保存磁盘数据
        diskManager.saveDiskData(diskUuid, diskData);

        // 更新磁盘物品的 Lore
        diskManager.updateDiskLore(diskItem, diskData);

        // 发送成功消息
        player.sendMessage(ChatColor.GREEN + "磁盘已清空！");
    }

    /**
     * 查看磁盘内容
     * /netdebug disktest check
     */
    private void handleCheck(Player player) {
        ItemStack diskItem = player.getInventory().getItemInMainHand();
        DiskManager diskManager = plugin.getDiskManager();

        // 检查是否手持磁盘
        if (!diskManager.isDisk(diskItem)) {
            player.sendMessage(ChatColor.RED + "请手持磁盘！");
            return;
        }

        // 获取磁盘 UUID
        UUID diskUuid = diskManager.getDiskUuidFromItem(diskItem);
        if (diskUuid == null) {
            player.sendMessage(ChatColor.RED + "磁盘数据无效！");
            return;
        }

        // 获取磁盘数据
        List<DiskItem> diskData = diskManager.getDiskData(diskUuid);

        // 格式化并显示磁盘内容
        String contents = diskManager.formatDiskContents(diskUuid, diskData);
        player.sendMessage(contents);
    }

    /**
     * 以 GUI 形式查看磁盘内容
     * /netdebug disktest checkgui
     */
    private void handleCheckGUI(Player player) {
        ItemStack diskItem = player.getInventory().getItemInMainHand();
        DiskManager diskManager = plugin.getDiskManager();

        // 检查是否手持磁盘
        if (!diskManager.isDisk(diskItem)) {
            player.sendMessage(ChatColor.RED + "请手持磁盘！");
            return;
        }

        // 获取磁盘 UUID
        UUID diskUuid = diskManager.getDiskUuidFromItem(diskItem);
        if (diskUuid == null) {
            player.sendMessage(ChatColor.RED + "磁盘数据无效！");
            return;
        }

        // 获取磁盘数据
        List<DiskItem> diskData = diskManager.getDiskData(diskUuid);

        // 打开 GUI
        DiskGUI gui = new DiskGUI(player, plugin, diskItem, diskUuid, diskData);
        plugin.getGuiManager().registerGUI(player, gui);
        gui.open();
    }

    /**
     * 发送帮助信息
     */
    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GREEN + "===== 磁盘测试命令 =====");
        player.sendMessage(ChatColor.YELLOW + "/netdebug disktest add <物品名称> [数量]" + 
                ChatColor.GRAY + " - 添加物品到磁盘（默认1个）");
        player.sendMessage(ChatColor.YELLOW + "/netdebug disktest remove <物品名称> [数量]" + 
                ChatColor.GRAY + " - 从磁盘移除物品（默认1个）");
        player.sendMessage(ChatColor.YELLOW + "/netdebug disktest clean" + 
                ChatColor.GRAY + " - 清空磁盘");
        player.sendMessage(ChatColor.YELLOW + "/netdebug disktest check" + 
                ChatColor.GRAY + " - 查看磁盘内容（文本）");
        player.sendMessage(ChatColor.YELLOW + "/netdebug disktest checkgui" + 
                ChatColor.GRAY + " - 查看磁盘内容（GUI）");
    }
}