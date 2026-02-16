[🇺🇸 English](README_en_US.md) | [🇩🇪 Deutsch](README_de_DE.md) | [🇪🇸 Español](README_es_ES.md) | [🇨🇳 中文](README_zh_CN.md) | [🇯🇵 日本語](README_ja_JP.md)

# IronDiscipline-dev (鉄の規律 - Folia専用版)

Minecraftサーバー用 総合管理・規律維持プラグイン。
軍隊・刑務所RPサーバー向けに設計されています。

> ⚡ **このバージョンは Folia 専用です！** 階級データは独自DBに保存され、LuckPermsに依存しません。
> PaperSpigotを使用する場合は [IronDiscipline](https://github.com/kaji11-jp/IronDiscipline) をご利用ください。

## 通常版との違い

| 項目 | 通常版 (IronDiscipline) | dev版 (IronDiscipline-dev) |
|---|---|---|
| 対応サーバー | PaperSpigot 1.18+ | Folia 1.18+ |
| 階級保存先 | LuckPermsメタデータ | 独自DB (H2/MySQL) |
| LuckPerms | 必須 | 不要（移行時のみオプション） |
| パフォーマンス | API経由 | 直接DB+キャッシュ |
| 並行処理 | 標準 | スレッドセーフな並行処理対応 |
| Folia対応 | 非対応 | 完全対応（専用） |

## 機能

- **階級システム**: 階級による権限管理、config.ymlでの完全なカスタマイズが可能
  - スレッドセーフな並行キャッシュ（`ConcurrentHashMap`使用）
  - レースコンディション対策済み
- **PTS (Permission to Speak)**: 下士官の発言許可システム
- **Discord連携**: アカウント連携、ロール・ニックネーム同期
- **警告・処分システム**: 警告蓄積と自動処分
  - 二重隔離防止機能
  - アイテムロス防止の即時インベントリバックアップ
  - データ不整合の自動検出・修復
- **試験システム**: GUIを使用した昇進試験
- **データ移行**: `/irondev migrate` でLuckPermsからデータを簡単移行
- **Folia専用**: MorePaperLibによるFoliaネイティブなスケジューリング

## 必要要件

- Java 17+
- Folia 1.18+（**PaperSpigot非対応**）
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
