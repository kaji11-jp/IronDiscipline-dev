[🇺🇸 English](README_en_US.md) | [🇩🇪 Deutsch](README_de_DE.md) | [🇪🇸 Español](README_es_ES.md) | [🇨🇳 中文](README_zh_CN.md) | [🇯🇵 日本語](README_ja_JP.md)

# IronDiscipline-dev (Eiserne Disziplin - LuckPerms Unabhängig)

Umfassendes Verwaltungs- und Disziplin-Plugin für Minecraft-Server.
Entwickelt für Militär- und Gefängnis-RP-Server.

> ⚡ **Diese Version benötigt KEIN LuckPerms!** Rangdaten werden in einer eigenen Datenbank gespeichert für bessere Leistung und Kompatibilität.

## Unterschiede zur Standardversion

| Element | Standard (IronDiscipline) | Dev (IronDiscipline-dev) |
|---|---|---|
| Rang-Speicherung | LuckPerms Metadaten | Eigene DB (H2/MySQL) |
| LuckPerms | Erforderlich | Nicht erforderlich (optional für Migration) |
| Leistung | Über API | Direkte DB + Cache |
| Nebenläufigkeit | Standard | Thread-sichere parallele Verarbeitung |
| Folia-Unterstützung | Nicht unterstützt | Vollständig unterstützt |

## Funktionen

- **Rangsystem**: Berechtigungsverwaltung nach Rang
  - Thread-sicherer paralleler Cache (`ConcurrentHashMap`)
  - Schutz vor Race-Conditions
- **PTS (Permission to Speak)**: Sprecherlaubnis-System
- **Discord-Integration**: Kontoverknüpfung, Rollensynchronisation
- **Warnsystem**: Verwarnungen mit automatischer Bestrafung
  - Verhinderung doppelter Inhaftierungen
  - Sofortige Inventar-Sicherung zur Vermeidung von Gegenstandsverlust
  - Automatische Erkennung und Reparatur von Dateninkonsistenzen
- **Prüfungssystem**: Beförderungsprüfungen mit GUI
- **Datenmigration**: Einfache Migration von LuckPerms mit `/irondev migrate`
- **Folia-Unterstützung**: Vollständige Folia-Kompatibilität über MorePaperLib

## Anforderungen

- Java 17+
- Paper / Spigot / Folia 1.18+ (Vollständige Folia-Unterstützung)
- MySQL, SQLite oder H2 Database (Standard)

## Installation

1. Neueste JAR von [Releases](https://github.com/kaji11-jp/IronDiscipline-dev/releases) herunterladen
2. In den `plugins`-Ordner des Servers legen
3. Server starten
4. `plugins/IronDisciplineDev/config.yml` nach Bedarf bearbeiten

## Migration von Standardversion

```
/irondev migrate
```

## Befehle

### 🔧 Dev-Version Befehle
| Befehl | Beschreibung | Berechtigung |
|---|---|---|
| `/irondev migrate` | Daten von LuckPerms migrieren | `iron.admin` |
| `/irondev status` | Status anzeigen | `iron.admin` |

## Build

```bash
mvn clean package
```

## Lizenz

MIT License
