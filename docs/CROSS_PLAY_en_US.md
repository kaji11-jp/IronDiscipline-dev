[🇺🇸 English](CROSS_PLAY_en_US.md) | [🇩🇪 Deutsch](CROSS_PLAY_de_DE.md) | [🇪🇸 Español](CROSS_PLAY_es_ES.md) | [🇨🇳 中文](CROSS_PLAY_zh_CN.md) | [🇯🇵 日本語](CROSS_PLAY_ja_JP.md)

# Bedrock (Cross-Play) Guide

This server supports connections from Bedrock Edition (Mobile, Switch, PS4/5, Xbox) using **Geyser** and **Floodgate**.

## 1. Required Plugins
Place the following two plugins in the `plugins` folder.
(The deployment script `scripts/gcp-startup.sh` already includes download commands, but follow these steps for manual installation)

- **Geyser**: Core plugin that translates communication between Java and Bedrock editions.
- **Floodgate**: Authentication plugin that allows Bedrock players without a Java account to log in.

```bash
# Move to plugins directory
cd /opt/minecraft/plugins

# Download
curl -o Geyser-Spigot.jar -L "https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest/downloads/spigot"
curl -o Floodgate-Spigot.jar -L "https://download.geysermc.org/v2/projects/floodgate/versions/latest/builds/latest/downloads/spigot"

# Restart server
sudo systemctl restart minecraft
```

## 2. Open Port (UDP 19132)
Bedrock Edition uses **`19132 (UDP)`**, unlike Java Edition's `25565 (TCP)`.
You need to open this port in your firewall.

### For GCP (Run on your local PC)
```powershell
gcloud compute firewall-rules create geyser-port --allow udp:19132 --target-tags=minecraft-server
```

### For Xserver VPS / ConoHa, etc.
Add the following rule in the "Firewall" or "Security Group" settings in your VPS control panel.
- Protocol: **UDP**
- Port Number: **19132**
- Source: **All (0.0.0.0/0)**

## 3. Connection Method
- **Server Address**: Same IP address as Java Edition
- **Port**: `19132` (Default)

## Note: About Skins
Floodgate automatically reflects Bedrock skins to Java Edition.
