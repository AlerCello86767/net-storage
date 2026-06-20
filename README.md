# NetStorage

类似 AE2 (Applied Energistics 2) 的 Minecraft 存储网络插件，为 Paper 1.21.1 服务器提供现代化的网络存储解决方案。

## 功能特性

### 核心系统
- **网络控制器** - 网络核心，管理所有连接设备
- **磁盘操纵器** - 存储磁盘的容器，最多支持 8 个磁盘
- **终端** - 统一的物品存取界面，支持搜索和分类
- **磁盘** - 1K 容量的存储介质，可扩展

### 外部存储总线
- **外部存储总线** - 将原版容器（箱子、漏斗、熔炉等）连接到网络
- 支持多种容器类型：箱子、陷阱箱、末影箱、漏斗、熔炉、高炉、烟熏炉、发射器、投掷器、潜影盒
- 自动聚合容器物品到网络存储
- 智能存取逻辑：磁盘优先，容器次之

### 智能物品管理
- 自动堆叠相同物品
- NBT 数据完整保留（附魔、名称、耐久度等）
- 实时容量计算和显示
- 物品搜索和筛选

### 网络管理
- 多网络支持（每个玩家最多 5 个网络）
- 公开/私有网络设置
- 网络描述和重命名
- 权限管理

## 安装方法

### 前置要求
- Paper 1.21.1 服务器
- Java 21+

### 安装步骤
1. 从 Releases 下载最新版本的插件 jar 文件
2. 将 jar 文件放入服务器 `plugins` 目录
3. 重启服务器
4. 插件会自动生成配置文件和数据库

### 编译方法
```bash
# 克隆项目
git clone https://github.com/AlerCello86767/net-storage.git
cd net-storage

# 使用 Gradle 编译
./gradlew build

# 生成的 jar 文件位于 build/libs/ 目录
```

## 使用指南

### 基础设备获取
使用调试命令获取设备：
```
/netdebug give controller      # 获取网络控制器
/netdebug give disk_manipulator # 获取磁盘操纵器
/netdebug give terminal         # 获取终端
/netdebug give disk_1k          # 获取 1K 磁盘
/netdebug give external_storage_bus # 获取外部存储总线
/netdebug give connect_tool     # 获取连接工具
```

### 网络搭建流程

#### 1. 创建网络
放置网络控制器会自动创建网络：
- 网络名称：`控制器网络-玩家名`
- 自动生成唯一网络 ID

#### 2. 连接设备
使用连接工具连接设备到网络：
- 左键点击控制器选择网络
- 右键点击设备（磁盘操纵器/终端/外部存储总线）进行连接

#### 3. 插入磁盘
打开磁盘操纵器 GUI：
- 点击空槽位放入磁盘
- 点击磁盘槽位取出磁盘
- 最多支持 8 个磁盘

#### 4. 使用终端
打开终端 GUI：
- 查看所有磁盘和外部容器物品
- 左键/右键点击取出 1 个物品
- Shift+点击取出全部物品
- 从玩家背包存入物品到网络

#### 5. 外部存储总线
放置外部存储总线：
- 底座对准容器放置（末地烛底座方向）
- 使用连接工具连接到网络
- 终端会自动显示容器物品

### 物品存取逻辑

#### 存入物品
1. 打开终端，点击玩家背包物品
2. 优先存入磁盘（自动堆叠相同物品）
3. 磁盘满后存入外部容器
4. 全满时提示空间不足

#### 取出物品
1. 在终端点击物品
2. 优先从磁盘取出
3. 磁盘不足时从外部容器取出
4. NBT 数据完整保留

## 命令列表

### 网络管理命令
```
/network create <名称>         # 创建新网络
/network list                  # 列出所有网络
/network mynetworks            # 列出我的网络
/network info <网络名>         # 查看网络详情
/network delete <网络名> confirm # 删除网络
/network rename <网络名> <新名称> # 重命名网络
/network setpublic <网络名> <true|false> # 设置公开/私有
/network setdesc <网络名> <描述> # 设置网络描述
/network help                  # 显示帮助
```

### 调试命令（管理员）
```
/netdebug give <类型> [数量]   # 获取设备物品
/disktest add <磁盘UUID> <物品> <数量> # 添加测试物品
/disktest remove <磁盘UUID> <物品> <数量> # 移除物品
/disktest clean <磁盘UUID>     # 清空磁盘
/disktest check <磁盘UUID>     # 检查磁盘数据
/testgui                       # 打开测试 GUI
```

## 权限说明

### 基础权限
- `netstorage.use` - 使用基础命令（默认：所有玩家）
- `netstorage.create` - 创建网络（默认：OP）
- `netstorage.delete` - 删除网络（默认：OP）
- `netstorage.rename` - 重命名网络（默认：OP）

### 管理权限
- `netstorage.admin` - 所有管理权限（默认：OP）
  - 包含所有上述权限
  - 可使用调试命令

## 配置说明

### 配置文件位置
`plugins/NetStorage/config.yml`

### 主要配置项

#### 数据库设置
```yaml
database:
  type: "h2"          # 数据库类型（H2）
  h2:
    file: "storage"   # 数据库文件名
```

#### 网络限制
```yaml
network:
  max-networks-per-player: 5   # 每个玩家最多网络数
  max-nodes-per-network: 50    # 每个网络最多节点数
  max-item-types-per-network: 1000 # 每个网络最多物品种类
```

#### 磁盘容量
```yaml
disk:
  max-capacity: 1024  # 磁盘最大容量（物品数）
  manipulator:
    max-disks: 8      # 磁盘操纵器最多磁盘数
```

#### 性能优化
```yaml
performance:
  save-interval: 300  # 数据保存间隔（秒）
  async-operations: true # 启用异步操作
```

## 技术架构

### 系统层级
```
终端 → 网络 → 磁盘操纵器 → 磁盘
      ↓
外部存储总线 → 原版容器
```

### 数据存储
- **H2 数据库** - 持久化存储所有数据
- **内存缓存** - 磁盘数据缓存，减少数据库查询
- **异步保存** - 定时批量保存，优化性能

### 主要组件

#### 网络层
- `StorageNetwork` - 网络数据模型
- `NetworkManager` - 网络管理器

#### 控制器层
- `ControllerManager` - 控制器和设备管理
- `ControllerListener` - 方块放置/破坏监听
- `ConnectToolListener` - 连接工具交互

#### 磁盘层
- `DiskManager` - 磁盘数据管理
- `DiskItem` - 物品数据模型（支持 NBT）

#### GUI 层
- `BaseGUI` - GUI 基类
- `TerminalGUI` - 终端界面
- `DiskManipulatorGUI` - 磁盘操纵器界面
- `ControllerGUI` - 控制器界面

#### 外部存储总线
- `ExternalStorageBusData` - 总线数据模型（UUID 标识）
- 容器检测和绑定
- 物品聚合和存取

## 数据库表结构

### networks 表
- 网络基本信息（ID、名称、创建者、公开状态）

### controllers 表
- 控制器位置和网络关联

### disk_manipulators 表
- 磁盘操纵器位置、网络关联、磁盘槽位

### terminals 表
- 终端位置和网络关联

### disks 表
- 磁盘 UUID、容量、物品数据

### external_storage_buses 表
- 总线 UUID、位置、网络关联、容器位置、容器类型

## 性能优化

### 已实现优化
- 磁盘数据缓存（减少数据库查询）
- 异步批量保存（定时保存磁盘数据）
- GUI 增量更新（只更新变化部分）
- 物品匹配优化（使用序列化字符串匹配）

### 性能建议
- 控制网络规模（避免过多磁盘）
- 定期清理无用网络
- 使用外部存储总线分散存储压力

## 开发信息

### 技术栈
- **Java 21** - 编程语言
- **Paper API 1.21.1** - Minecraft 服务器 API
- **H2 Database** - 嵌入式数据库
- **Gradle** - 构建工具
- **Shadow Plugin** - 依赖打包

### 项目结构
```
net-storage/
├── src/main/java/com/AlerCello86767/net_storage/
│   ├── commands/        # 命令处理
│   ├── controller/      # 控制器和设备管理
│   ├── disk/            # 磁盘管理
│   ├── gui/             # GUI 界面
│   ├── network/         # 网络管理
│   ├── utils/           # 工具类
│   └── Net_storage.java # 主类
├── src/main/resources/
│   ├── plugin.yml       # 插件描述
│   └ config.yml         # 配置文件
└── build.gradle.kts     # 构建配置
```

### API 版本
- Paper API 1.21.1-R0.1-SNAPSHOT

### 作者
- AlerCello86767

## 更新日志

### v1.0.0
- 初始版本发布
- 完整的网络存储系统
- 磁盘和终端功能
- 外部存储总线集成
- UUID 标识系统

## 许可证

本项目采用 MIT 许可证。

## 联系方式

如有问题或建议，请通过以下方式联系：
- GitHub Issues
- Minecraft 服务器内反馈

## 致谢

感谢 Applied Energistics 2 模组提供的灵感和技术参考。