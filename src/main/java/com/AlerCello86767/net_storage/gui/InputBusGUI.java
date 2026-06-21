package com.AlerCello86767.net_storage.gui;

import com.AlerCello86767.net_storage.Net_storage;
import com.AlerCello86767.net_storage.controller.InputBusData;
import com.AlerCello86767.net_storage.utils.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InputBusGUI extends BaseGUI {

    private final Net_storage plugin;
    private final InputBusData busData;

    private static final int UPGRADE_START = 0;
    private static final int UPGRADE_END = 4;
    private static final int NBT_TOGGLE_SLOT = 7;
    private static final int FILTER_MODE_SLOT = 8;
    private static final int SEPARATOR_START = 9;
    private static final int SEPARATOR_END = 18;
    private static final int FILTER_START = 18;
    private static final int FILTER_END = 27;

    private boolean nbtMatching = false;
    private boolean whitelistMode = true;
    private final List<ItemStack> filterItems = new ArrayList<>();

    public InputBusGUI(Player player, Net_storage plugin, InputBusData busData) {
        super(player, 27, ChatColor.translateAlternateColorCodes('&', "&d输入总线"));
        this.plugin = plugin;
        this.busData = busData;
        
        // 从 busData 加载过滤设置
        if (busData != null) {
            this.whitelistMode = busData.whitelistMode;
            this.nbtMatching = busData.nbtMatching;
            // 反序列化过滤物品（保留槽位索引，Base64格式）
            if (busData.filterItems != null) {
                for (int i = 0; i < busData.filterItems.size() && i < 9; i++) {
                    String serialized = busData.filterItems.get(i);
                    if (serialized != null && !serialized.isEmpty()) {
                        try {
                            // 使用 Base64 反序列化
                            ItemStack item = InputBusData.deserializeItemStack(serialized);
                            if (item != null) {
                                this.filterItems.add(item);
                            } else {
                                this.filterItems.add(null);  // 反序列化失败
                            }
                        } catch (Exception e) {
                            this.filterItems.add(null);  // 无效数据视为空槽位
                        }
                    } else {
                        this.filterItems.add(null);  // 空字符串表示空槽位
                    }
                }
                // 确保有9个槽位
                while (this.filterItems.size() < 9) {
                    this.filterItems.add(null);
                }
            }
        }
    }

    @Override
    public void initialize() {
        inventory.clear();
        clickActions.clear();

        setupUpgradeSlots();
        setupButtons();
        setupSeparator();
        setupFilterList();
    }

    private void setupUpgradeSlots() {
        ItemStack upgrade = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .setName(ChatColor.GRAY + "升级槽")
                .setLore(ChatColor.DARK_GRAY + "暂未开放")
                .build();

        for (int i = UPGRADE_START; i < UPGRADE_END; i++) {
            inventory.setItem(i, upgrade);
        }

        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .setName(" ")
                .hideAll()
                .build();

        for (int i = UPGRADE_END; i < NBT_TOGGLE_SLOT; i++) {
            inventory.setItem(i, filler);
        }
    }

    private void setupButtons() {
        ItemStack nbtToggle = new ItemStack(Material.IRON_SWORD);
        ItemMeta nbtMeta = nbtToggle.getItemMeta();
        if (nbtMeta != null) {
            nbtMeta.setDisplayName(nbtMatching ? ChatColor.GREEN + "NBT匹配: 启用" : ChatColor.RED + "NBT匹配: 忽略");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "点击切换");
            lore.add(ChatColor.GRAY + "启用: 精确匹配NBT");
            lore.add(ChatColor.GRAY + "忽略: 只匹配物品类型");
            nbtMeta.setLore(lore);
            if (nbtMatching) {
                nbtMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
                nbtMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            nbtToggle.setItemMeta(nbtMeta);
        }
        inventory.setItem(NBT_TOGGLE_SLOT, nbtToggle);
        setClickAction(NBT_TOGGLE_SLOT, (p, i, s, c) -> {
            nbtMatching = !nbtMatching;
            p.sendMessage(ChatColor.GRAY + "NBT匹配: " + (nbtMatching ? ChatColor.GREEN + "启用" : ChatColor.RED + "忽略"));
            update();
        });

        ItemStack filterMode = new ItemStack(whitelistMode ? Material.WHITE_CONCRETE : Material.BLACK_CONCRETE);
        ItemMeta filterMeta = filterMode.getItemMeta();
        if (filterMeta != null) {
            filterMeta.setDisplayName(whitelistMode ? ChatColor.GREEN + "白名单模式" : ChatColor.RED + "黑名单模式");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "点击切换");
            lore.add(ChatColor.GRAY + "白名单: 只传输列表中的物品");
            lore.add(ChatColor.GRAY + "黑名单: 排除列表中的物品");
            filterMeta.setLore(lore);
            filterMode.setItemMeta(filterMeta);
        }
        inventory.setItem(FILTER_MODE_SLOT, filterMode);
        setClickAction(FILTER_MODE_SLOT, (p, i, s, c) -> {
            whitelistMode = !whitelistMode;
            p.sendMessage(ChatColor.GRAY + "过滤模式: " + (whitelistMode ? ChatColor.GREEN + "白名单" : ChatColor.RED + "黑名单"));
            update();
        });
    }

    private void setupSeparator() {
        ItemStack separator = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        for (int i = SEPARATOR_START; i < SEPARATOR_END; i++) {
            inventory.setItem(i, separator);
        }
    }

    private void setupFilterList() {
        for (int i = FILTER_START; i < FILTER_END; i++) {
            int index = i - FILTER_START;
            if (index < filterItems.size() && filterItems.get(index) != null) {
                inventory.setItem(i, filterItems.get(index));
            } else {
                // 空槽位显示为占位符
                ItemStack emptySlot = new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                        .setName(ChatColor.GRAY + "过滤槽 #" + (index + 1))
                        .setLore(ChatColor.DARK_GRAY + "点击放入物品")
                        .build();
                inventory.setItem(i, emptySlot);
            }

            setClickAction(i, (p, item, slot, clickType) -> {
                int idx = slot - FILTER_START;

                // 检查点击的槽位是否有物品
                if (item != null && item.getType() != Material.AIR && item.getType() != Material.LIGHT_GRAY_STAINED_GLASS_PANE) {
                    // 点击已有物品，移除过滤（设为null保持索引）
                    if (idx < filterItems.size() && filterItems.get(idx) != null) {
                        filterItems.set(idx, null);
                        p.sendMessage(ChatColor.GRAY + "已移除: " + getItemName(item));
                        update();
                    }
                } else {
                    // 点击空槽位，检查鼠标上的物品
                    ItemStack cursor = p.getItemOnCursor();
                    if (cursor != null && cursor.getType() != Material.AIR) {
                        // 确保 filterItems 列表足够大
                        while (filterItems.size() <= idx) {
                            filterItems.add(null);
                        }

                        // 检查是否已存在相同物品（避免重复）
                        for (ItemStack existing : filterItems) {
                            if (existing != null && existing.isSimilar(cursor)) {
                                p.sendMessage(ChatColor.YELLOW + "该物品已在过滤列表中！");
                                return;
                            }
                        }

                        // 检查槽位是否已有物品
                        if (filterItems.get(idx) != null) {
                            p.sendMessage(ChatColor.YELLOW + "该槽已有物品！");
                            return;
                        }

                        // 复制物品到过滤列表
                        ItemStack toAdd = cursor.clone();
                        toAdd.setAmount(1);
                        filterItems.set(idx, toAdd);

                        p.sendMessage(ChatColor.GREEN + "已添加物品到过滤列表");
                        update();
                    }
                }
            });
        }
    }

    private String getItemName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return item.getType().name();
    }

    // ========== 背包点击处理 ==========

// ========== 背包点击处理 ==========

    @Override
    protected void handlePlayerInventoryClick(InventoryClickEvent event, int slot, ItemStack item, ClickType clickType) {
        // 只处理背包区域的点击
        if (slot < inventory.getSize()) {
            event.setCancelled(true);
            return;
        }

        // 检查是否点击了有效的物品
        if (item == null || item.getType() == Material.AIR) {
            event.setCancelled(false);
            return;
        }

        // === Shift 点击处理 ===
        if (clickType == ClickType.SHIFT_LEFT_CLICK || clickType == ClickType.SHIFT_RIGHT_CLICK) {
            event.setCancelled(true);

            // 检查是否已存在相同物品（避免重复）
            for (ItemStack existing : filterItems) {
                if (existing != null && existing.isSimilar(item)) {
                    player.sendMessage(ChatColor.YELLOW + "该物品已在过滤列表中！");
                    return;
                }
            }

            // 找到第一个空槽位
            int emptySlot = -1;
            for (int i = 0; i < 9; i++) { // 过滤槽位共9个
                if (i >= filterItems.size() || filterItems.get(i) == null) {
                    emptySlot = i;
                    break;
                }
            }

            if (emptySlot == -1) {
                player.sendMessage(ChatColor.RED + "过滤列表已满！");
                return;
            }

            // 确保列表足够大
            while (filterItems.size() <= emptySlot) {
                filterItems.add(null);
            }

            // ✅ 复制物品到过滤列表（不消耗原物品）
            // 过滤列表只存储物品的"模板"用于匹配，不消耗实际物品
            ItemStack toAdd = item.clone();
            toAdd.setAmount(1);
            filterItems.set(emptySlot, toAdd);

            // 不移除背包中的物品，只复制一份
            // 这样玩家可以继续使用这些物品，同时过滤列表保存了物品模板

            player.sendMessage(ChatColor.GREEN + "已添加 " + getItemName(item) + " 到过滤列表！");
            update();
            return;
        }

        // === 普通点击（左键/右键） ===
        // 允许玩家正常操作背包（取放物品）
        event.setCancelled(false);
    }
    // ========== 拖拽处理 ==========

    @Override
    public void handleDrag(InventoryDragEvent event) {
        Set<Integer> guiSlots = new HashSet<>();
        for (int raw : event.getRawSlots()) {
            if (raw < inventory.getSize()) {
                guiSlots.add(raw);
            }
        }

        // 只在背包内拖拽 - 放行
        if (guiSlots.isEmpty()) {
            event.setCancelled(false);
            return;
        }

        // 检查是否有升级槽或分隔线
        for (int slot : guiSlots) {
            if ((slot >= UPGRADE_START && slot < UPGRADE_END) ||
                    (slot >= SEPARATOR_START && slot < SEPARATOR_END)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "无法拖拽到该区域！");
                return;
            }
        }

        // 检查是否拖拽到过滤槽
        List<Integer> filterSlots = new ArrayList<>();
        for (int slot : guiSlots) {
            if (slot >= FILTER_START && slot < FILTER_END) {
                filterSlots.add(slot);
            }
        }

        if (filterSlots.isEmpty()) {
            event.setCancelled(true);
            return;
        }

        ItemStack dragged = event.getOldCursor();
        if (dragged == null || dragged.getType() == Material.AIR) {
            event.setCancelled(false);
            return;
        }

        event.setCancelled(true);

        ItemStack toAdd = dragged.clone();
        toAdd.setAmount(1);
        
        // 检查是否已存在相同物品（避免重复）
        for (ItemStack existing : filterItems) {
            if (existing != null && existing.isSimilar(toAdd)) {
                player.sendMessage(ChatColor.YELLOW + "该物品已在过滤列表中！");
                return;
            }
        }

        int placed = 0;
        for (int slot : filterSlots) {
            int index = slot - FILTER_START;
            if (index < filterItems.size() && filterItems.get(index) != null) continue;

            while (filterItems.size() <= index) {
                filterItems.add(null);
            }
            filterItems.set(index, toAdd.clone());
            placed++;
        }

        if (placed > 0) {
            // 不扣除玩家物品，只复制物品到过滤列表
            // 由于event.setCancelled(false)，鼠标上的物品会自动回到玩家背包
            event.setCancelled(false);
            player.sendMessage(ChatColor.GREEN + "已添加 " + placed + " 个物品到过滤列表");
            update();
        } else {
            player.sendMessage(ChatColor.YELLOW + "所有过滤槽已满！");
        }
    }

    @Override
    protected void onClose() {
        // 保存过滤设置到 InputBusData
        if (busData != null) {
            // 序列化过滤物品（保留槽位索引，null用空字符串表示，Base64格式）
            busData.filterItems = new java.util.ArrayList<>();
            for (int i = 0; i < 9; i++) {  // 过滤列表最多9个槽位
                if (i < filterItems.size()) {
                    ItemStack item = filterItems.get(i);
                    if (item != null && item.getType() != Material.AIR) {
                        busData.filterItems.add(InputBusData.serializeItemStack(item));
                    } else {
                        busData.filterItems.add("");  // 空槽位用空字符串表示
                    }
                } else {
                    busData.filterItems.add("");  // 未使用的槽位
                }
            }
            busData.whitelistMode = whitelistMode;
            busData.nbtMatching = nbtMatching;
            
            // 保存到数据库
            plugin.getDatabaseManager().saveInputBusToDB(busData);
            plugin.getLogger().info("输入总线设置已保存: " + busData.filterItems.size() + " 个过滤槽位");
        }
    }

    public boolean isNbtMatching() { return nbtMatching; }
    public boolean isWhitelistMode() { return whitelistMode; }
    public List<ItemStack> getFilterItems() { return filterItems; }
}