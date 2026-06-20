package com.AlerCello86767.net_storage.commands;

import com.AlerCello86767.net_storage.Net_storage;
import com.AlerCello86767.net_storage.network.StorageNetwork;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NetworkCommand implements CommandExecutor {

    private final Net_storage plugin;

    public NetworkCommand(Net_storage plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以执行此命令！");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                handleCreate(player, args);
                break;

            case "list":
                handleList(player, args);
                break;

            case "info":
                handleInfo(player, args);
                break;

            case "delete":
                handleDelete(player, args);
                break;

            case "rename":
                handleRename(player, args);
                break;

            case "setpublic":
                handleSetPublic(player, args);
                break;

            case "setdesc":
                handleSetDescription(player, args);
                break;

            case "mynetworks":
                handleMyNetworks(player);
                break;

            case "addnode":
                handleAddNode(player);
                break;

            case "removenode":
                handleRemoveNode(player);
                break;

            case "help":
            default:
                sendHelp(player);
                break;
        }
        return true;
    }

    // ========== 权限检查 ==========

    private boolean hasPermission(Player player, String permission) {
        if (!player.hasPermission(permission)) {
            player.sendMessage(ChatColor.RED + "你没有权限执行此命令！");
            player.sendMessage(ChatColor.GRAY + "需要权限: " + permission);
            return false;
        }
        return true;
    }

    // ========== 命令处理方法 ==========

    /**
     * 创建网络
     * /network create <名称>
     */
    private void handleCreate(Player player, String[] args) {
        if (!hasPermission(player, "netstorage.create")) {
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /network create <名称>");
            return;
        }

        String name = args[1];

        if (!name.matches("^[a-zA-Z0-9_\\u4e00-\\u9fa5]{1,32}$")) {
            player.sendMessage(ChatColor.RED + "网络名称只能包含字母、数字、下划线，长度1-32个字符");
            return;
        }

        // 检查是否已存在同名网络
        if (plugin.getNetworkManager().getNetworkByName(name) != null) {
            player.sendMessage(ChatColor.RED + "已存在同名网络: " + name);
            return;
        }

        StorageNetwork network = plugin.getNetworkManager().createNetwork(name, player);

        if (network == null) {
            player.sendMessage(ChatColor.RED + "创建失败！你可能已达到网络数量上限");
            return;
        }

        player.sendMessage(ChatColor.GREEN + "✅ 已创建网络: " + ChatColor.GOLD + network.getName());
        player.sendMessage(ChatColor.GRAY + "网络ID: " + network.getNetworkId().toString().substring(0, 8) + "...");
        player.sendMessage(ChatColor.GRAY + "创建者: " + network.getCreatorName());
        player.sendMessage(ChatColor.GRAY + "使用 /network info " + network.getName() + " 查看详情");
    }

    /**
     * 列出所有网络
     * /network list [page]
     */
    private void handleList(Player player, String[] args) {
        if (!hasPermission(player, "netstorage.use")) {
            return;
        }

        Map<UUID, StorageNetwork> networks = plugin.getNetworkManager().getNetworks();

        if (networks.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "⚠ 还没有创建任何网络");
            player.sendMessage(ChatColor.GRAY + "使用 /network create <名称> 创建");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "=== 所有网络列表 (" + networks.size() + ") ===");
        int index = 1;
        for (StorageNetwork net : networks.values()) {
            int nodeCount = net.getNodeCount();
            String creator = net.getCreatorName();
            String publicStatus = net.isPublic() ? ChatColor.GREEN + "公开" : ChatColor.GRAY + "私有";

            player.sendMessage(ChatColor.YELLOW + "" + index + ". " + ChatColor.WHITE + net.getName() +
                    ChatColor.GRAY + " | 创建者: " + ChatColor.AQUA + creator +
                    ChatColor.GRAY + " | 节点: " + ChatColor.AQUA + nodeCount +
                    ChatColor.GRAY + " | " + publicStatus);
            index++;
        }
    }

    /**
     * 查看网络详情
     * /network info <名称>
     */
    private void handleInfo(Player player, String[] args) {
        if (!hasPermission(player, "netstorage.use")) {
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "请指定网络名称: /network info <名称>");
            player.sendMessage(ChatColor.GRAY + "使用 /network list 查看所有网络");
            return;
        }

        StorageNetwork network = plugin.getNetworkManager().getNetworkByName(args[1]);
        if (network == null) {
            player.sendMessage(ChatColor.RED + "找不到网络: " + args[1]);
            return;
        }

        // 检查是否有权限查看（创建者、OP、或公开网络）
        if (!network.isPublic() && !network.hasManagePermission(player) && !player.hasPermission("netstorage.admin")) {
            player.sendMessage(ChatColor.RED + "此网络为私有网络，你没有权限查看！");
            return;
        }

        // 显示详细信息
        player.sendMessage(ChatColor.GOLD + "=== " + network.getName() + " 详情 ===");
        player.sendMessage(ChatColor.YELLOW + "网络ID: " + ChatColor.WHITE + network.getNetworkId().toString().substring(0, 8) + "...");
        player.sendMessage(ChatColor.YELLOW + "创建者: " + ChatColor.AQUA + network.getCreatorName());
        player.sendMessage(ChatColor.YELLOW + "创建时间: " + ChatColor.WHITE + network.getFormattedCreatedTime());
        player.sendMessage(ChatColor.YELLOW + "最后修改: " + ChatColor.WHITE + network.getFormattedLastModifiedTime());
        player.sendMessage(ChatColor.YELLOW + "状态: " + (network.isPublic() ? ChatColor.GREEN + "公开" : ChatColor.RED + "私有"));
        if (!network.getDescription().isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "描述: " + ChatColor.WHITE + network.getDescription());
        }
        player.sendMessage(ChatColor.YELLOW + "节点数: " + ChatColor.AQUA + network.getNodeCount());
        player.sendMessage(ChatColor.GRAY + "提示: 物品存储在磁盘操纵器的磁盘中，请使用终端查看");
    }

    /**
     * 删除网络（需要确认）
     * /network delete <名称> [confirm]
     */
    private void handleDelete(Player player, String[] args) {
        if (!hasPermission(player, "netstorage.delete")) {
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /network delete <名称>");
            player.sendMessage(ChatColor.RED + "警告: 这将永久删除网络及其所有数据！");
            player.sendMessage(ChatColor.GRAY + "确认请使用: /network delete <名称> confirm");
            return;
        }

        StorageNetwork network = plugin.getNetworkManager().getNetworkByName(args[1]);
        if (network == null) {
            player.sendMessage(ChatColor.RED + "找不到网络: " + args[1]);
            return;
        }

        // 检查权限：只有创建者或OP可以删除
        if (!network.hasManagePermission(player) && !player.hasPermission("netstorage.admin")) {
            player.sendMessage(ChatColor.RED + "只有网络创建者或管理员可以删除此网络！");
            return;
        }

        boolean confirmed = args.length >= 3 && args[2].equalsIgnoreCase("confirm");

        if (!confirmed) {
            player.sendMessage(ChatColor.RED + "⚠ 警告：你正在删除网络 \"" + network.getName() + "\"");
            player.sendMessage(ChatColor.RED + "此操作不可恢复！");
            player.sendMessage(ChatColor.YELLOW + "节点数: " + ChatColor.AQUA + network.getNodeCount());
            player.sendMessage(ChatColor.GRAY + "确认请使用: /network delete " + args[1] + " confirm");
            return;
        }

        plugin.getNetworkManager().deleteNetwork(network.getNetworkId());
        player.sendMessage(ChatColor.RED + "🗑 已删除网络: " + ChatColor.GOLD + args[1]);
    }

    /**
     * 重命名网络
     * /network rename <旧名称> <新名称>
     */
    private void handleRename(Player player, String[] args) {
        if (!hasPermission(player, "netstorage.rename")) {
            return;
        }

        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "用法: /network rename <旧名称> <新名称>");
            return;
        }

        StorageNetwork network = plugin.getNetworkManager().getNetworkByName(args[1]);
        if (network == null) {
            player.sendMessage(ChatColor.RED + "找不到网络: " + args[1]);
            return;
        }

        // 检查权限：只有创建者可以重命名
        if (!network.hasManagePermission(player) && !player.hasPermission("netstorage.admin")) {
            player.sendMessage(ChatColor.RED + "只有网络创建者可以重命名此网络！");
            return;
        }

        String newName = args[2];
        if (!newName.matches("^[a-zA-Z0-9_\\u4e00-\\u9fa5]{1,32}$")) {
            player.sendMessage(ChatColor.RED + "网络名称只能包含字母、数字、下划线，长度1-32个字符");
            return;
        }

        if (plugin.getNetworkManager().getNetworkByName(newName) != null) {
            player.sendMessage(ChatColor.RED + "已存在同名网络: " + newName);
            return;
        }

        String oldName = network.getName();
        network.setName(newName);
        plugin.getNetworkManager().saveNetworkAsync(network);
        player.sendMessage(ChatColor.GREEN + "✅ 已重命名: " + ChatColor.GOLD + oldName +
                ChatColor.GREEN + " → " + ChatColor.GOLD + newName);
    }

    /**
     * 设置网络公开/私有
     * /network setpublic <名称> <true/false>
     */
    private void handleSetPublic(Player player, String[] args) {
        if (!hasPermission(player, "netstorage.rename")) {
            return;
        }

        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "用法: /network setpublic <名称> <true/false>");
            return;
        }

        StorageNetwork network = plugin.getNetworkManager().getNetworkByName(args[1]);
        if (network == null) {
            player.sendMessage(ChatColor.RED + "找不到网络: " + args[1]);
            return;
        }

        if (!network.hasManagePermission(player) && !player.hasPermission("netstorage.admin")) {
            player.sendMessage(ChatColor.RED + "只有网络创建者可以修改此设置！");
            return;
        }

        boolean isPublic = Boolean.parseBoolean(args[2]);
        network.setPublic(isPublic);
        plugin.getNetworkManager().saveNetworkAsync(network);
        player.sendMessage(ChatColor.GREEN + "✅ 网络 " + network.getName() +
                " 已设置为: " + (isPublic ? ChatColor.GREEN + "公开" : ChatColor.RED + "私有"));
    }

    /**
     * 设置网络描述
     * /network setdesc <名称> <描述>
     */
    private void handleSetDescription(Player player, String[] args) {
        if (!hasPermission(player, "netstorage.rename")) {
            return;
        }

        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "用法: /network setdesc <名称> <描述>");
            player.sendMessage(ChatColor.GRAY + "描述最长50个字符");
            return;
        }

        StorageNetwork network = plugin.getNetworkManager().getNetworkByName(args[1]);
        if (network == null) {
            player.sendMessage(ChatColor.RED + "找不到网络: " + args[1]);
            return;
        }

        if (!network.hasManagePermission(player) && !player.hasPermission("netstorage.admin")) {
            player.sendMessage(ChatColor.RED + "只有网络创建者可以修改此设置！");
            return;
        }

        // 拼接描述（支持空格）
        StringBuilder desc = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            if (desc.length() > 0) desc.append(" ");
            desc.append(args[i]);
        }

        String description = desc.toString();
        if (description.length() > 50) {
            player.sendMessage(ChatColor.RED + "描述太长！最大50个字符");
            return;
        }

        network.setDescription(description);
        plugin.getNetworkManager().saveNetworkAsync(network);
        player.sendMessage(ChatColor.GREEN + "✅ 已更新网络描述: " + ChatColor.WHITE + description);
    }

    /**
     * 查看我的网络
     * /network mynetworks
     */
    private void handleMyNetworks(Player player) {
        if (!hasPermission(player, "netstorage.use")) {
            return;
        }

        List<StorageNetwork> myNetworks = plugin.getNetworkManager().getPlayerNetworks(player);

        if (myNetworks.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "你还没有创建任何网络");
            player.sendMessage(ChatColor.GRAY + "使用 /network create <名称> 创建第一个网络");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "=== 我的网络 (" + myNetworks.size() + ") ===");
        int index = 1;
        for (StorageNetwork net : myNetworks) {
            int nodeCount = net.getNodeCount();
            String publicStatus = net.isPublic() ? ChatColor.GREEN + "公开" : ChatColor.GRAY + "私有";

            player.sendMessage(ChatColor.YELLOW + "" + index + ". " + ChatColor.WHITE + net.getName() +
                    ChatColor.GRAY + " | 节点: " + ChatColor.AQUA + nodeCount +
                    ChatColor.GRAY + " | " + publicStatus);
            index++;
        }
    }

    /**
     * 添加节点（占位）
     */
    private void handleAddNode(Player player) {
        if (!hasPermission(player, "netstorage.admin")) {
            return;
        }
        player.sendMessage(ChatColor.YELLOW + "⚠ 此功能正在开发中...");
    }

    /**
     * 移除节点（占位）
     */
    private void handleRemoveNode(Player player) {
        if (!hasPermission(player, "netstorage.admin")) {
            return;
        }
        player.sendMessage(ChatColor.YELLOW + "⚠ 此功能正在开发中...");
    }

    // ========== 帮助 ==========

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== NetStorage 命令帮助 ===");
        player.sendMessage(ChatColor.GRAY + "使用 /network <命令> [参数]");
        player.sendMessage("");

        player.sendMessage(ChatColor.GRAY + "--- 基础命令 ---");
        player.sendMessage(ChatColor.YELLOW + "/network list" + ChatColor.WHITE + " - 列出所有网络");
        player.sendMessage(ChatColor.YELLOW + "/network mynetworks" + ChatColor.WHITE + " - 查看我创建的网络");
        player.sendMessage(ChatColor.YELLOW + "/network info <名称>" + ChatColor.WHITE + " - 查看网络详情");
        player.sendMessage(ChatColor.YELLOW + "/network help" + ChatColor.WHITE + " - 显示此帮助");
        player.sendMessage("");

        player.sendMessage(ChatColor.GRAY + "--- 管理命令 ---");
        if (player.hasPermission("netstorage.create")) {
            player.sendMessage(ChatColor.YELLOW + "/network create <名称>" + ChatColor.WHITE + " - 创建新网络");
        }
        if (player.hasPermission("netstorage.delete")) {
            player.sendMessage(ChatColor.YELLOW + "/network delete <名称> [confirm]" + ChatColor.WHITE + " - 删除网络");
        }
        if (player.hasPermission("netstorage.rename")) {
            player.sendMessage(ChatColor.YELLOW + "/network rename <旧名> <新名>" + ChatColor.WHITE + " - 重命名网络");
            player.sendMessage(ChatColor.YELLOW + "/network setpublic <名称> <true/false>" + ChatColor.WHITE + " - 设置公开/私有");
            player.sendMessage(ChatColor.YELLOW + "/network setdesc <名称> <描述>" + ChatColor.WHITE + " - 设置网络描述");
        }

        player.sendMessage("");
        player.sendMessage(ChatColor.GRAY + "--- 示例 ---");
        player.sendMessage(ChatColor.GRAY + "/network create main");
        player.sendMessage(ChatColor.GRAY + "/network setpublic main true");
        player.sendMessage(ChatColor.GRAY + "/network setdesc main 我的存储网络");
        player.sendMessage(ChatColor.GRAY + "/network delete main confirm");
    }
}