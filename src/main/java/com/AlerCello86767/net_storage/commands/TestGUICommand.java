package com.AlerCello86767.net_storage.commands;

import com.AlerCello86767.net_storage.Net_storage;
import com.AlerCello86767.net_storage.gui.TestGUI;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TestGUICommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以执行此命令！");
            return true;
        }

        // 打开测试GUI
        TestGUI gui = new TestGUI(player);
        Net_storage.getInstance().getGuiManager().registerGUI(player, gui);
        gui.open();

        return true;
    }
}