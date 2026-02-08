[🇺🇸 English](README_en_US.md) | [🇩🇪 Deutsch](README_de_DE.md) | [🇪🇸 Español](README_es_ES.md) | [🇨🇳 中文](README_zh_CN.md) | [🇯🇵 日本語](README_ja_JP.md)

# IronDiscipline-dev (鉄の規律 - LuckPerms非依存版)

Minecraftサーバー用 総合管理・規律維持プラグイン。
軍隊・刑務所RPサーバー向けに設計されています。

> ⚡ **このバージョンはLuckPermsに依存しません！** 階級データは独自DBに保存されるため、より高速で互換性が高いです。

## 通常版との違い

| 項目 | 通常版 (IronDiscipline) | dev版 (IronDiscipline-dev) |
|---|---|---|
| 階級保存先 | LuckPermsメタデータ | 独自DB (H2/MySQL) |
| LuckPerms | 必須 | 不要（移行時のみオプション） |
| パフォーマンス | API経由 | 直接DB+キャッシュ |

## 機能

- **階級システム**: 階級による権限管理、config.ymlでの完全なカスタマイズが可能
- **PTS (Permission to Speak)**: 下士官の発言許可システム
- **Discord連携**: アカウント連携、ロール・ニックネーム同期
- **警告・処分システム**: 警告蓄積と自動処分
- **試験システム**: GUIを使用した昇進試験
- **データ移行**: `/irondev migrate` でLuckPermsからデータを簡単移行

## 必要要件

- Java 17+
- Paper / Spigot / Folia 1.18+
- MySQL, SQLite または H2 Database (デフォルト)

## インストール

1. [Releases](https://github.com/kaji11-jp/IronDiscipline-dev/releases) から最新のJARファイルをダウンロード
2. サーバーの `plugins` フォルダに配置
3. サーバーを起動
4. `plugins/IronDisciplineDev/config.yml` を必要に応じて編集

## 通常版からの移行

```
/irondev migrate
```

詳細は [移行ガイド](docs/MIGRATION.md) を参照してください。

## コマンド

### 🔧 dev版専用コマンド
| コマンド | 説明 | 権限 |
|---|---|---|
| `/irondev migrate` | LuckPermsからデータ移行 | `iron.admin` |
| `/irondev status` | ステータス表示 | `iron.admin` |

## ビルド

```bash
mvn clean package
```

## ライセンス

MIT License
