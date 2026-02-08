[🇺🇸 English](CROSS_PLAY_en_US.md) | [🇩🇪 Deutsch](CROSS_PLAY_de_DE.md) | [🇪🇸 Español](CROSS_PLAY_es_ES.md) | [🇨🇳 中文](CROSS_PLAY_zh_CN.md) | [🇯🇵 日本語](CROSS_PLAY_ja_JP.md)

# 基岩版 (跨平台) 指南

本服务器支持使用 **Geyser** 和 **Floodgate** 从基岩版（手机、Switch、PS4/5、Xbox）进行连接。

## 1. 所需插件
将以下两个插件放入 `plugins` 文件夹中。
（部署脚本 `scripts/gcp-startup.sh` 已包含下载命令，但请按照以下步骤进行手动安装）

- **Geyser**: 核心插件，用于翻译 Java 版和基岩版之间的通信。
- **Floodgate**: 验证插件，允许没有 Java 账号的基岩版玩家登录。

```bash
# 进入插件目录
cd /opt/minecraft/plugins

# 下载
curl -o Geyser-Spigot.jar -L "https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest/downloads/spigot"
curl -o Floodgate-Spigot.jar -L "https://download.geysermc.org/v2/projects/floodgate/versions/latest/builds/latest/downloads/spigot"

# 重启服务器
sudo systemctl restart minecraft
```

## 2. 开放端口 (UDP 19132)
与 Java 版的 `25565 (TCP)` 不同，基岩版使用 **`19132 (UDP)`**。
您需要在防火墙中开放此端口。

### 对于 GCP (在本地 PC 上运行)
```powershell
gcloud compute firewall-rules create geyser-port --allow udp:19132 --target-tags=minecraft-server
```

### 对于 Xserver VPS / ConoHa 等
在您的 VPS 控制面板的“防火墙”或“安全组”设置中添加以下规则。
- 协议: **UDP**
- 端口号: **19132**
- 来源: **全部 (0.0.0.0/0)**

## 3. 连接方法
- **服务器地址**: 与 Java 版 IP 地址相同
- **端口**: `19132` (默认)

## 注意: 关于皮肤
Floodgate 会自动将基岩版皮肤反映到 Java 版。
