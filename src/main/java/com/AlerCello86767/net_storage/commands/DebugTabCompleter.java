package com.AlerCello86767.net_storage.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DebugTabCompleter implements TabCompleter {

    private static final List<String> SUB_COMMANDS = Arrays.asList("give");
    private static final List<String> ITEM_TYPES = Arrays.asList(
            "controller", "connect_tool", "debug_device", "disk_manipulator", "terminal", "external_storage_bus", "disk_1k"
    );

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player player)) {
            return completions;
        }

        if (!player.hasPermission("netstorage.admin")) {
            return completions;
        }

        if (args.length == 1) {
            for (String sub : SUB_COMMANDS) {
                if (sub.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("give")) {
                for (String type : ITEM_TYPES) {
                    if (type.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(type);
                    }
                }
            }
        } else if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("give")) {
                completions.add("1");
                completions.add("16");
                completions.add("32");
                completions.add("64");
            }
        }

        return completions;
    }
}