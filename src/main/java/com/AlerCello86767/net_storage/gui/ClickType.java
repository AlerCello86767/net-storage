package com.AlerCello86767.net_storage.gui;

/**
 * GUI点击类型枚举
 */
public enum ClickType {
    LEFT_CLICK,          // 左键点击
    RIGHT_CLICK,         // 右键点击
    SHIFT_LEFT_CLICK,    // Shift+左键
    SHIFT_RIGHT_CLICK,   // Shift+右键
    MIDDLE_CLICK,        // 中键点击
    DOUBLE_CLICK,        // 双击
    DROP,                // Q键丢弃
    CONTROL_DROP,        // Ctrl+Q丢弃整组
    NUMBER_KEY,          // 数字键
    UNKNOWN;             // 未知

    /**
     * 判断是否为Shift点击
     */
    public boolean isShiftClick() {
        return this == SHIFT_LEFT_CLICK || this == SHIFT_RIGHT_CLICK;
    }

    /**
     * 判断是否为左键相关
     */
    public boolean isLeftClick() {
        return this == LEFT_CLICK || this == SHIFT_LEFT_CLICK;
    }

    /**
     * 判断是否为右键相关
     */
    public boolean isRightClick() {
        return this == RIGHT_CLICK || this == SHIFT_RIGHT_CLICK;
    }
}