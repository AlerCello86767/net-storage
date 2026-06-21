package com.AlerCello86767.net_storage.utils;

import com.AlerCello86767.net_storage.Net_storage;
import com.AlerCello86767.net_storage.network.StorageNetwork;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DatabaseManager {

    private final Net_storage plugin;
    private Connection connection;

    public DatabaseManager(Net_storage plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        try {
            Class.forName("org.h2.Driver");
            
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            
            String dbPath = plugin.getDataFolder().getAbsolutePath() + "/storage";
            plugin.getLogger().info("数据库文件路径: " + dbPath);
            
            String url = "jdbc:h2:" + dbPath + ";DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL";
            connection = DriverManager.getConnection(url, "sa", "");
            
            createTables();
            migrateDisksTable();
            migrateInputBusesTable();
            plugin.getLogger().info("数据库初始化成功！");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("H2 数据库驱动未找到: " + e.getMessage());
            e.printStackTrace();
        } catch (SQLException e) {
            plugin.getLogger().severe("数据库连接失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createTables() throws SQLException {
        String createNetworks = """
            CREATE TABLE IF NOT EXISTS networks (
                network_id VARCHAR(36) PRIMARY KEY,
                network_name VARCHAR(64) NOT NULL,
                creator_uuid VARCHAR(36) NOT NULL,
                creator_name VARCHAR(64) NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                is_public BOOLEAN DEFAULT FALSE,
                description VARCHAR(256) DEFAULT ''
            )
        """;

        String createControllers = """
            CREATE TABLE IF NOT EXISTS controllers (
                id INTEGER PRIMARY KEY AUTO_INCREMENT,
                location VARCHAR(128) NOT NULL UNIQUE,
                network_id VARCHAR(36) NOT NULL,
                owner_uuid VARCHAR(36) NOT NULL,
                placed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (network_id) REFERENCES networks(network_id) ON DELETE CASCADE
            )
        """;

        String createDebugDevices = """
            CREATE TABLE IF NOT EXISTS debug_devices (
                id INTEGER PRIMARY KEY AUTO_INCREMENT,
                location VARCHAR(128) NOT NULL UNIQUE,
                network_id VARCHAR(36),
                placed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;

        String createDisks = """
            CREATE TABLE IF NOT EXISTS disks (
                disk_uuid VARCHAR(36) PRIMARY KEY,
                item_data TEXT,
                disk_type VARCHAR(16) DEFAULT 'disk_1k',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        
        String createDiskManipulators = """
            CREATE TABLE IF NOT EXISTS disk_manipulators (
                id INTEGER PRIMARY KEY AUTO_INCREMENT,
                location VARCHAR(128) NOT NULL UNIQUE,
                network_id VARCHAR(36),
                slot_1 VARCHAR(36),
                slot_2 VARCHAR(36),
                slot_3 VARCHAR(36),
                slot_4 VARCHAR(36),
                slot_5 VARCHAR(36),
                slot_6 VARCHAR(36),
                slot_7 VARCHAR(36),
                slot_8 VARCHAR(36),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        
        String createTerminals = """
            CREATE TABLE IF NOT EXISTS terminals (
                id INTEGER PRIMARY KEY AUTO_INCREMENT,
                location VARCHAR(128) NOT NULL UNIQUE,
                network_id VARCHAR(36),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        
        String createExternalStorageBuses = """
            CREATE TABLE IF NOT EXISTS external_storage_buses (
                id INTEGER PRIMARY KEY AUTO_INCREMENT,
                bus_uuid VARCHAR(36) UNIQUE NOT NULL,
                location VARCHAR(128) NOT NULL UNIQUE,
                network_id VARCHAR(36),
                container_location VARCHAR(128) NOT NULL,
                container_type VARCHAR(64),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        
        String createInputBuses = """
            CREATE TABLE IF NOT EXISTS input_buses (
                id INTEGER PRIMARY KEY AUTO_INCREMENT,
                bus_uuid VARCHAR(36) UNIQUE NOT NULL,
                location VARCHAR(128) NOT NULL UNIQUE,
                network_id VARCHAR(36),
                container_location VARCHAR(128) NOT NULL,
                container_type VARCHAR(64),
                filter_items TEXT,
                whitelist_mode BOOLEAN DEFAULT TRUE,
                nbt_matching BOOLEAN DEFAULT FALSE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;

        String createOutputBuses = """
            CREATE TABLE IF NOT EXISTS output_buses (
                id INTEGER PRIMARY KEY AUTO_INCREMENT,
                bus_uuid VARCHAR(36) UNIQUE NOT NULL,
                location VARCHAR(128) NOT NULL UNIQUE,
                network_id VARCHAR(36),
                container_location VARCHAR(128) NOT NULL,
                container_type VARCHAR(64),
                filter_items TEXT,
                nbt_matching BOOLEAN DEFAULT FALSE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createNetworks);
            stmt.execute(createControllers);
            stmt.execute(createDebugDevices);
            stmt.execute(createDisks);
            stmt.execute(createDiskManipulators);
            stmt.execute(createTerminals);
            stmt.execute(createExternalStorageBuses);
            stmt.execute(createInputBuses);
            stmt.execute(createOutputBuses);
        }
    }
    
    public void migrateDisksTable() {
        if (connection == null) return;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE disks ADD COLUMN IF NOT EXISTS disk_type VARCHAR(16) DEFAULT 'disk_1k'");
            
            String countSql = "SELECT COUNT(*) FROM disks WHERE disk_type IS NULL OR disk_type = ''";
            try (ResultSet rs = stmt.executeQuery(countSql)) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    if (count > 0) {
                        stmt.execute("UPDATE disks SET disk_type = 'disk_1k' WHERE disk_type IS NULL OR disk_type = ''");
                        plugin.getLogger().info("迁移了 " + count + " 个磁盘数据");
                    }
                }
            }
            
            plugin.getLogger().info("磁盘表迁移完成");
        } catch (SQLException e) {
            plugin.getLogger().warning("磁盘表迁移失败: " + e.getMessage());
        }
    }
    
    /**
     * 迁移输入总线表，添加过滤相关列
     */
    public void migrateInputBusesTable() {
        if (connection == null) return;
        
        try (Statement stmt = connection.createStatement()) {
            // 添加过滤相关列
            try {
                stmt.execute("ALTER TABLE input_buses ADD COLUMN IF NOT EXISTS filter_items TEXT");
            } catch (SQLException e) {
                // 列可能已存在，忽略
            }
            try {
                stmt.execute("ALTER TABLE input_buses ADD COLUMN IF NOT EXISTS whitelist_mode BOOLEAN DEFAULT TRUE");
            } catch (SQLException e) {
                // 列可能已存在，忽略
            }
            try {
                stmt.execute("ALTER TABLE input_buses ADD COLUMN IF NOT EXISTS nbt_matching BOOLEAN DEFAULT FALSE");
            } catch (SQLException e) {
                // 列可能已存在，忽略
            }
            
            plugin.getLogger().info("输入总线表迁移完成");
        } catch (SQLException e) {
            plugin.getLogger().warning("输入总线表迁移失败: " + e.getMessage());
        }
    }

    public void saveNetworkToDB(StorageNetwork network) {
        if (connection == null) return;

        try {
            String sql = """
                REPLACE INTO networks (network_id, network_name, creator_uuid, creator_name, 
                    created_at, last_modified_at, is_public, description)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, network.getNetworkId().toString());
                stmt.setString(2, network.getName());
                stmt.setString(3, network.getCreatorUUID().toString());
                stmt.setString(4, network.getCreatorName());
                stmt.setTimestamp(5, new Timestamp(network.getCreatedTime()));
                stmt.setTimestamp(6, new Timestamp(network.getLastModifiedTime()));
                stmt.setBoolean(7, network.isPublic());
                stmt.setString(8, network.getDescription());
                stmt.executeUpdate();
            }

            network.setDirty(false);
            plugin.getLogger().info("网络已保存: " + network.getName());
        } catch (SQLException e) {
            plugin.getLogger().severe("保存网络失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void deleteNetworkFromDB(UUID networkId) {
        if (connection == null) return;

        try {
            String sql = "DELETE FROM networks WHERE network_id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, networkId.toString());
                stmt.executeUpdate();
            }
            plugin.getLogger().info("网络已从数据库删除: " + networkId);
        } catch (SQLException e) {
            plugin.getLogger().severe("删除网络失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<StorageNetwork> loadAllNetworksFromDB() {
        List<StorageNetwork> networks = new ArrayList<>();
        if (connection == null) return networks;

        try {
            String sql = "SELECT * FROM networks";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    try {
                        UUID networkId = UUID.fromString(rs.getString("network_id"));
                        String name = rs.getString("network_name");
                        UUID creatorUUID = UUID.fromString(rs.getString("creator_uuid"));
                        String creatorName = rs.getString("creator_name");
                        long createdTime = rs.getTimestamp("created_at").getTime();
                        boolean isPublic = rs.getBoolean("is_public");
                        String description = rs.getString("description");

                        StorageNetwork network = new StorageNetwork(networkId, name, creatorUUID, creatorName, createdTime);
                        network.setPublic(isPublic);
                        network.setDescription(description);
                        network.setDirty(false);

                        networks.add(network);
                    } catch (Exception e) {
                        plugin.getLogger().warning("加载网络失败，跳过: " + e.getMessage());
                    }
                }
            }
            plugin.getLogger().info("从数据库加载了 " + networks.size() + " 个网络");
        } catch (SQLException e) {
            plugin.getLogger().severe("加载网络失败: " + e.getMessage());
            e.printStackTrace();
        }

        return networks;
    }

    public Connection getConnection() {
        return connection;
    }

    public void saveControllerToDB(String location, UUID networkId, UUID ownerUuid) {
        if (connection == null) return;

        try {
            String sql = """
                REPLACE INTO controllers (location, network_id, owner_uuid)
                VALUES (?, ?, ?)
            """;

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, location);
                stmt.setString(2, networkId.toString());
                stmt.setString(3, ownerUuid.toString());
                stmt.executeUpdate();
            }
            plugin.getLogger().info("控制器已保存: " + location);
        } catch (SQLException e) {
            plugin.getLogger().severe("保存控制器失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void deleteControllerFromDB(String location) {
        if (connection == null) return;

        try {
            String sql = "DELETE FROM controllers WHERE location = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, location);
                stmt.executeUpdate();
            }
            plugin.getLogger().info("控制器已从数据库删除: " + location);
        } catch (SQLException e) {
            plugin.getLogger().severe("删除控制器失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<com.AlerCello86767.net_storage.controller.ControllerManager.ControllerData> loadAllControllersFromDB() {
        List<com.AlerCello86767.net_storage.controller.ControllerManager.ControllerData> controllers = new ArrayList<>();
        if (connection == null) return controllers;

        try {
            String sql = "SELECT * FROM controllers";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    try {
                        String location = rs.getString("location");
                        String networkIdStr = rs.getString("network_id");
                        UUID networkId = (networkIdStr != null && !networkIdStr.isEmpty()) ? UUID.fromString(networkIdStr) : null;
                        String ownerUuidStr = rs.getString("owner_uuid");
                        UUID ownerUuid = (ownerUuidStr != null && !ownerUuidStr.isEmpty()) ? UUID.fromString(ownerUuidStr) : null;

                        if (location == null || location.isEmpty()) continue;

                        controllers.add(new com.AlerCello86767.net_storage.controller.ControllerManager.ControllerData(
                                location, networkId, ownerUuid));
                    } catch (Exception e) {
                        plugin.getLogger().warning("加载控制器失败，跳过: " + e.getMessage());
                    }
                }
            }
            plugin.getLogger().info("从数据库加载了 " + controllers.size() + " 个控制器");
        } catch (SQLException e) {
            plugin.getLogger().severe("加载控制器失败: " + e.getMessage());
            e.printStackTrace();
        }

        return controllers;
    }

    public void saveAllControllersToDB(Map<String, UUID> controllers) {
        if (connection == null) return;

        try {
            String deleteSql = "DELETE FROM controllers";
            try (PreparedStatement stmt = connection.prepareStatement(deleteSql)) {
                stmt.executeUpdate();
            }

            String insertSql = """
                INSERT INTO controllers (location, network_id, owner_uuid)
                VALUES (?, ?, ?)
            """;

            try (PreparedStatement stmt = connection.prepareStatement(insertSql)) {
                for (Map.Entry<String, UUID> entry : controllers.entrySet()) {
                    stmt.setString(1, entry.getKey());
                    stmt.setString(2, entry.getValue().toString());
                    stmt.setString(3, "");
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
            plugin.getLogger().info("所有控制器已保存，共 " + controllers.size() + " 个");
        } catch (SQLException e) {
            plugin.getLogger().severe("保存所有控制器失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ========== 调试子设备操作 ==========

    public void saveDebugDeviceToDB(String location, UUID networkId) {
        if (connection == null) return;

        try {
            String sql = """
                MERGE INTO debug_devices (location, network_id)
                KEY (location)
                VALUES (?, ?)
            """;

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, location);
                if (networkId != null) {
                    stmt.setString(2, networkId.toString());
                } else {
                    stmt.setNull(2, java.sql.Types.VARCHAR);
                }
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("保存调试子设备失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void deleteDebugDeviceFromDB(String location) {
        if (connection == null) return;

        try {
            String sql = "DELETE FROM debug_devices WHERE location = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, location);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("删除调试子设备失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<com.AlerCello86767.net_storage.controller.ControllerManager.DebugDeviceData> loadAllDebugDevicesFromDB() {
        List<com.AlerCello86767.net_storage.controller.ControllerManager.DebugDeviceData> devices = new ArrayList<>();
        if (connection == null) return devices;

        try {
            String sql = "SELECT location, network_id FROM debug_devices";
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    try {
                        String location = rs.getString("location");
                        String networkIdStr = rs.getString("network_id");
                        UUID networkId = (networkIdStr != null && !networkIdStr.isEmpty()) ? UUID.fromString(networkIdStr) : null;

                        devices.add(new com.AlerCello86767.net_storage.controller.ControllerManager.DebugDeviceData(
                                location, networkId));
                    } catch (Exception e) {
                        plugin.getLogger().warning("加载调试子设备失败，跳过: " + e.getMessage());
                    }
                }
            }
            plugin.getLogger().info("从数据库加载了 " + devices.size() + " 个调试子设备");
        } catch (SQLException e) {
            plugin.getLogger().severe("加载调试子设备失败: " + e.getMessage());
            e.printStackTrace();
        }

        return devices;
    }

    public void saveAllDebugDevicesToDB(java.util.Map<String, UUID> devices) {
        if (connection == null) return;

        try {
            String deleteSql = "DELETE FROM debug_devices";
            try (PreparedStatement stmt = connection.prepareStatement(deleteSql)) {
                stmt.executeUpdate();
            }

            String insertSql = """
                INSERT INTO debug_devices (location, network_id)
                VALUES (?, ?)
            """;

            try (PreparedStatement stmt = connection.prepareStatement(insertSql)) {
                for (java.util.Map.Entry<String, UUID> entry : devices.entrySet()) {
                    stmt.setString(1, entry.getKey());
                    if (entry.getValue() != null) {
                        stmt.setString(2, entry.getValue().toString());
                    } else {
                        stmt.setNull(2, java.sql.Types.VARCHAR);
                    }
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
            plugin.getLogger().info("所有调试子设备已保存，共 " + devices.size() + " 个");
        } catch (SQLException e) {
            plugin.getLogger().severe("保存所有调试子设备失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ========== 磁盘操作 ==========

    public void saveDiskToDB(UUID diskUuid, String itemData, String diskType) {
        if (connection == null) return;

        try {
            String sql = """
                MERGE INTO disks (disk_uuid, item_data, disk_type)
                KEY (disk_uuid)
                VALUES (?, ?, ?)
            """;

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, diskUuid.toString());
                stmt.setString(2, itemData);
                stmt.setString(3, diskType != null ? diskType : "disk_1k");
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("保存磁盘数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveDiskToDB(UUID diskUuid, String itemData) {
        saveDiskToDB(diskUuid, itemData, "disk_1k");
    }

    public String loadDiskFromDB(UUID diskUuid) {
        if (connection == null) return null;

        try {
            String sql = "SELECT item_data FROM disks WHERE disk_uuid = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, diskUuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("item_data");
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("加载磁盘数据失败: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    public String loadDiskTypeFromDB(UUID diskUuid) {
        if (connection == null) return "disk_1k";

        try {
            String sql = "SELECT disk_type FROM disks WHERE disk_uuid = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, diskUuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String type = rs.getString("disk_type");
                        return type != null ? type : "disk_1k";
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("加载磁盘类型失败: " + e.getMessage());
            e.printStackTrace();
        }

        return "disk_1k";
    }

    public void deleteDiskFromDB(UUID diskUuid) {
        if (connection == null) return;

        try {
            String sql = "DELETE FROM disks WHERE disk_uuid = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, diskUuid.toString());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("删除磁盘数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("关闭数据库连接时出错: " + e.getMessage());
        }
    }
    
    // ========== 磁盘操纵器操作 ==========
    
    public void saveDiskManipulatorToDB(com.AlerCello86767.net_storage.controller.DiskManipulatorData data) {
        if (connection == null || data == null) return;
        
        try {
            String sql = """
                MERGE INTO disk_manipulators (location, network_id, slot_1, slot_2, slot_3, slot_4, 
                    slot_5, slot_6, slot_7, slot_8)
                KEY (location)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, data.location);
                if (data.networkId != null) {
                    stmt.setString(2, data.networkId.toString());
                } else {
                    stmt.setNull(2, Types.VARCHAR);
                }
                for (int i = 0; i < 8; i++) {
                    if (data.slots[i] != null) {
                        stmt.setString(3 + i, data.slots[i].toString());
                    } else {
                        stmt.setNull(3 + i, Types.VARCHAR);
                    }
                }
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("保存磁盘操纵器失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void deleteDiskManipulatorFromDB(String location) {
        if (connection == null) return;
        
        try {
            String sql = "DELETE FROM disk_manipulators WHERE location = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, location);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("删除磁盘操纵器失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public java.util.List<com.AlerCello86767.net_storage.controller.DiskManipulatorData> loadAllDiskManipulatorsFromDB() {
        java.util.List<com.AlerCello86767.net_storage.controller.DiskManipulatorData> dataList = 
                new java.util.ArrayList<>();
        
        if (connection == null) return dataList;
        
        try {
            String sql = "SELECT * FROM disk_manipulators";
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    try {
                        com.AlerCello86767.net_storage.controller.DiskManipulatorData data = 
                                new com.AlerCello86767.net_storage.controller.DiskManipulatorData();
                        data.location = rs.getString("location");
                        
                        String networkIdStr = rs.getString("network_id");
                        data.networkId = (networkIdStr != null && !networkIdStr.isEmpty()) ? UUID.fromString(networkIdStr) : null;
                        
                        data.slots = new UUID[8];
                        for (int i = 0; i < 8; i++) {
                            String slotStr = rs.getString("slot_" + (i + 1));
                            data.slots[i] = slotStr != null ? UUID.fromString(slotStr) : null;
                        }
                        
                        data.createdAt = rs.getTimestamp("created_at");
                        dataList.add(data);
                    } catch (Exception e) {
                        plugin.getLogger().warning("加载磁盘操纵器失败，跳过: " + e.getMessage());
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("加载磁盘操纵器列表失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return dataList;
    }
    
    // ========== 终端操作 ==========
    
    public void saveTerminalToDB(com.AlerCello86767.net_storage.controller.TerminalData data) {
        if (connection == null || data == null) return;
        
        try {
            String sql = """
                MERGE INTO terminals (location, network_id)
                KEY (location)
                VALUES (?, ?)
            """;
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, data.location);
                if (data.networkId != null) {
                    stmt.setString(2, data.networkId.toString());
                } else {
                    stmt.setNull(2, Types.VARCHAR);
                }
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("保存终端失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void deleteTerminalFromDB(String location) {
        if (connection == null) return;
        
        try {
            String sql = "DELETE FROM terminals WHERE location = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, location);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("删除终端失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public java.util.List<com.AlerCello86767.net_storage.controller.TerminalData> loadAllTerminalsFromDB() {
        java.util.List<com.AlerCello86767.net_storage.controller.TerminalData> dataList = 
                new java.util.ArrayList<>();
        
        if (connection == null) return dataList;
        
        try {
            String sql = "SELECT * FROM terminals";
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    try {
                        com.AlerCello86767.net_storage.controller.TerminalData data = 
                                new com.AlerCello86767.net_storage.controller.TerminalData();
                        data.location = rs.getString("location");
                        
                        String networkIdStr = rs.getString("network_id");
                        data.networkId = (networkIdStr != null && !networkIdStr.isEmpty()) ? UUID.fromString(networkIdStr) : null;
                        
                        data.createdAt = rs.getTimestamp("created_at");
                        dataList.add(data);
                    } catch (Exception e) {
                        plugin.getLogger().warning("加载终端失败，跳过: " + e.getMessage());
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("加载终端列表失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return dataList;
    }
    
    // ========== 外部存储总线操作 ==========
    
    public void saveExternalStorageBusToDB(com.AlerCello86767.net_storage.controller.ExternalStorageBusData data) {
        if (connection == null || data == null) return;
        
        try {
            String sql = """
                MERGE INTO external_storage_buses (bus_uuid, location, network_id, container_location, container_type)
                KEY (location)
                VALUES (?, ?, ?, ?, ?)
            """;
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, data.busUuid.toString());
                stmt.setString(2, data.location);
                if (data.networkId != null) {
                    stmt.setString(3, data.networkId.toString());
                } else {
                    stmt.setNull(3, Types.VARCHAR);
                }
                stmt.setString(4, data.containerLocation);
                stmt.setString(5, data.containerType);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("保存外部存储总线失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void deleteExternalStorageBusFromDB(String location) {
        if (connection == null) return;
        
        try {
            String sql = "DELETE FROM external_storage_buses WHERE location = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, location);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("删除外部存储总线失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public java.util.List<com.AlerCello86767.net_storage.controller.ExternalStorageBusData> loadAllExternalStorageBusesFromDB() {
        java.util.List<com.AlerCello86767.net_storage.controller.ExternalStorageBusData> dataList = 
                new java.util.ArrayList<>();
        
        if (connection == null) return dataList;
        
        try {
            String sql = "SELECT * FROM external_storage_buses";
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    try {
                        String busUuidStr = rs.getString("bus_uuid");
                        UUID busUuid = busUuidStr != null ? UUID.fromString(busUuidStr) : UUID.randomUUID();
                        
                        String location = rs.getString("location");
                        String networkIdStr = rs.getString("network_id");
                        UUID networkId = (networkIdStr != null && !networkIdStr.isEmpty()) ? UUID.fromString(networkIdStr) : null;
                        String containerLocation = rs.getString("container_location");
                        String containerType = rs.getString("container_type");
                        
                        com.AlerCello86767.net_storage.controller.ExternalStorageBusData data = 
                                new com.AlerCello86767.net_storage.controller.ExternalStorageBusData(
                                        busUuid, location, networkId, containerLocation, containerType);
                        dataList.add(data);
                    } catch (Exception e) {
                        plugin.getLogger().warning("加载外部存储总线失败，跳过: " + e.getMessage());
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("加载外部存储总线列表失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return dataList;
    }
    
    // ========== 输入总线操作 ==========
    
    public void saveInputBusToDB(com.AlerCello86767.net_storage.controller.InputBusData data) {
        if (connection == null || data == null) return;
        
        try {
            String sql = """
                MERGE INTO input_buses (bus_uuid, location, network_id, container_location, container_type, filter_items, whitelist_mode, nbt_matching)
                KEY (location)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, data.busUuid.toString());
                stmt.setString(2, data.location);
                if (data.networkId != null) {
                    stmt.setString(3, data.networkId.toString());
                } else {
                    stmt.setNull(3, Types.VARCHAR);
                }
                stmt.setString(4, data.containerLocation);
                stmt.setString(5, data.containerType);
                
                // 序列化过滤物品列表（JSON数组+Base64）
                String filterItemsStr = "[]";
                if (data.filterItems != null && !data.filterItems.isEmpty()) {
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < data.filterItems.size(); i++) {
                        if (i > 0) sb.append(",");
                        String itemBase64 = data.filterItems.get(i);
                        sb.append("\"").append(itemBase64 != null ? itemBase64.replace("\"", "\\\"") : "").append("\"");
                    }
                    sb.append("]");
                    filterItemsStr = sb.toString();
                }
                stmt.setString(6, filterItemsStr);
                stmt.setBoolean(7, data.whitelistMode);
                stmt.setBoolean(8, data.nbtMatching);
                
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("保存输入总线失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void deleteInputBusFromDB(String location) {
        if (connection == null) return;
        
        try {
            String sql = "DELETE FROM input_buses WHERE location = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, location);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("删除输入总线失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public java.util.List<com.AlerCello86767.net_storage.controller.InputBusData> loadAllInputBusesFromDB() {
        java.util.List<com.AlerCello86767.net_storage.controller.InputBusData> dataList = 
                new java.util.ArrayList<>();
        
        if (connection == null) return dataList;
        
        try {
            String sql = "SELECT * FROM input_buses";
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    try {
                        String busUuidStr = rs.getString("bus_uuid");
                        UUID busUuid = busUuidStr != null ? UUID.fromString(busUuidStr) : UUID.randomUUID();
                        
                        String location = rs.getString("location");
                        String networkIdStr = rs.getString("network_id");
                        UUID networkId = (networkIdStr != null && !networkIdStr.isEmpty()) ? UUID.fromString(networkIdStr) : null;
                        String containerLocation = rs.getString("container_location");
                        String containerType = rs.getString("container_type");
                        
                        com.AlerCello86767.net_storage.controller.InputBusData data = 
                                new com.AlerCello86767.net_storage.controller.InputBusData(
                                        busUuid, location, networkId, containerLocation, containerType);
                        
                        // 加载过滤设置（JSON数组+Base64）
                        String filterItemsStr = rs.getString("filter_items");
                        if (filterItemsStr != null && !filterItemsStr.isEmpty() && !"[]".equals(filterItemsStr)) {
                            java.util.List<String> filterItems = new java.util.ArrayList<>();
                            // 简单的 JSON 数组解析
                            try {
                                String content = filterItemsStr.trim();
                                if (content.startsWith("[") && content.endsWith("]")) {
                                    content = content.substring(1, content.length() - 1);
                                    String[] items = content.split(",");
                                    for (String item : items) {
                                        item = item.trim();
                                        if (item.startsWith("\"") && item.endsWith("\"")) {
                                            item = item.substring(1, item.length() - 1);
                                            item = item.replace("\\\"", "\"");
                                        }
                                        filterItems.add(item);
                                    }
                                }
                            } catch (Exception e) {
                                // 兼容旧格式（|分隔）
                                java.util.List<String> oldFormatItems = java.util.Arrays.asList(filterItemsStr.split("\\|"));
                                filterItems.addAll(oldFormatItems);
                            }
                            data.filterItems = filterItems;
                        }
                        data.whitelistMode = rs.getBoolean("whitelist_mode");
                        data.nbtMatching = rs.getBoolean("nbt_matching");
                        
                        dataList.add(data);
                    } catch (Exception e) {
                        plugin.getLogger().warning("加载输入总线失败，跳过: " + e.getMessage());
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("加载输入总线列表失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return dataList;
    }

    // ==================== 输出总线 (Output Bus) ====================

    public void saveOutputBusToDB(com.AlerCello86767.net_storage.controller.OutputBusData data) {
        if (connection == null || data == null) return;
        
        try {
            String sql = """
                MERGE INTO output_buses (bus_uuid, location, network_id, container_location, container_type, filter_items, nbt_matching)
                KEY (location)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, data.busUuid.toString());
                stmt.setString(2, data.location);
                if (data.networkId != null) {
                    stmt.setString(3, data.networkId.toString());
                } else {
                    stmt.setNull(3, Types.VARCHAR);
                }
                stmt.setString(4, data.containerLocation);
                stmt.setString(5, data.containerType);
                
                // 序列化过滤物品列表（JSON数组+Base64）
                String filterItemsStr = "[]";
                if (data.filterItems != null && !data.filterItems.isEmpty()) {
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < data.filterItems.size(); i++) {
                        if (i > 0) sb.append(",");
                        String itemBase64 = data.filterItems.get(i);
                        sb.append("\"").append(itemBase64 != null ? itemBase64.replace("\"", "\\\"") : "").append("\"");
                    }
                    sb.append("]");
                    filterItemsStr = sb.toString();
                }
                stmt.setString(6, filterItemsStr);
                stmt.setBoolean(7, data.nbtMatching);
                
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("保存输出总线失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void deleteOutputBusFromDB(String location) {
        if (connection == null) return;
        
        try {
            String sql = "DELETE FROM output_buses WHERE location = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, location);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("删除输出总线失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public java.util.List<com.AlerCello86767.net_storage.controller.OutputBusData> loadAllOutputBusesFromDB() {
        java.util.List<com.AlerCello86767.net_storage.controller.OutputBusData> dataList = 
                new java.util.ArrayList<>();
        
        if (connection == null) return dataList;
        
        try {
            String sql = "SELECT * FROM output_buses";
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    try {
                        String busUuidStr = rs.getString("bus_uuid");
                        UUID busUuid = busUuidStr != null ? UUID.fromString(busUuidStr) : UUID.randomUUID();
                        
                        String location = rs.getString("location");
                        String networkIdStr = rs.getString("network_id");
                        UUID networkId = (networkIdStr != null && !networkIdStr.isEmpty()) ? UUID.fromString(networkIdStr) : null;
                        String containerLocation = rs.getString("container_location");
                        String containerType = rs.getString("container_type");
                        
                        com.AlerCello86767.net_storage.controller.OutputBusData data = 
                                new com.AlerCello86767.net_storage.controller.OutputBusData(
                                        busUuid, location, networkId, containerLocation, containerType);
                        
                        // 加载过滤设置（JSON数组+Base64）
                        String filterItemsStr = rs.getString("filter_items");
                        if (filterItemsStr != null && !filterItemsStr.isEmpty() && !"[]".equals(filterItemsStr)) {
                            java.util.List<String> filterItems = new java.util.ArrayList<>();
                            try {
                                String content = filterItemsStr.trim();
                                if (content.startsWith("[") && content.endsWith("]")) {
                                    content = content.substring(1, content.length() - 1);
                                    String[] items = content.split(",");
                                    for (String item : items) {
                                        item = item.trim();
                                        if (item.startsWith("\"") && item.endsWith("\"")) {
                                            item = item.substring(1, item.length() - 1);
                                            item = item.replace("\\\"", "\"");
                                        }
                                        filterItems.add(item);
                                    }
                                }
                            } catch (Exception e) {
                                // 兼容旧格式
                                java.util.List<String> oldFormatItems = java.util.Arrays.asList(filterItemsStr.split("\\|"));
                                filterItems.addAll(oldFormatItems);
                            }
                            data.filterItems = filterItems;
                        }
                        data.nbtMatching = rs.getBoolean("nbt_matching");
                        
                        dataList.add(data);
                    } catch (Exception e) {
                        plugin.getLogger().warning("加载输出总线失败，跳过: " + e.getMessage());
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("加载输出总线列表失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return dataList;
    }
}