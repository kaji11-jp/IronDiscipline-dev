[🇺🇸 English](VPS_DEPLOY_en_US.md) | [🇩🇪 Deutsch](VPS_DEPLOY_de_DE.md) | [🇪🇸 Español](VPS_DEPLOY_es_ES.md) | [🇨🇳 中文](VPS_DEPLOY_zh_CN.md) | [🇯🇵 日本語](VPS_DEPLOY_ja_JP.md)

# Generic VPS Deployment Guide (Xserver, ConoHa, Linode, etc.)

> ⚡ **LuckPerms Independent Version** - No LuckPerms installation required

This project can easily run on common VPS providers other than Google Cloud Platform.

## Supported VPS Examples
- **Xserver VPS** (Japan - fast & stable)
- **ConoHa VPS** (Japan - user-friendly)
- **Linode / DigitalOcean / Vultr** (International - affordable)

## 1. Server Preparation

### Recommended Specs
- **OS**: Ubuntu 22.04 LTS or 24.04 LTS
- **CPU**: 2+ cores
- **RAM**: 4GB+ (8GB recommended)

### Steps
1. Create an instance from your VPS control panel
2. Select **Ubuntu** as the OS
3. Set `root` password or register SSH key

## 2. Run Setup Script

SSH into your server and run:

```bash
# 1. SSH connect
ssh root@<server-ip>

# 2. Download and run setup script
curl -O https://raw.githubusercontent.com/kaji11-jp/IronDiscipline-dev/main/scripts/setup-ubuntu.sh
sudo bash setup-ubuntu.sh
```

## 3. Upload Plugin

### Build & Upload
```bash
# Build locally
mvn clean package

# Upload (No LuckPerms needed!)
scp target/IronDiscipline-dev-*.jar root@<server-ip>:/opt/minecraft/plugins/
```

Restart server:
```bash
ssh root@<server-ip> "systemctl restart minecraft"
```

## 4. Auto Update

Update to latest version with one command:

```bash
curl -sL https://raw.githubusercontent.com/kaji11-jp/IronDiscipline-dev/main/scripts/update-server.sh | sudo bash
```

## 5. Migration from Standard Version

See [Migration Guide](MIGRATION_en_US.md) for migrating from LuckPerms version.

## 6. Port Forwarding

**Required Ports:**
- TCP: `25565` (Java Edition)
- UDP: `19132` (Bedrock - with Geyser)

## 7. Discord Bot Setup

1. Edit config:
```bash
nano /opt/minecraft/plugins/IronDisciplineDev/config.yml
```
2. Enter `bot_token` etc. and save
3. Restart: `systemctl restart minecraft`
