[🇺🇸 English](README_en_US.md) | [🇩🇪 Deutsch](README_de_DE.md) | [🇪🇸 Español](README_es_ES.md) | [🇨🇳 中文](README_zh_CN.md) | [🇯🇵 日本語](README_ja_JP.md)

# IronDiscipline-dev (鉄の規律 LuckPerms非依存版)

Minecraftサーバー用 総合管理・規律維持プラグイン。
軍隊・刑務所RPサーバー向けに設計されています。

> ⚡ **このバージョンはLuckPermsに依存しません！** 階級データは独自DBに保存されるため、より高速で互換性が高いです。

## 通常版との違い

| 項目 | 通常版 (IronDiscipline) | dev版 (IronDiscipline-dev) |
|---|---|---|
| 階級保存先 | LuckPermsメタデータ | 独自DB (H2/MySQL) |
| LuckPerms | 必須 | 不要（移行時のみオプション） |
| パフォーマンス | API経由 | 直接DB+キャッシュ |
| 並行処理 | 標準 | スレッドセーフな並行処理対応 |
| Folia対応 | 非対応 | 完全対応 |

## 機能

- **階級システム**: 階級による権限管理、config.ymlでの完全なカスタマイズが可能
  - スレッドセーフな並行キャッシュ（`ConcurrentHashMap`使用）
  - レースコンディション対策済み
- **PTS (Permission to Speak)**: 下士官の発言許可システム
- **Discord連携**:
  - アカウント連携 (`/link`)
  - ロール・ニックネーム同期
  - 通達システム
  - サーバーステータス表示
- **警告・処分システム**:
  - `/warn` で警告蓄積
  - 一定数で自動隔離・Kick
  - `/jail` 隔離システム (DB保存)
  - 二重隔離防止機能
  - アイテムロス防止の即時インベントリバックアップ
  - データ不整合の自動検出・修復
- **試験システム**: GUIを使用した昇進試験
- **勤務時間管理**: オンライン時間の記録
- **メッセージカスタマイズ**: ゲーム内メッセージのほとんどを変更可能
- **データ移行**: `/irondev migrate` でLuckPermsからデータを簡単移行
- **Folia対応**: MorePaperLibによる完全なFolia互換性

## 必要要件

- Java 17+
- Paper / Spigot / Folia 1.18+（Folia完全対応）
- MySQL, SQLite または H2 Database (デフォルト)

## インストール

1. [Releases](https://github.com/kaji11-jp/IronDiscipline-dev/releases) から最新のJARファイルをダウンロードします。
2. サーバーの `plugins` フォルダに配置します。
3. サーバーを起動します。
4. `plugins/IronDisciplineDev/config.yml` が生成されるので、必要に応じて編集します。
5. サーバーを再起動または `/iron reload` で設定を反映させます。

## 通常版からの移行

LuckPerms版(通常版)からデータを移行する場合：

1. 通常版で使っていたLuckPermsプラグインも一緒にpluginsに配置
2. サーバー起動後、以下コマンドを実行：
   ```
   /irondev migrate
   ```
3. 移行完了後、LuckPermsを削除可能

## 設定

### データベース設定
デフォルトでは H2 Database (ファイルベース) を使用しますが、大規模サーバーでは MySQL の使用を推奨します。

```yaml
database:
  # タイプ: h2, sqlite, mysql
  type: mysql
  mysql:
    host: localhost
    port: 3306
    database: irondiscipline
    username: root
    password: "password"
```

### Discord連携設定
`config.yml` に Discord Bot Token 等を設定してください。

```yaml
discord:
  enabled: true
  bot_token: "YOUR_TOKEN"
  guild_id: "YOUR_GUILD_ID"
  notification_channel_id: "YOUR_CHANNEL_ID"
```

## コマンド一覧

### 🌐 一般コマンド
| コマンド | 説明 | 権限 |
|---|---|---|
| `/link [コード]` | Discordアカウント連携 | なし |
| `/playtime [top]` | 勤務時間（プレイ時間）を表示 | `iron.playtime.view` |
| `/radio <周波数>` | 無線チャンネルに参加・退出 | `iron.radio.use` |
| `/radiobroadcast <msg>` | 無線で広域放送 | `iron.radio.use` |
| `/warnings [player]` | 自分または他人の警告履歴を表示 | `iron.warn.view` |

### 👮 規律・管理コマンド
| コマンド | 説明 | 権限 |
|---|---|---|
| `/warn <player> <理由>` | 警告を与える（累積で自動処分） | `iron.warn.use` |
| `/unwarn <player>` | 最新の警告を取り消す | `iron.warn.admin` |
| `/clearwarnings <player>` | 警告を全消去する | `iron.warn.admin` |
| `/jail <player> [理由]` | プレイヤーを強制隔離 | `iron.jail.use` |
| `/unjail <player>` | プレイヤーを釈放 | `iron.jail.use` |
| `/setjail` | 隔離場所を現在地に設定 | `iron.jail.admin` |
| `/grant <player> [秒]` | 下士官に発言権(PTS)を付与 | `iron.pts.grant` |
| `/promote <player>` | 階級を昇進させる | `iron.rank.promote` |
| `/demote <player>` | 階級を降格させる | `iron.rank.demote` |
| `/division <set/remove...>` | 部隊配属・除隊管理 | `iron.division.use` |
| `/exam <start/end...>` | 昇進試験の管理 | `iron.exam.use` |
| `/killlog [player] [数]` | PvP詳細ログの確認 | `iron.killlog.view` |
| `/iron reload` | 設定リロード | `iron.admin` |

### 🔧 dev版専用コマンド
| コマンド | 説明 | 権限 |
|---|---|---|
| `/irondev migrate` | LuckPermsからデータ移行 | `iron.admin` |
| `/irondev status` | ステータス表示 | `iron.admin` |

### 🤖 Discord Bot コマンド（スラッシュコマンド）
| コマンド | 説明 |
|---|---|
| `/link` | アカウント連携（DM・サーバー両対応） |
| `/settings` | Bot設定・ロール紐付け管理 |
| `/panel` | 連携・ロール管理パネルの設置 |
| `/promote, /demote` | 階級操作（Discordから実行可） |
| `/division` | 部隊管理 |
| `/kick, /ban` | 処罰実行 |

## ビルド

```bash
mvn clean package
```

## デプロイ

このプロジェクトは、**Google Cloud Platform (GCP)** や **Xserver VPS** などの一般的なVPSで動作するように設計されています。

### 1. GCP (Google Cloud Platform)
[GCPデプロイガイド (Docs)](docs/GCP_DEPLOY.md) を参照してください。

### 2. 一般的なVPS (Xserver, ConoHaなど)
[汎用VPSデプロイガイド (Docs)](docs/VPS_DEPLOY.md) を参照してください。

### 3. 統合版 (スマホ/Switch) 対応
[統合版対応ガイド (Docs)](docs/CROSS_PLAY.md) を参照してください。

## 🔄 自動アップデート

このプロジェクトは **GitHub Actions** による自動ビルドに対応しています。
`main` ブランチにプッシュされると、自動的に最新版がビルドされ、[Releases](https://github.com/kaji11-jp/IronDiscipline-dev/releases) に公開されます。

## ライセンス

MIT License
