# SoftRouter - Android 软路由 APK

一键将安卓手机变成软路由，通过 Shizuku 提权执行系统命令，内嵌 Web 管理仪表盘。

## 功能特性

- **Shizuku 免 Root 提权**：通过 Shizuku API 执行 iptables/dnsmasq/ip 等系统命令
- **Root 自动回退**：如果 Shizuku 不可用，自动尝试 su 模式
- **内嵌 Web 管理后台**：NanoHTTPD 服务器监听 `0.0.0.0:8080`，局域网任意设备浏览器访问
- **暗色主题 SPA 仪表盘**：
  - 总览面板（运行状态、IP转发、客户端数、流量统计）
  - 防火墙规则管理（添加 iptables 规则）
  - DHCP 配置（地址池范围、租约时间、DNS）
  - 端口转发（源端口→目标IP:端口，TCP/UDP）
  - 在线客户端列表（ARP 表扫描）
  - 实时流量图表（Canvas 绘制，3秒刷新）
- **前台服务保活**：通知栏常驻，防止系统杀后台
- **Material Design 3**：暗色主题原生 Android UI

## 项目结构

```
soft-router-app/
├── build.gradle.kts              # 根构建脚本 (AGP 8.2.2 + Kotlin 1.9.22)
├── settings.gradle.kts           # 项目设置
├── gradle.properties             # Gradle 属性
├── README.md                     # 本文件
└── app/
    ├── build.gradle.kts          # App 模块依赖 (Shizuku, NanoHTTPD, Gson, Material3)
    └── src/main/
        ├── AndroidManifest.xml   # 权限声明 + ShizukuProvider + Service
        ├── assets/dashboard/
        │   └── index.html        # Web 管理仪表盘 (单文件 SPA)
        ├── java/com/softrouter/app/
        │   ├── MainActivity.kt         # 主界面 (启停控制、Shizuku 状态、日志)
        │   ├── RouterService.kt        # 前台服务 (启动 Web 服务器)
        │   ├── ShizukuHelper.kt        # Shizuku/Root 双通道命令执行
        │   └── WebDashboardServer.kt   # NanoHTTPD REST API 服务器
        └── res/
            ├── layout/activity_main.xml  # Material3 布局
            └── values/{strings,themes}.xml
```

## 构建方法

### 前提条件
- JDK 17+
- Android SDK (compileSdk 34)
- Android Studio Hedgehog (2023.1.1) 或更高版本

### 方式一：Android Studio
1. 将整个 `soft-router-app/` 目录复制到电脑
2. 用 Android Studio 打开该目录
3. 等待 Gradle Sync 完成
4. 点击 Build → Build Bundle(s) / APK(s) → Build APK(s)
5. APK 生成在 `app/build/outputs/apk/debug/app-debug.apk`

### 方式二：命令行
```bash
cd soft-router-app

# 如果没有 gradlew，先生成 wrapper
gradle wrapper --gradle-version 8.5

# 构建 Debug APK
./gradlew assembleDebug

# APK 位置
ls app/build/outputs/apk/debug/
```

## 使用方法

### 安装前准备
1. 在手机上安装 [Shizuku](https://shizuku.rikka.app/) 并启动（通过无线调试或 Root）
2. 安装本 APK

### 操作步骤
1. 打开 SoftRouter App
2. 等待 Shizuku 连接成功（状态显示 ✅）
3. 点击「启动路由」按钮或拨动开关
4. 管理后台地址会显示在界面上（如 `http://192.168.1.x:8080`）
5. 在同一局域网内的任意设备浏览器中打开该地址即可进入管理仪表盘

### 管理后台功能
| 标签页 | 功能 |
|--------|------|
| 总览 | 查看运行状态、一键启停、IP转发状态、客户端数、流量统计 |
| 防火墙 | 添加自定义 iptables 规则，查看已有规则 |
| DHCP | 配置地址池范围、租约时间、DNS 服务器 |
| 端口转发 | 添加端口映射规则（支持 TCP/UDP） |
| 客户端 | 查看当前 ARP 表中的在线设备及 MAC 地址 |
| 流量 | Canvas 实时流量图表（RX/TX 双线） |

## API 接口

Web 服务器提供以下 REST API：

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/` | 返回仪表盘 HTML |
| GET | `/api/status` | 获取路由器状态 JSON |
| POST | `/api/start` | 启动路由（开启 IP 转发 + NAT + dnsmasq） |
| POST | `/api/stop` | 停止路由（清理规则 + 关闭 dnsmasq） |
| GET/POST | `/api/firewall` | 查看/添加防火墙规则 |
| GET/POST | `/api/portforward` | 查看/添加端口转发 |
| GET/POST | `/api/dhcp` | 查看/修改 DHCP 配置 |
| GET | `/api/clients` | 获取在线客户端列表 |

## 注意事项

- 需要手机已连接 WiFi 或移动数据作为上游网络
- 下游设备需连接到手机的热点（wlan1/tether 接口）
- dnsmasq 需要在系统中可用（部分 ROM 内置，否则需自行 push 到 /data/local/tmp/）
- Shizuku 模式下某些内核参数可能受限，建议使用 Root 模式获得完整功能
- 前台服务会在通知栏显示运行状态，可在通知中直接停止
