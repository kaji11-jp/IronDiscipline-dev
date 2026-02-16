[🇺🇸 English](README_en_US.md) | [🇩🇪 Deutsch](README_de_DE.md) | [🇪🇸 Español](README_es_ES.md) | [🇨🇳 中文](README_zh_CN.md) | [🇯🇵 日本語](README_ja_JP.md)

# IronDiscipline-dev (Iron Discipline - Folia Edition)

Comprehensive management and discipline plugin for Minecraft servers.
Designed for military and prison RP servers.

> ⚡ **This version is Folia-exclusive!** Rank data is stored in a dedicated database. LuckPerms is not required.
> For PaperSpigot, please use [IronDiscipline](https://github.com/kaji11-jp/IronDiscipline).

## Differences from Standard Version

| Item | Standard (IronDiscipline) | Dev (IronDiscipline-dev) |
|---|---|---|
| Server | PaperSpigot 1.18+ | Folia 1.18+ |
| Rank Storage | LuckPerms metadata | Own DB (H2/MySQL) |
| LuckPerms | Required | Not required (optional for migration) |
| Performance | Via API | Direct DB + Cache |
| Concurrency | Standard | Thread-safe concurrent processing |
| Folia Support | Not supported | Fully supported (exclusive) |

## Features

- **Rank System**: Permission management by rank, fully customizable in config.yml
  - Thread-safe concurrent cache (`ConcurrentHashMap`)
  - Race condition protection
- **PTS (Permission to Speak)**: Speaking permission system for lower ranks
- **Discord Integration**:
  - Account linking (`/link`)
  - Role & nickname sync
  - Announcement system
  - Server status display
- **Warning System**:
  - Accumulate warnings with `/warn`
  - Auto-jail/kick at threshold
  - `/jail` isolation system (DB saved)
  - Duplicate jail prevention
  - Instant inventory backup to prevent item loss
  - Auto-detection and repair of data inconsistencies
- **Exam System**: Promotion exams using GUI
- **Playtime Management**: Online time tracking
- **Message Customization**: Most in-game messages can be changed
- **Data Migration**: Easy migration from LuckPerms with `/irondev migrate`
- **Folia Exclusive**: Native Folia scheduling via MorePaperLib

## Requirements

- Java 17+
- Folia 1.18+ (**PaperSpigot not supported**)
- MySQL, SQLite or H2 Database (default)

## Installation

1. Download the latest JAR from [Releases](https://github.com/kaji11-jp/IronDiscipline-dev/releases)
2. Place in your server's `plugins` folder
3. Start the server
4. Edit `plugins/IronDisciplineDev/config.yml` as needed
5. Restart server or use `/iron reload`

## Migration from Standard Version

To migrate data from LuckPerms version:

1. Keep LuckPerms plugin in plugins folder alongside this plugin
2. Start server, then run:
   ```
   /irondev migrate
   ```
3. After migration completes, LuckPerms can be removed

## Commands

### 🌐 General Commands
| Command | Description | Permission |
|---|---|---|
| `/link [code]` | Discord account linking | None |
| `/playtime [top]` | Display playtime | `iron.playtime.view` |
| `/radio <frequency>` | Join/leave radio channel | `iron.radio.use` |
| `/warnings [player]` | View warning history | `iron.warn.view` |

### 👮 Discipline Commands
| Command | Description | Permission |
|---|---|---|
| `/warn <player> <reason>` | Issue warning | `iron.warn.use` |
| `/jail <player> [reason]` | Force isolate player | `iron.jail.use` |
| `/unjail <player>` | Release player | `iron.jail.use` |
| `/promote <player>` | Promote rank | `iron.rank.promote` |
| `/demote <player>` | Demote rank | `iron.rank.demote` |

### 🔧 Dev Version Commands
| Command | Description | Permission |
|---|---|---|
| `/irondev migrate` | Migrate data from LuckPerms | `iron.admin` |
| `/irondev status` | Show status | `iron.admin` |

## Build

```bash
mvn clean package
```

## License

MIT License
