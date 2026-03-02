# Platform Guide

[🇯🇵 日本語](PLATFORM_GUIDE.md)

## About IronDiscipline-dev

IronDiscipline-dev is a **Folia-exclusive** military/prison RP plugin.

| Item | Details |
|---|---|
| Server | Folia 1.20.4 |
| Rank Management | Own DB (H2/MySQL) |
| Required Dependency | None (no LuckPerms) |
| Thread Model | Folia region/entity schedulers |

### Key Features

- **Rank System**: 10 ranks from PRIVATE to COMMANDER, own DB management
- **Jail System**: Jail/unjail with inventory preservation
- **Exam System**: Promotion exam creation and grading
- **Discord Integration**: JDA 5 Bot for notifications and rank sync
- **Web Dashboard**: Embedded HTTP server for Discord Bot management
- **Radio System**: Channel-based radio communication
- **Multi-language**: ja_JP / en_US / de_DE / es_ES / zh_CN

### Server Requirements

- **Java**: 21+
- **Server**: Folia 1.20.4
- **Memory**: 4GB+ (8GB recommended)
- **OS**: Ubuntu 22.04 LTS / 24.04 LTS (recommended)

## Migration from Old Paper+LuckPerms Version

> ⚠️ **Note**: The old version (Paper+LuckPerms `IronDiscipline`) has been discontinued.
> Only the Folia-exclusive `IronDiscipline-dev` is actively maintained.

### 1. Prerequisites

- Folia server is ready
- Backup of old version data is complete

### 2. Steps

1. **Backup data**
   ```bash
   cp -r plugins/IronDiscipline/ plugins/IronDiscipline_backup/
   ```

2. **Replace plugin**
   ```bash
   rm plugins/IronDiscipline.jar
   cp IronDiscipline-dev-2.0.0-dev.jar plugins/
   ```

3. **Migrate data from LuckPerms** (if using LuckPerms in old version)
   - Temporarily keep LuckPerms in plugins
   - Run `/irondev migrate` after server starts
   - Remove LuckPerms after migration completes
   
   See [MIGRATION_en_US.md](MIGRATION_en_US.md) for details

4. **Configuration**
   - `config.yml` structure is mostly the same, can be used as-is
   - `ranks.meta_key` setting is not needed (managed via DB)

5. **Verify**
   ```
   /irondev status
   ```

## Deployment Guides

- [Generic VPS Deployment](VPS_DEPLOY_en_US.md) — Xserver, ConoHa, Linode, etc.
- [Google Cloud Platform](GCP_DEPLOY_en_US.md) — GCE + GCS
- [Docker](../docker-compose.yml) — Docker Compose

## FAQ

### Q: Is LuckPerms required?
A: No. Rank data is managed in an own database (H2/MySQL), so LuckPerms is not needed.

### Q: Does it run on Paper/Spigot?
A: No. This plugin is Folia-exclusive. It depends on Folia's region scheduler and will not work on Paper/Spigot.

### Q: Where do I configure the Discord bot?
A: In the `discord` section of `config.yml`. Settings can also be changed via the Web Dashboard.

### Q: Which database should I use?
A: H2 (default, zero-config) for small servers, MySQL for larger servers.
