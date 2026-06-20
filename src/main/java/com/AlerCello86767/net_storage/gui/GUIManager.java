package com.AlerCello86767.net_storage.gui;

import com.AlerCello86767.net_storage.Net_storage;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * GUI管理器 - 管理所有打开的GUI
 */
public class GUIManager {

    private final Net_storage plugin;
    private final Map<UUID, BaseGUI> openGUIs = new HashMap<>();

    public GUIManager(Net_storage plugin) {
        this.plugin = plugin;
    }

    /**
     * 注册GUI
     */
    public void registerGUI(Player player, BaseGUI gui) {
        openGUIs.put(player.getUniqueId(), gui);
    }

    /**
     * 取消注册GUI
     */
    public void unregisterGUI(Player player) {
        openGUIs.remove(player.getUniqueId());
    }

    /**
     * 获取玩家当前打开的GUI
     */
    public BaseGUI getGUI(Player player) {
        return openGUIs.get(player.getUniqueId());
    }

    /**
     * 获取玩家当前打开的GUI（通过UUID）
     */
    public BaseGUI getGUI(UUID uuid) {
        return openGUIs.get(uuid);
    }

    /**
     * 检查玩家是否打开了GUI
     */
    public boolean hasGUI(Player player) {
        return openGUIs.containsKey(player.getUniqueId());
    }

    /**
     * 关闭所有GUI
     */
    public void closeAll() {
        for (UUID uuid : openGUIs.keySet()) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                BaseGUI gui = openGUIs.get(uuid);
                if (gui != null) {
                    gui.close();
                }
            }
        }
        openGUIs.clear();
    }

    /**
     * 获取所有打开的GUI
     */
    public Map<UUID, BaseGUI> getOpenGUIs() {
        return openGUIs;
    }
}