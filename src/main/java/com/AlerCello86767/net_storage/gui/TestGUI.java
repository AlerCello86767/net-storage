package com.AlerCello86767.net_storage.gui;

import com.AlerCello86767.net_storage.utils.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 测试GUI - 演示所有点击事件
 */
public class TestGUI extends BaseGUI {

    private final Map<Integer, ItemStack> testItems = new HashMap<>();
    private final Random random = new Random();
    private int counter = 0;

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
        super(player, 54, ChatColor.BLUE + "测试界面");
    }

    @Override
    public void initialize() {
        inventory.clear();
        clearClickActions();
        testItems.clear();

        setBorder(ItemBuilder.createBorder());

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

        // 区域点击
        setRegionClickAction(20, 30, (p, item, slot, clickType) -> {
            if (item != null && item.getType() != Material.AIR) {
                p.sendMessage(ChatColor.AQUA + "你点击了区域物品! 槽位: " + slot + " 物品: " + item.getType().name());
            }
        });

        // 关闭按钮
        setItem(49, ItemBuilder.createCloseButton());
        setClickAction(49, (p, item, slot, clickType) -> {
            p.closeInventory();
            p.sendMessage(ChatColor.GREEN + "已关闭界面！");
        });

        // 随机物品
        for (int i = 20; i < 30; i++) {
            Material randomMat = RANDOM_MATERIALS.get(random.nextInt(RANDOM_MATERIALS.size()));
            ItemStack item = new ItemBuilder(randomMat)
                    .setName("&7物品 #" + (i - 19))
                    .setLore("&8点击我试试")
                    .build();
            setItem(i, item);
        }

        // 使用说明
        ItemStack info = new ItemBuilder(Material.OAK_SIGN)
                .setName("&e&l使用说明")
                .setLore(
                        "&7红色羊毛: 左键点击测试",
                        "&7绿色羊毛: 右键点击测试",
                        "&7蓝色羊毛: 显示详细信息",
                        "&7金色锭: 点击计数",
                        "&7中间物品: 区域点击测试",
                        "&7背包物品: 也可点击测试"
                )
                .build();
        setItem(4, info);
    }

    private void updateCounterItem() {
        ItemStack newItem = new ItemBuilder(Material.GOLD_INGOT)
                .setName("&6点击计数")
                .setLore("&7点击次数: &e" + counter)
                .build();
        setItem(16, newItem);
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
        player.sendMessage(ChatColor.GRAY + "试试点击不同颜色的羊毛查看效果");
    }

    @Override
    protected void onClose() {
        player.sendMessage(ChatColor.YELLOW + "测试界面已关闭！");
    }
}