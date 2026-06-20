package com.AlerCello86767.net_storage.listeners;

import com.AlerCello86767.net_storage.Net_storage;
import com.AlerCello86767.net_storage.disk.DiskManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerInteractListener implements Listener {

    private final Net_storage plugin;

    public PlayerInteractListener(Net_storage plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // 检查是否右键点击
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR &&
            event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        DiskManager diskManager = plugin.getDiskManager();

        // 检查主手物品
        ItemStack mainHand = event.getPlayer().getInventory().getItemInMainHand();
        if (diskManager.isDisk(mainHand)) {
            event.setCancelled(true);
            return;
        }

        // 检查副手物品
        ItemStack offHand = event.getPlayer().getInventory().getItemInOffHand();
        if (diskManager.isDisk(offHand)) {
            event.setCancelled(true);
            return;
        }

        // TODO: 实现其他玩家交互逻辑（连接方块到网络等）
    }
}