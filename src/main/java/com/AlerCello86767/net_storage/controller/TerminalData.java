package com.AlerCello86767.net_storage.controller;

import java.util.UUID;

/**
 * 终端数据结构
 */
public class TerminalData {
    
    /** 方块位置字符串 */
    public String location;
    
    /** 所属网络ID */
    public UUID networkId;
    
    /** 创建时间 */
    public java.sql.Timestamp createdAt;
    
    public TerminalData() {
    }
    
    public TerminalData(String location, UUID networkId) {
        this.location = location;
        this.networkId = networkId;
    }
}
