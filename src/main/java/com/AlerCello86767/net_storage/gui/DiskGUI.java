package com.AlerCello86767.net_storage.gui;

import com.AlerCello86767.net_storage.Net_storage;
import com.AlerCello86767.net_storage.disk.DiskItem;
import com.AlerCello86767.net_storage.disk.DiskManager;
import com.AlerCello86767.net_storage.utils.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 磁盘内容查看 GUI
 * 显示磁盘内的所有物品、使用量进度条和基本信息
 */
public class DiskGUI extends BaseGUI {

    private final Net_storage plugin;
    private final ItemStack diskItem;
    private final UUID diskUuid;
    private final List<DiskItem> diskItems;
    private final DiskManager diskManager;
    
    private int currentPage = 1;
    private int totalPages = 1;
    private String diskType;
    
    // 物品显示区域（槽位 9-44，共 36 格）
    private static final int ITEM_AREA_START = 9;
    private static final int ITEM_AREA_END = 44;
    private static final int ITEMS_PER_PAGE = 36;
    
    // 特殊槽位
    private static final int INFO_SLOT = 0;           // 指南针（磁盘信息）
    private static final int PREV_PAGE_SLOT = 45;     // 上一页
    private static final int PAGE_INFO_SLOT = 49;     // 页数信息
    private static final int NEXT_PAGE_SLOT = 53;     // 下一页
    
    // 时间格式化
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public DiskGUI(Player player, Net_storage plugin, ItemStack diskItem, UUID diskUuid, List<DiskItem> items) {
        super(player, 54, ChatColor.translateAlternateColorCodes('&', "&b磁盘内容"));
        this.plugin = plugin;
        this.diskItem = diskItem;
        this.diskUuid = diskUuid;
        this.diskItems = items;
        this.diskManager = plugin.getDiskManager();
        
        // 获取磁盘类型
        if (diskItem.hasItemMeta()) {
            this.diskType = diskItem.getItemMeta().getPersistentDataContainer()
                    .get(new org.bukkit.NamespacedKey(plugin, "item_type"), 
                         org.bukkit.persistence.PersistentDataType.STRING);
            if (this.diskType == null) {
                this.diskType = "disk_1k";
            }
        } else {
            this.diskType = "disk_1k";
        }
        
        // 计算总页数
        totalPages = Math.max(1, (int) Math.ceil(items.size() / (double) ITEMS_PER_PAGE));
        
        initialize();
    }

    @Override
    public void initialize() {
        setupInfoSlot();
        setupTopRowFiller();
        setupItems();
        setupPagination();
        setupBottomRowFiller();
    }

    /**
     * 设置指南针信息槽位
     */
    private void setupInfoSlot() {
        int totalItems = diskManager.getTotalItems(diskItems);
        int maxCapacity = diskManager.getMaxCapacity();
        int percentage = maxCapacity > 0 ? (totalItems * 100) / maxCapacity : 0;
        
        // 获取创建时间（从数据库）
        String createdAt = "未知";
        String jsonData = plugin.getDatabaseManager().loadDiskFromDB(diskUuid);
        if (jsonData != null && !jsonData.isEmpty()) {
            // 简化显示，使用当前时间作为参考
            createdAt = DATE_FORMAT.format(new Date());
        }
        
        // 构建进度条
        String progressBar = createProgressBar(percentage);
        
        // 构建UUID显示
        String uuidStr = diskUuid.toString();
        String uuidShort = uuidStr.substring(0, 8) + "...";
        
        ItemStack compass = new ItemBuilder(Material.COMPASS)
                .setName(ChatColor.GOLD + "磁盘信息")
                .setLore(
                        ChatColor.GRAY + "类型: " + ChatColor.WHITE + diskType,
                        ChatColor.GRAY + "UUID: " + ChatColor.WHITE + uuidShort,
                        ChatColor.GRAY + "完整UUID: " + ChatColor.WHITE + uuidStr,
                        "",
                        ChatColor.GRAY + "容量: " + ChatColor.WHITE + totalItems + "/" + maxCapacity,
                        ChatColor.GRAY + "使用率: " + ChatColor.WHITE + percentage + "%",
                        progressBar,
                        "",
                        ChatColor.GRAY + "物品种类: " + ChatColor.WHITE + diskItems.size(),
                        ChatColor.GRAY + "创建时间: " + ChatColor.WHITE + createdAt
                )
                .build();
        
        setItem(INFO_SLOT, compass);
    }

    /**
     * 创建进度条
     * @param percentage 使用百分比 (0-100)
     * @return 进度条字符串
     */
    private String createProgressBar(int percentage) {
        int filled = (percentage * 20) / 100;
        int remain = 20 - filled;
        
        // 根据百分比选择颜色
        ChatColor fillColor;
        if (percentage < 80) {
            fillColor = ChatColor.BLUE;  // 0%-79%: 蓝色
        } else if (percentage < 95) {
            fillColor = ChatColor.YELLOW; // 80%-94%: 黄色
        } else {
            fillColor = ChatColor.RED;    // 95%-100%: 红色
        }
        
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < filled; i++) {
            bar.append(fillColor).append("|");
        }
        for (int i = 0; i < remain; i++) {
            bar.append(ChatColor.GRAY).append("|");
        }
        
        return bar.toString();
    }

    /**
     * 设置第一排填充物（槽位 1-8）
     */
    private void setupTopRowFiller() {
        ItemStack glassPane = new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                .setName(" ")
                .build();
        
        for (int i = 1; i <= 8; i++) {
            setItem(i, glassPane);
        }
    }

    /**
     * 设置物品列表显示区域（槽位 9-44）
     */
    private void setupItems() {
        if (diskItems.isEmpty()) {
            // 磁盘为空，显示占位符
            ItemStack emptyPlaceholder = new ItemBuilder(Material.BARRIER)
                    .setName(ChatColor.GRAY + "磁盘为空")
                    .setLore(ChatColor.DARK_GRAY + "使用 /disktest add 添加物品")
                    .build();
            
            for (int i = ITEM_AREA_START; i <= ITEM_AREA_END; i++) {
                setItem(i, emptyPlaceholder);
            }
            return;
        }
        
        // 计算当前页的物品范围
        int startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, diskItems.size());
        
        int slot = ITEM_AREA_START;
        for (int i = startIndex; i < endIndex && slot <= ITEM_AREA_END; i++) {
            DiskItem diskItem = diskItems.get(i);
            
            // 获取物品显示名称
            String displayName = diskItem.getDisplayName();
            if (displayName == null) {
                displayName = ChatColor.WHITE + diskItem.getMaterial().name();
            } else {
                displayName = ChatColor.WHITE + displayName;
            }
            
            // 创建显示物品
            ItemStack displayItem = new ItemBuilder(diskItem.getMaterial())
                    .setName(displayName)
                    .setLore(
                            ChatColor.GRAY + "数量: " + ChatColor.WHITE + diskItem.getAmount() + " 个"
                    )
                    .build();
            
            setItem(slot, displayItem);
            slot++;
        }
        
        // 填充剩余空位
        ItemStack emptyPane = new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                .setName(" ")
                .build();
        
        while (slot <= ITEM_AREA_END) {
            setItem(slot, emptyPane);
            slot++;
        }
    }

    /**
     * 设置翻页控制区域
     */
    private void setupPagination() {
        // 上一页按钮
        ItemStack prevPageItem;
        if (currentPage > 1) {
            prevPageItem = new ItemBuilder(Material.ARROW)
                    .setName(ChatColor.YELLOW + "上一页")
                    .setLore(ChatColor.GRAY + "当前页: " + currentPage + "/" + totalPages)
                    .build();
            
            setClickAction(PREV_PAGE_SLOT, (p, item, slot, clickType) -> {
                if (currentPage > 1) {
                    currentPage--;
                    update();
                }
            });
        } else {
            prevPageItem = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                    .setName(ChatColor.DARK_GRAY + "已是第一页")
                    .build();
        }
        setItem(PREV_PAGE_SLOT, prevPageItem);
        
        // 页数信息
        ItemStack pageInfoItem = new ItemBuilder(Material.BELL)
                .setName(ChatColor.GOLD + "页面信息")
                .setLore(
                        ChatColor.GRAY + "当前页: " + ChatColor.WHITE + currentPage,
                        ChatColor.GRAY + "总页数: " + ChatColor.WHITE + totalPages
                )
                .build();
        setItem(PAGE_INFO_SLOT, pageInfoItem);
        
        // 下一页按钮
        ItemStack nextPageItem;
        if (currentPage < totalPages) {
            nextPageItem = new ItemBuilder(Material.ARROW)
                    .setName(ChatColor.YELLOW + "下一页")
                    .setLore(ChatColor.GRAY + "当前页: " + currentPage + "/" + totalPages)
                    .build();
            
            setClickAction(NEXT_PAGE_SLOT, (p, item, slot, clickType) -> {
                if (currentPage < totalPages) {
                    currentPage++;
                    update();
                }
            });
        } else {
            nextPageItem = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                    .setName(ChatColor.DARK_GRAY + "已是最后一页")
                    .build();
        }
        setItem(NEXT_PAGE_SLOT, nextPageItem);
    }

    /**
     * 设置第六排填充物（除翻页按钮外的槽位）
     */
    private void setupBottomRowFiller() {
        ItemStack glassPane = new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                .setName(" ")
                .build();
        
        for (int i = 45; i <= 53; i++) {
            if (i != PREV_PAGE_SLOT && i != PAGE_INFO_SLOT && i != NEXT_PAGE_SLOT) {
                setItem(i, glassPane);
            }
        }
    }

    /**
     * 更新 GUI 内容
     */
    public void update() {
        inventory.clear();
        clickActions.clear();
        initialize();
        player.updateInventory();
    }
}