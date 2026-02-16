# Platform Guide

## Version Selection Guide

IronDiscipline is split into two versions. Choose the appropriate version based on your server environment.

### IronDiscipline (Standard)

| Item | Details |
|---|---|
| Server | PaperSpigot / Spigot 1.18+ |
| Rank Management | LuckPerms metadata |
| Required Dependency | LuckPerms |
| Folia | **Not supported** |

**Recommended for:**
- PaperSpigot users
- Already using LuckPerms
- Want to integrate permission management with ranks

### IronDiscipline-dev (Folia Edition)

| Item | Details |
|---|---|
| Server | Folia 1.18+ |
| Rank Management | Own DB (H2/MySQL) |
| Required Dependency | None |
| PaperSpigot | **Not supported** |

**Recommended for:**
- Folia users
- Want a lightweight setup without LuckPerms dependency
- Need high performance with multi-threaded support

## Migration: Standard → Folia Edition

Steps for migrating from PaperSpigot to Folia.

### 1. Prerequisites

- Folia server is ready
- Backup of standard version data is complete

### 2. Steps

1. **Backup data**
   ```bash
   cp -r plugins/IronDiscipline/ plugins/IronDiscipline_backup/
   ```

2. **Replace plugin**
   ```bash
   rm plugins/IronDiscipline.jar
   cp IronDiscipline-dev-latest.jar plugins/
   ```

3. **Migrate data from LuckPerms** (if using LuckPerms)
   - Temporarily keep LuckPerms in plugins
   - Run `/irondev migrate` after server starts
   - Remove LuckPerms after migration completes
   
   See [MIGRATION.md](../IronDiscipline-dev/docs/MIGRATION.md) for details

4. **Configuration**
   - `config.yml` structure is shared, can be used as-is
   - `ranks.meta_key` setting is not used in dev version (managed via DB)

5. **Verify**
   ```
   /irondev status
   ```

## FAQ

### Q: Can I use existing config files?
A: Yes. The `config.yml` structure is nearly identical. However, `ranks.meta_key` is only used in the standard version.

### Q: Can both versions run on the same server?
A: No. This is not recommended due to potential data inconsistencies.

### Q: Does the Discord bot work with both versions?
A: Yes. Discord integration features work equally in both versions.
