package com.AlerCello86767.net_storage.utils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 物品构建工具类
 */
public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this(material, 1);
    }

    public ItemBuilder(Material material, int amount) {
        this.item = new ItemStack(material, amount);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder(ItemStack item) {
        this.item = item.clone();
        this.meta = item.getItemMeta();
    }

    /**
     * 设置显示名称
     */
    public ItemBuilder setName(String name) {
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        return this;
    }

    /**
     * 设置Lore
     */
    public ItemBuilder setLore(String... lore) {
        List<String> coloredLore = new ArrayList<>();
        for (String line : lore) {
            coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        meta.setLore(coloredLore);
        return this;
    }

    /**
     * 设置Lore
     */
    public ItemBuilder setLore(List<String> lore) {
        List<String> coloredLore = new ArrayList<>();
        for (String line : lore) {
            coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        meta.setLore(coloredLore);
        return this;
    }

    /**
     * 添加Lore行
     */
    public ItemBuilder addLore(String... lines) {
        List<String> lore = meta.getLore() != null ? meta.getLore() : new ArrayList<>();
        for (String line : lines) {
            lore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        meta.setLore(lore);
        return this;
    }

    /**
     * 设置数量
     */
    public ItemBuilder setAmount(int amount) {
        item.setAmount(amount);
        return this;
    }

    /**
     * 设置耐久度
     */
    public ItemBuilder setDurability(short durability) {
        item.setDurability(durability);
        return this;
    }

    /**
     * 添加附魔
     */
    public ItemBuilder addEnchant(Enchantment enchantment, int level) {
        meta.addEnchant(enchantment, level, true);
        return this;
    }

    /**
     * 设置是否不可破坏
     */
    public ItemBuilder setUnbreakable(boolean unbreakable) {
        meta.setUnbreakable(unbreakable);
        return this;
    }

    /**
     * 隐藏所有附魔信息
     */
    public ItemBuilder hideEnchants() {
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        return this;
    }

    /**
     * 隐藏所有属性
     */
    public ItemBuilder hideAttributes() {
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        return this;
    }

    /**
     * 隐藏所有信息
     */
    public ItemBuilder hideAll() {
        meta.addItemFlags(ItemFlag.values());
        return this;
    }

    /**
     * 设置自定义模型数据
     */
    public ItemBuilder setCustomModelData(int data) {
        meta.setCustomModelData(data);
        return this;
    }

    /**
     * 设置头颅皮肤（玩家头颅）
     */
    public ItemBuilder setSkullOwner(String owner) {
        if (meta instanceof SkullMeta skullMeta) {
            skullMeta.setOwner(owner);
        }
        return this;
    }

    /**
     * 构建物品
     */
    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 创建占位符物品（玻璃板）
     */
    public static ItemStack createPlaceholder(Material material, String name) {
        return new ItemBuilder(material)
                .setName(name)
                .hideAll()
                .build();
    }

    /**
     * 创建边框物品
     */
    public static ItemStack createBorder() {
        return new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .setName("&7")
                .hideAll()
                .build();
    }

    /**
     * 创建关闭按钮
     */
    public static ItemStack createCloseButton() {
        return new ItemBuilder(Material.BARRIER)
                .setName("&c&l关闭")
                .setLore("&7点击关闭此界面")
                .build();
    }

    /**
     * 创建返回按钮
     */
    public static ItemStack createBackButton() {
        return new ItemBuilder(Material.ARROW)
                .setName("&e&l返回")
                .setLore("&7点击返回上一页")
                .build();
    }

    /**
     * 创建下一页按钮
     */
    public static ItemStack createNextButton() {
        return new ItemBuilder(Material.ARROW)
                .setName("&a&l下一页")
                .setLore("&7点击查看下一页")
                .build();
    }

    /**
     * 创建上一页按钮
     */
    public static ItemStack createPreviousButton() {
        return new ItemBuilder(Material.ARROW)
                .setName("&e&l上一页")
                .setLore("&7点击查看上一页")
                .build();
    }

    /**
     * 创建刷新按钮
     */
    public static ItemStack createRefreshButton() {
        return new ItemBuilder(Material.LIME_DYE)
                .setName("&a&l刷新")
                .setLore("&7点击刷新界面")
                .build();
    }
}