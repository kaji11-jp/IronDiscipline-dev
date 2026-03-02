# プラットフォームガイド

[🇺🇸 English](PLATFORM_GUIDE_en_US.md)

## IronDiscipline-dev について

IronDiscipline-dev は **Folia専用** の軍事/監獄RPプラグインです。

| 項目 | 内容 |
|---|---|
| 対応サーバー | Folia 1.20.4 |
| 階級管理 | 独自DB (H2/MySQL) |
| 必須依存 | なし（LuckPerms不要） |
| スレッドモデル | Folia リージョン/エンティティスケジューラー |

### 主な機能

- **階級システム**: PRIVATE〜COMMANDERの10段階、独自DB管理
- **刑務所システム**: 投獄/釈放、インベントリ保存
- **試験システム**: 昇進試験の出題・採点
- **Discord連携**: JDA 5 Botによる通知・階級同期
- **Web Dashboard**: 組み込みHTTPサーバーによるDiscord Bot管理画面
- **無線通信**: チャンネル制の無線システム
- **多言語対応**: ja_JP / en_US / de_DE / es_ES / zh_CN

### サーバー要件

- **Java**: 21以上
- **サーバー**: Folia 1.20.4
- **メモリ**: 4GB以上（推奨8GB）
- **OS**: Ubuntu 22.04 LTS / 24.04 LTS（推奨）

## 旧Paper+LuckPerms版からの移行

> ⚠️ **注意**: 旧バージョン（Paper+LuckPerms版の `IronDiscipline`）は廃止されました。
> 現在はFolia専用版の `IronDiscipline-dev` のみがメンテナンスされています。

旧バージョンからの移行手順：

### 1. 前提条件

- Foliaサーバーの準備が完了していること
- 旧バージョンのデータバックアップが取得済みであること

### 2. 手順

1. **データバックアップ**
   ```bash
   cp -r plugins/IronDiscipline/ plugins/IronDiscipline_backup/
   ```

2. **プラグイン差し替え**
   ```bash
   rm plugins/IronDiscipline.jar
   cp IronDiscipline-dev-2.0.0-dev.jar plugins/
   ```

3. **LuckPermsからのデータ移行**（旧版でLuckPermsを使用していた場合）
   - LuckPermsプラグインを一時的にpluginsに配置
   - サーバー起動後に `/irondev migrate` を実行
   - 移行完了後、LuckPermsを削除
   
   詳細は [MIGRATION.md](MIGRATION.md) を参照

4. **設定ファイル**
   - `config.yml` の構造はほぼ同一のため、そのまま使用可能
   - `ranks.meta_key` 設定は不要です（DB管理のため）

5. **動作確認**
   ```
   /irondev status
   ```

### 3. 注意事項

- 移行前に必ず**バックアップ**を取得してください
- Folia版ではすべてのスケジューリングがリージョン/エンティティスケジューラーで行われます
- Paper/Spigot用プラグインとの互換性にご注意ください

## デプロイガイド

- [汎用VPSデプロイガイド](VPS_DEPLOY.md) — Xserver, ConoHa, Linode等
- [Google Cloud Platformデプロイガイド](GCP_DEPLOY.md) — GCE + GCS
- [Docker](../docker-compose.yml) — Docker Compose による起動

## FAQ

### Q: LuckPermsは必要ですか？
A: いいえ。階級データは独自データベース（H2/MySQL）で管理されるため、LuckPermsは不要です。

### Q: Paper/Spigotサーバーで動作しますか？
A: いいえ。このプラグインはFolia専用です。Foliaのリージョンスケジューラーに依存しているため、Paper/Spigotでは動作しません。

### Q: Discordボットの設定はどこで行いますか？
A: `config.yml` の `discord` セクションでBot Token等を設定します。Web Dashboardからも設定変更が可能です。

### Q: データベースはどれを選べばよいですか？
A: 小規模サーバーにはH2（デフォルト、設定不要）、大規模サーバーにはMySQLを推奨します。
