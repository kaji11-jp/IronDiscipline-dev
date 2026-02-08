[🇺🇸 English](VPS_DEPLOY_en_US.md) | [🇩🇪 Deutsch](VPS_DEPLOY_de_DE.md) | [🇪🇸 Español](VPS_DEPLOY_es_ES.md) | [🇨🇳 中文](VPS_DEPLOY_zh_CN.md) | [🇯🇵 日本語](VPS_DEPLOY_ja_JP.md)

# Guía de Implementación en VPS Genérico

> ⚡ **Versión Independiente de LuckPerms** - No requiere instalación de LuckPerms

## VPS Compatibles
- **Linode / DigitalOcean / Vultr** (Internacional)
- **OVH** (Europa)

## 1. Preparación del Servidor

- **OS**: Ubuntu 22.04 LTS
- **CPU**: 2+ núcleos
- **RAM**: 4GB+ (8GB recomendado)

## 2. Ejecutar Script de Configuración

```bash
ssh root@<ip-servidor>
curl -O https://raw.githubusercontent.com/kaji11-jp/IronDiscipline-dev/main/scripts/setup-ubuntu.sh
sudo bash setup-ubuntu.sh
```

## 3. Subir Plugin

```bash
scp target/IronDiscipline-dev-*.jar root@<ip-servidor>:/opt/minecraft/plugins/
```

## 4. Actualización Automática

```bash
curl -sL https://raw.githubusercontent.com/kaji11-jp/IronDiscipline-dev/main/scripts/update-server.sh | sudo bash
```

## Migración

Ver [Guía de Migración](MIGRATION_en_US.md).
