[🇺🇸 English](VPS_DEPLOY_en_US.md) | [🇩🇪 Deutsch](VPS_DEPLOY_de_DE.md) | [🇪🇸 Español](VPS_DEPLOY_es_ES.md) | [🇨🇳 中文](VPS_DEPLOY_zh_CN.md) | [🇯🇵 日本語](VPS_DEPLOY_ja_JP.md)

# Allgemeiner VPS Bereitstellungsleitfaden

> ⚡ **Folia Exklusiv** - Militär/Gefängnis-RP-Plugin für Folia-Server

## Unterstützte VPS
- **Hetzner** (Deutschland - schnell)
- **Linode / DigitalOcean / Vultr** (International)

## 1. Server-Vorbereitung

- **OS**: Ubuntu 22.04 LTS
- **CPU**: 2+ Kerne
- **RAM**: 4GB+ (8GB empfohlen)

## 2. Setup-Skript ausführen

```bash
ssh root@<server-ip>
curl -O https://raw.githubusercontent.com/kaji11-jp/IronDiscipline-dev/main/scripts/setup-ubuntu.sh
sudo bash setup-ubuntu.sh
```

## 3. Plugin hochladen

```bash
scp target/IronDiscipline-dev-*.jar root@<server-ip>:/opt/minecraft/plugins/
```

## 4. Automatisches Update

```bash
curl -sL https://raw.githubusercontent.com/kaji11-jp/IronDiscipline-dev/main/scripts/update-server.sh | sudo bash
```

## Migration

Siehe [Migrationsleitfaden](MIGRATION_en_US.md).
