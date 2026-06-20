package com.AlerCello86767.net_storage.commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 磁盘测试命令 Tab 补全
 */
public class DiskTestTabCompleter implements TabCompleter {

    private static final List<String> SUB_COMMANDS = Arrays.asList("add", "remove", "clean", "check", "checkgui");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player)) {
            return completions;
        }

        Player player = (Player) sender;

        // 检查权限
        if (!player.hasPermission("netstorage.admin")) {
            return completions;
        }

        // 第一个参数：子命令
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            for (String subCmd : SUB_COMMANDS) {
                if (subCmd.startsWith(prefix)) {
                    completions.add(subCmd);
                }
            }
            return completions;
        }

        // 第二个参数：物品名称（仅对 add 和 remove 命令）
        if (args.length == 2) {
            String subCmd = args[0].toLowerCase();
            if (subCmd.equals("add") || subCmd.equals("remove")) {
                String prefix = args[1].toUpperCase();
                // 返回匹配的 Material 名称
                for (Material material : Material.values()) {
                    if (material.isItem() && material.name().startsWith(prefix)) {
                        completions.add(material.name().toLowerCase());
                    }
                }
            }
            return completions;
        }

        // 第三个参数：数量（仅对 add 和 remove 命令）
        if (args.length == 3) {
            String subCmd = args[0].toLowerCase();
            if (subCmd.equals("add") || subCmd.equals("remove")) {
                // 补全常用数量
                completions.add("1");
                completions.add("16");
                completions.add("32");
                completions.add("64");
            }
            return completions;
        }

        return completions;
    }
}