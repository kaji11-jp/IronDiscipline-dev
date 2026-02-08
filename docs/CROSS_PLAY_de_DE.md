[🇺🇸 English](CROSS_PLAY_en_US.md) | [🇩🇪 Deutsch](CROSS_PLAY_de_DE.md) | [🇪🇸 Español](CROSS_PLAY_es_ES.md) | [🇨🇳 中文](CROSS_PLAY_zh_CN.md) | [🇯🇵 日本語](CROSS_PLAY_ja_JP.md)

# Bedrock (Cross-Play) Leitfaden

Dieser Server unterstützt Verbindungen von der Bedrock Edition (Handy, Switch, PS4/5, Xbox) unter Verwendung von **Geyser** und **Floodgate**.

## 1. Erforderliche Plugins
Legen Sie die folgenden zwei Plugins in den `plugins`-Ordner.
(Das Bereitstellungsskript `scripts/gcp-startup.sh` enthält bereits Download-Befehle, aber führen Sie dies für eine manuelle Installation durch)

- **Geyser**: Kern-Plugin zur Übersetzung der Kommunikation zwischen Java- und Bedrock-Edition.
- **Floodgate**: Authentifizierungs-Plugin, das Bedrock-Spielern ohne Java-Konto das Einloggen ermöglicht.

```bash
# Zum Plugins-Verzeichnis wechseln
cd /opt/minecraft/plugins

# Herunterladen
curl -o Geyser-Spigot.jar -L "https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest/downloads/spigot"
curl -o Floodgate-Spigot.jar -L "https://download.geysermc.org/v2/projects/floodgate/versions/latest/builds/latest/downloads/spigot"

# Server neu starten
sudo systemctl restart minecraft
```

## 2. Portfreigabe (UDP 19132)
Die Bedrock Edition verwendet **`19132 (UDP)`**, im Gegensatz zu `25565 (TCP)` der Java Edition.
Sie müssen diesen Port in Ihrer Firewall öffnen.

### Für GCP (Auf Ihrem lokalen PC ausführen)
```powershell
gcloud compute firewall-rules create geyser-port --allow udp:19132 --target-tags=minecraft-server
```

### Für Xserver VPS / ConoHa usw.
Fügen Sie die folgende Regel in den "Firewall"- oder "Sicherheitsgruppen"-Einstellungen Ihres VPS-Panels hinzu.
- Protokoll: **UDP**
- Portnummer: **19132**
- Quelle: **Alle (0.0.0.0/0)**

## 3. Verbindungsmethode
- **Serveradresse**: Gleiche IP-Adresse wie Java Edition
- **Port**: `19132` (Standard)

## Hinweis: Zu Skins
Floodgate überträgt Bedrock-Skins automatisch auf die Java Edition.
