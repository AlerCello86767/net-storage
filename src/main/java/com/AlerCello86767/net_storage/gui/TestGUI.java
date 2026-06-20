package com.AlerCello86767.net_storage.gui;

import com.AlerCello86767.net_storage.utils.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * 测试GUI - 演示所有点击事件和拖拽事件
 */
public class TestGUI extends BaseGUI {

    private int counter = 0;
    private final Random random = new Random();

    // 拖拽测试区域
    private static final int DRAG_ALLOW_START = 9;
    private static final int DRAG_ALLOW_END = 35;
    private static final int DRAG_FORBID_START = 36;
    private static final int DRAG_FORBID_END = 44;

    private static final List<Material> RANDOM_MATERIALS = Arrays.asList(
            Material.DIAMOND,
            Material.EMERALD,
            Material.GOLD_INGOT,
            Material.IRON_INGOT,
            Material.REDSTONE,
            Material.LAPIS_LAZULI,
            Material.COAL,
            Material.QUARTZ,
            Material.NETHERITE_INGOT,
            Material.AMETHYST_SHARD,
            Material.COPPER_INGOT,
            Material.ECHO_SHARD
    );

    public TestGUI(Player player) {
        super(player, 54, ChatColor.BLUE + "测试界面 - 点击 & 拖拽");
    }

    @Override
    public void initialize() {
        inventory.clear();
        clearClickActions();

        setBorder(ItemBuilder.createBorder());

        // === 第一行：功能按钮 ===
        setupFunctionButtons();

        // === 第二到四行：拖拽允许区域（绿色） ===
        setupDragAllowArea();

        // === 第五行：拖拽禁止区域（红色） ===
        setupDragForbidArea();

        // === 第六行：随机物品 + 关闭按钮 ===
        setupBottomRow();

        // === 说明 ===
        setupInfoItem();
    }

    private void setupFunctionButtons() {
        // 红色羊毛 - 左键测试
        ItemStack leftClickItem = new ItemBuilder(Material.RED_WOOL)
                .setName("&c左键点击")
                .setLore("&7左键点击触发事件", "&7Shift+左键触发事件")
                .build();
        setItem(10, leftClickItem);
        setClickAction(10, (p, item, slot, clickType) -> {
            p.sendMessage(ChatColor.GREEN + "你点击了左键物品！点击类型: " + clickType.name());
        });

        // 绿色羊毛 - 右键测试
        ItemStack rightClickItem = new ItemBuilder(Material.GREEN_WOOL)
                .setName("&a右键点击")
                .setLore("&7右键点击触发事件", "&7Shift+右键触发事件")
                .build();
        setItem(12, rightClickItem);
        setClickAction(12, (p, item, slot, clickType) -> {
            p.sendMessage(ChatColor.GREEN + "你点击了右键物品！点击类型: " + clickType.name());
        });

        // 蓝色羊毛 - 信息显示
        ItemStack infoItem = new ItemBuilder(Material.BLUE_WOOL)
                .setName("&b点击显示信息")
                .setLore("&7点击显示详细信息")
                .build();
        setItem(14, infoItem);
        setClickAction(14, (p, item, slot, clickType) -> {
            p.sendMessage(ChatColor.GOLD + "=== 点击信息 ===");
            p.sendMessage(ChatColor.YELLOW + "点击类型: " + ChatColor.WHITE + clickType.name());
            p.sendMessage(ChatColor.YELLOW + "槽位: " + ChatColor.WHITE + slot);
            p.sendMessage(ChatColor.YELLOW + "物品: " + ChatColor.WHITE + (item != null ? item.getType().name() : "空气"));
            p.sendMessage(ChatColor.YELLOW + "Shift点击: " + ChatColor.WHITE + clickType.isShiftClick());
            p.sendMessage(ChatColor.YELLOW + "左键: " + ChatColor.WHITE + clickType.isLeftClick());
            p.sendMessage(ChatColor.YELLOW + "右键: " + ChatColor.WHITE + clickType.isRightClick());
        });

        // 金锭 - 计数器
        updateCounterItem();
        setClickAction(16, (p, item, slot, clickType) -> {
            counter++;
            updateCounterItem();
            p.sendMessage(ChatColor.GREEN + "点击次数: " + ChatColor.YELLOW + counter);
        });
    }

    private void updateCounterItem() {
        ItemStack newItem = new ItemBuilder(Material.GOLD_INGOT)
                .setName("&6点击计数")
                .setLore("&7点击次数: &e" + counter)
                .build();
        setItem(16, newItem);
    }

    private void setupDragAllowArea() {
        // 绿色玻璃板 - 标识可拖拽区域
        ItemStack allowItem = new ItemBuilder(Material.GREEN_STAINED_GLASS_PANE)
                .setName("&a可拖拽区域")
                .setLore("&7从背包拖拽物品到这里", "&7会自动放置")
                .hideAll()
                .build();

        for (int i = DRAG_ALLOW_START; i <= DRAG_ALLOW_END; i++) {
            inventory.setItem(i, allowItem);
        }
    }

    private void setupDragForbidArea() {
        // 红色玻璃板 - 标识禁止拖拽区域
        ItemStack forbidItem = new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                .setName("&c禁止拖拽区域")
                .setLore("&7拖拽到此区域会被拒绝")
                .hideAll()
                .build();

        for (int i = DRAG_FORBID_START; i <= DRAG_FORBID_END; i++) {
            inventory.setItem(i, forbidItem);
        }
    }

    private void setupBottomRow() {
        // 随机物品（槽位 45-48）
        for (int i = 45; i <= 48; i++) {
            Material randomMat = RANDOM_MATERIALS.get(random.nextInt(RANDOM_MATERIALS.size()));
            ItemStack item = new ItemBuilder(randomMat)
                    .setName("&7随机物品")
                    .setLore("&8点击我试试")
                    .build();
            setItem(i, item);
            setClickAction(i, (p, it, slot, clickType) -> {
                p.sendMessage(ChatColor.AQUA + "你点击了 " + it.getType().name() + "！");
            });
        }

        // 关闭按钮
        for (int i = 49; i <= 53; i++) {
            if (i == 49) {
                setItem(i, ItemBuilder.createCloseButton());
                setClickAction(i, (p, it, slot, clickType) -> {
                    p.closeInventory();
                    p.sendMessage(ChatColor.GREEN + "已关闭界面！");
                });
            } else {
                ItemStack glass = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                        .setName(" ")
                        .hideAll()
                        .build();
                setItem(i, glass);
            }
        }
    }

    private void setupInfoItem() {
        ItemStack info = new ItemBuilder(Material.OAK_SIGN)
                .setName("&e&l使用说明")
                .setLore(
                        "&7红色羊毛: 左键点击测试",
                        "&7绿色羊毛: 右键点击测试",
                        "&7蓝色羊毛: 显示详细信息",
                        "&7金色锭: 点击计数",
                        "",
                        "&a绿色区域: 可拖拽放入",
                        "&c红色区域: 禁止拖拽",
                        "&7从背包拖拽物品到绿色区域测试"
                )
                .build();
        setItem(4, info);
    }

    // ========== 拖拽事件处理 ==========

    @Override
    public void handleDrag(InventoryDragEvent event) {
        // 1. 过滤出 GUI 区域的槽位
        Set<Integer> guiSlots = new HashSet<>();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < inventory.getSize()) {
                guiSlots.add(rawSlot);
            }
        }

        // 2. 只在背包内拖拽 → 放行
        if (guiSlots.isEmpty()) {
            event.setCancelled(false);
            return;
        }

        // 3. 检查是否拖拽到禁止区域（红色区域，槽位 36-44）
        boolean hasForbiddenSlot = false;
        for (int slot : guiSlots) {
            if (slot >= DRAG_FORBID_START && slot <= DRAG_FORBID_END) {
                hasForbiddenSlot = true;
                break;
            }
        }

        if (hasForbiddenSlot) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "禁止拖拽到红色区域！");
            return;
        }

        // 4. 检查是否拖拽到允许区域（绿色区域，槽位 9-35）
        boolean hasAllowSlot = false;
        for (int slot : guiSlots) {
            if (slot >= DRAG_ALLOW_START && slot <= DRAG_ALLOW_END) {
                hasAllowSlot = true;
                break;
            }
        }

        if (hasAllowSlot) {
            // 放行，让物品进入绿色区域
            event.setCancelled(false);
            ItemStack dragged = event.getOldCursor();
            if (dragged != null && dragged.getType() != Material.AIR) {
                player.sendMessage(ChatColor.GREEN + "拖拽成功！物品: " + dragged.getType().name() +
                        " x" + dragged.getAmount() + " 到绿色区域");
            }
        } else {
            // 其他 GUI 区域默认取消
            event.setCancelled(true);
        }
    }

    @Override
    protected void handlePlayerInventoryClick(InventoryClickEvent event, int slot, ItemStack item, com.AlerCello86767.net_storage.gui.ClickType clickType) {
        event.setCancelled(false);

        Player p = (Player) event.getWhoClicked();
        p.sendMessage(ChatColor.LIGHT_PURPLE + "你点击了背包中的物品！");
        p.sendMessage(ChatColor.GRAY + "点击类型: " + clickType.name());

        if (item != null && item.getType() != Material.AIR) {
            p.sendMessage(ChatColor.GRAY + "物品: " + item.getType().name() + " x" + item.getAmount());
        }
    }

    @Override
    protected void onOpen() {
        player.sendMessage(ChatColor.GREEN + "测试界面已打开！");
        player.sendMessage(ChatColor.GRAY + "试试从背包拖拽物品到绿色区域");
        player.sendMessage(ChatColor.GRAY + "拖拽到红色区域会被拒绝");
    }

    @Override
    protected void onClose() {
        player.sendMessage(ChatColor.YELLOW + "测试界面已关闭！");
    }
}