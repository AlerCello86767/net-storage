package com.AlerCello86767.net_storage.commands;

import com.AlerCello86767.net_storage.Net_storage;
import com.AlerCello86767.net_storage.network.StorageNetwork;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandTabCompleter implements TabCompleter {

    private static final List<String> COMMANDS = Arrays.asList(
            "create", "list", "mynetworks", "info", "items",
            "delete", "rename", "setpublic", "setdesc",
            "addnode", "removenode", "help"
    );

    private final Net_storage plugin;

    public CommandTabCompleter(Net_storage plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            for (String cmd : COMMANDS) {
                if (!cmd.toLowerCase().startsWith(args[0].toLowerCase())) {
                    continue;
                }
                if (sender instanceof Player player) {
                    if (cmd.equals("create") && !player.hasPermission("netstorage.create")) continue;
                    if ((cmd.equals("delete") || cmd.equals("setpublic") || cmd.equals("setdesc"))
                            && !player.hasPermission("netstorage.delete") && !player.hasPermission("netstorage.rename")) continue;
                    if (cmd.equals("rename") && !player.hasPermission("netstorage.rename")) continue;
                    if ((cmd.equals("addnode") || cmd.equals("removenode")) && !player.hasPermission("netstorage.admin")) continue;
                }
                completions.add(cmd);
            }
        } else if (args.length == 2) {
            String cmd = args[0].toLowerCase();
            if (cmd.equals("info") || cmd.equals("items") || cmd.equals("delete") ||
                    cmd.equals("rename") || cmd.equals("setpublic") || cmd.equals("setdesc")) {
                // 只显示玩家有权限访问的网络
                if (sender instanceof Player player) {
                    for (StorageNetwork network : plugin.getNetworkManager().getNetworks().values()) {
                        String name = network.getName();
                        if (name.toLowerCase().startsWith(args[1].toLowerCase())) {
                            // 检查权限：公开网络、创建者、或管理员
                            if (network.isPublic() || network.hasManagePermission(player) ||
                                    player.hasPermission("netstorage.admin")) {
                                completions.add(name);
                            }
                        }
                    }
                }
            }
        } else if (args.length == 3) {
            String cmd = args[0].toLowerCase();
            if (cmd.equals("delete")) {
                completions.add("confirm");
            } else if (cmd.equals("setpublic")) {
                if (args[2].toLowerCase().startsWith("t")) completions.add("true");
                if (args[2].toLowerCase().startsWith("f")) completions.add("false");
            } else if (cmd.equals("rename")) {
                completions.add("<新名称>");
            } else if (cmd.equals("setdesc")) {
                completions.add("<描述>");
            }
        }

        return completions;
    }
}