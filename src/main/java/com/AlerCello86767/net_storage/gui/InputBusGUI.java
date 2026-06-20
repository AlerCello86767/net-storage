package com.AlerCello86767.net_storage.gui;

import com.AlerCello86767.net_storage.Net_storage;
import com.AlerCello86767.net_storage.controller.InputBusData;
import com.AlerCello86767.net_storage.utils.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class InputBusGUI extends BaseGUI {

    private final Net_storage plugin;
    private final InputBusData busData;
    
    // 槽位定义
    private static final int UPGRADE_START = 0;
    private static final int UPGRADE_END = 4;
    private static final int NBT_TOGGLE_SLOT = 7;
    private static final int FILTER_MODE_SLOT = 8;
    private static final int SEPARATOR_START = 9;
    private static final int SEPARATOR_END = 18;
    private static final int FILTER_START = 18;
    private static final int FILTER_END = 27;
    
    // 设置
    private boolean nbtMatching = false;
    private boolean whitelistMode = true;
    
    // 过滤列表
    private final List<ItemStack> filterItems = new ArrayList<>();
    
    public InputBusGUI(Player player, Net_storage plugin, InputBusData busData) {
        super(player, 27, ChatColor.translateAlternateColorCodes('&', "&d输入总线"));
        this.plugin = plugin;
        this.busData = busData;
    }
    
    @Override
    public void initialize() {
        inventory.clear();
        clickActions.clear();
        
        // 第一排：升级槽和按钮
        setupUpgradeSlots();
        setupButtons();
        
        // 第二排：分隔线
        setupSeparator();
        
        // 第三排：过滤列表
        setupFilterList();
    }
    
    private void setupUpgradeSlots() {
        ItemStack upgradePlaceholder = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .setName(ChatColor.GRAY + "升级槽")
                .setLore(ChatColor.DARK_GRAY + "暂未开放")
                .build();
        
        for (int i = UPGRADE_START; i < UPGRADE_END; i++) {
            inventory.setItem(i, upgradePlaceholder);
        }
        
        // 升级槽中间的空槽
        for (int i = UPGRADE_END; i < NBT_TOGGLE_SLOT; i++) {
            inventory.setItem(i, null);
        }
    }
    
    private void setupButtons() {
        // NBT匹配切换按钮
        ItemStack nbtToggle = new ItemStack(Material.IRON_SWORD);
        ItemMeta nbtMeta = nbtToggle.getItemMeta();
        if (nbtMeta != null) {
            nbtMeta.setDisplayName(nbtMatching 
                    ? ChatColor.GREEN + "NBT匹配: 启用" 
                    : ChatColor.RED + "NBT匹配: 忽略");
            List<String> nbtLore = new ArrayList<>();
            nbtLore.add(ChatColor.GRAY + "点击切换");
            nbtLore.add(ChatColor.GRAY + "启用: 精确匹配NBT");
            nbtLore.add(ChatColor.GRAY + "忽略: 只匹配物品类型");
            nbtMeta.setLore(nbtLore);
            if (nbtMatching) {
                nbtMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
                nbtMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            nbtToggle.setItemMeta(nbtMeta);
        }
        inventory.setItem(NBT_TOGGLE_SLOT, nbtToggle);
        setClickAction(NBT_TOGGLE_SLOT, this::handleNbtToggleClick);
        
        // 过滤模式切换按钮
        ItemStack filterMode = new ItemStack(whitelistMode ? Material.WHITE_CONCRETE : Material.BLACK_CONCRETE);
        ItemMeta filterMeta = filterMode.getItemMeta();
        if (filterMeta != null) {
            filterMeta.setDisplayName(whitelistMode 
                    ? ChatColor.GREEN + "白名单模式" 
                    : ChatColor.RED + "黑名单模式");
            List<String> filterLore = new ArrayList<>();
            filterLore.add(ChatColor.GRAY + "点击切换");
            filterLore.add(ChatColor.GRAY + "白名单: 只传输列表中的物品");
            filterLore.add(ChatColor.GRAY + "黑名单: 排除列表中的物品");
            filterMeta.setLore(filterLore);
            filterMode.setItemMeta(filterMeta);
        }
        inventory.setItem(FILTER_MODE_SLOT, filterMode);
        setClickAction(FILTER_MODE_SLOT, this::handleFilterModeToggleClick);
    }
    
    private void setupSeparator() {
        ItemStack separator = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        for (int i = SEPARATOR_START; i < SEPARATOR_END; i++) {
            inventory.setItem(i, separator);
        }
    }
    
    private void setupFilterList() {
        for (int i = FILTER_START; i < FILTER_END; i++) {
            int filterIndex = i - FILTER_START;
            if (filterIndex < filterItems.size()) {
                inventory.setItem(i, filterItems.get(filterIndex));
            } else {
                inventory.setItem(i, null);
            }
            setClickAction(i, this::handleFilterSlotClick);
        }
    }
    
    private void handleNbtToggleClick(Player player, ItemStack item, int slot, com.AlerCello86767.net_storage.gui.ClickType clickType) {
        nbtMatching = !nbtMatching;
        player.sendMessage(ChatColor.GRAY + "NBT匹配: " + (nbtMatching ? ChatColor.GREEN + "启用" : ChatColor.RED + "忽略"));
        update();
    }
    
    private void handleFilterModeToggleClick(Player player, ItemStack item, int slot, com.AlerCello86767.net_storage.gui.ClickType clickType) {
        whitelistMode = !whitelistMode;
        player.sendMessage(ChatColor.GRAY + "过滤模式: " + (whitelistMode ? ChatColor.GREEN + "白名单" : ChatColor.RED + "黑名单"));
        update();
    }
    
    private void handleFilterSlotClick(Player player, ItemStack item, int slot, com.AlerCello86767.net_storage.gui.ClickType clickType) {
        if (item != null && item.getType() != Material.AIR) {
            int filterIndex = slot - FILTER_START;
            filterItems.remove(filterIndex);
            player.sendMessage(ChatColor.GRAY + "已从过滤列表移除: " + ChatColor.WHITE + getItemName(item));
            update();
        }
    }
    
    private String getItemName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return item.getType().name();
    }
    
    @Override
    public void handleDrag(InventoryDragEvent event) {
        for (int slot : event.getRawSlots()) {
            if (slot < inventory.getSize()) {
                if (slot >= UPGRADE_START && slot < UPGRADE_END) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "无法放入升级槽！");
                    return;
                }
                if (slot >= SEPARATOR_START && slot < SEPARATOR_END) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        
        for (int slot : event.getRawSlots()) {
            if (slot >= FILTER_START && slot < FILTER_END) {
                ItemStack draggedItem = event.getCursor();
                if (draggedItem != null && draggedItem.getType() != Material.AIR) {
                    int filterIndex = slot - FILTER_START;
                    ItemStack filterItem = draggedItem.clone();
                    filterItem.setAmount(1);
                    
                    if (filterIndex < filterItems.size()) {
                        filterItems.set(filterIndex, filterItem);
                    } else {
                        filterItems.add(filterItem);
                    }
                    player.sendMessage(ChatColor.GREEN + "已添加到过滤列表: " + ChatColor.WHITE + getItemName(filterItem));
                }
            }
        }
        
        event.setCancelled(true);
        update();
    }
    
    @Override
    protected void onClose() {
        saveSettings();
    }
    
    private void saveSettings() {
        // 可以在这里保存设置到数据库
        plugin.getLogger().info("输入总线设置已保存: NBT匹配=" + nbtMatching + ", 白名单模式=" + whitelistMode);
    }
    
    public boolean isNbtMatching() {
        return nbtMatching;
    }
    
    public boolean isWhitelistMode() {
        return whitelistMode;
    }
    
    public List<ItemStack> getFilterItems() {
        return filterItems;
    }
}