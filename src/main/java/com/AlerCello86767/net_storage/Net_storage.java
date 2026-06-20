package com.AlerCello86767.net_storage;

import com.AlerCello86767.net_storage.commands.CommandTabCompleter;
import com.AlerCello86767.net_storage.commands.ControllerCommand;
import com.AlerCello86767.net_storage.commands.DebugTabCompleter;
import com.AlerCello86767.net_storage.commands.DiskTestCommand;
import com.AlerCello86767.net_storage.commands.DiskTestTabCompleter;
import com.AlerCello86767.net_storage.commands.NetworkCommand;
import com.AlerCello86767.net_storage.commands.TestGUICommand;
import com.AlerCello86767.net_storage.controller.ConnectToolListener;
import com.AlerCello86767.net_storage.controller.ControllerListener;
import com.AlerCello86767.net_storage.controller.ControllerManager;
import com.AlerCello86767.net_storage.disk.DiskManager;
import com.AlerCello86767.net_storage.gui.GUIManager;
import com.AlerCello86767.net_storage.gui.listener.GUIListener;
import com.AlerCello86767.net_storage.listeners.PlayerInteractListener;
import com.AlerCello86767.net_storage.network.NetworkManager;
import com.AlerCello86767.net_storage.task.InputBusTask;
import com.AlerCello86767.net_storage.utils.ConfigManager;
import com.AlerCello86767.net_storage.utils.DatabaseManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class Net_storage extends JavaPlugin {

    private static Net_storage instance;
    private NetworkManager networkManager;
    private DatabaseManager databaseManager;
    private ConfigManager configManager;
    private GUIManager guiManager;
    private ControllerManager controllerManager;
    private DiskManager diskManager;
    private BukkitTask autoSaveTask;
    private BukkitTask inputBusTask;

    @Override
    public void onEnable() {
        instance = this;

        configManager = new ConfigManager(this);
        configManager.loadConfig();

        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();

        networkManager = new NetworkManager(this);
        networkManager.loadAllNetworks();

        controllerManager = new ControllerManager(this);
        controllerManager.loadAllControllers();
        controllerManager.loadAllDebugDevices();
        controllerManager.loadAllDiskManipulators();
        controllerManager.loadAllTerminals();
        controllerManager.loadAllExternalStorageBuses();
        controllerManager.loadAllInputBuses();

        diskManager = new DiskManager(this);

        guiManager = new GUIManager(this);

        getCommand("network").setExecutor(new NetworkCommand(this));
        getCommand("network").setTabCompleter(new CommandTabCompleter(this));
        getCommand("testgui").setExecutor(new TestGUICommand());
        getCommand("netdebug").setExecutor(new ControllerCommand(this));
        getCommand("netdebug").setTabCompleter(new DebugTabCompleter());
        getCommand("disktest").setExecutor(new DiskTestCommand(this));
        getCommand("disktest").setTabCompleter(new DiskTestTabCompleter());

        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new ControllerListener(this), this);
        getServer().getPluginManager().registerEvents(new ConnectToolListener(this), this);

        startAutoSaveTask();
        controllerManager.startActionBarTask();
        startInputBusTask();

        getLogger().info("NetStorage 插件已启用！");
        getLogger().info("使用 /network help 查看帮助");
        getLogger().info("使用 /testgui 打开测试界面");
        getLogger().info("使用 /netdebug give controller 获取网络控制器");
    }

    @Override
    public void onDisable() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }

        if (inputBusTask != null) {
            inputBusTask.cancel();
        }

        if (guiManager != null) {
            guiManager.closeAll();
        }

        if (networkManager != null) {
            networkManager.forceSaveAllNetworks();
        }

        if (diskManager != null) {
            diskManager.saveAllDiskData();
        }

        if (controllerManager != null) {
            controllerManager.stopActionBarTask();
            controllerManager.saveAllControllers();
            controllerManager.saveAllDebugDevices();
            controllerManager.saveAllDiskManipulators();
            controllerManager.saveAllTerminals();
            controllerManager.saveAllExternalStorageBuses();
            controllerManager.saveAllInputBuses();
        }

        if (databaseManager != null) {
            databaseManager.close();
        }

        getLogger().info("NetStorage 插件已禁用！");
    }

    private void startAutoSaveTask() {
        int saveInterval = configManager.getConfig().getInt("performance.save-interval", 300);
        long delay = saveInterval * 20L;
        long period = saveInterval * 20L;

        autoSaveTask = getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            getLogger().info("自动保存数据...");
            networkManager.saveAllNetworks();
            diskManager.saveAllDiskData();
        }, delay, period);

        getLogger().info("自动保存任务已启动，间隔: " + saveInterval + " 秒");
    }

    private void startInputBusTask() {
        // 每9 ticks执行一次输入总线物品提取（约每秒4.5次）
        inputBusTask = getServer().getScheduler().runTaskTimer(this, new InputBusTask(this), 1L, 9L);
        getLogger().info("输入总线定时任务已启动");
    }

    public static Net_storage getInstance() {
        return instance;
    }

    public NetworkManager getNetworkManager() {
        return networkManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public GUIManager getGuiManager() {
        return guiManager;
    }

    public ControllerManager getControllerManager() {
        return controllerManager;
    }

    public DiskManager getDiskManager() {
        return diskManager;
    }
}