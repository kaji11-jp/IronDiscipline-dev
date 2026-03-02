[🇺🇸 English](MIGRATION_en_US.md) | [🇯🇵 日本語](MIGRATION.md)

# Migration Guide from LuckPerms

> ⚠️ **Note**: The old version (Paper+LuckPerms `IronDiscipline`) has been discontinued.
> This guide is for migrating from the old version.

Guide for migrating data from the old IronDiscipline version (LuckPerms dependent) to IronDiscipline-dev (Folia exclusive).

## Overview

IronDiscipline-dev stores rank data in its own database (H2/MySQL).
The standard version stored this in LuckPerms metadata, so migration is required.

## Migration Steps

### 1. Preparation

1. Stop the server
2. Remove `plugins/IronDiscipline.jar`
3. Add `plugins/IronDiscipline-dev-latest.jar`
4. **Important**: Do NOT remove LuckPerms yet (needed for migration)

### 2. Start Server

```bash
./start.sh
```

### 3. Run Migration Command

Execute from server console or as an admin player:

```
/irondev migrate
```

Output will look like:

```
Target users: 150
Progress: 10% (15/150)
Progress: 20% (30/150)
...
===========================
Migration complete!
Migrated users: 142
===========================
```

### 4. Verify

```
/irondev status
```

Shows the count of migrated data.

### 5. Remove LuckPerms (Optional)

After confirming successful migration, LuckPerms can be removed:

```bash
rm plugins/LuckPerms*.jar
```

## Troubleshooting

### "LuckPerms not found" Error

Ensure LuckPerms plugin is present in the plugins folder.
Do not remove LuckPerms until migration is complete.

### Some Users Not Migrated

Users without rank metadata (default: `rank`) in LuckPerms are skipped.
These users will automatically be registered as PRIVATE on first login.
