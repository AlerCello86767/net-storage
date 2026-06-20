package com.AlerCello86767.net_storage.utils;

import com.AlerCello86767.net_storage.Net_storage;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final Net_storage plugin;
    private FileConfiguration config;

    public ConfigManager(Net_storage plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
        plugin.getLogger().info("配置文件加载完成！");
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }
}