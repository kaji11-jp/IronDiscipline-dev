# プラットフォームガイド

## バージョン選択ガイド

IronDisciplineは2つのバージョンに分かれています。サーバー環境に応じて適切なバージョンを選択してください。

### IronDiscipline（通常版）

| 項目 | 内容 |
|---|---|
| 対応サーバー | PaperSpigot / Spigot 1.18+ |
| 階級管理 | LuckPermsメタデータ |
| 必須依存 | LuckPerms |
| Folia | **非対応** |

**こんな方におすすめ:**
- PaperSpigot を使用している
- LuckPerms を既に導入済み
- 権限管理と階級を統合して運用したい

### IronDiscipline-dev（Folia専用版）

| 項目 | 内容 |
|---|---|
| 対応サーバー | Folia 1.18+ |
| 階級管理 | 独自DB (H2/MySQL) |
| 必須依存 | なし |
| PaperSpigot | **非対応** |

**こんな方におすすめ:**
- Folia を使用している
- LuckPermsに依存しない軽量な構成を望む
- マルチスレッド対応の高パフォーマンスが必要

## 通常版 → Folia専用版への移行

PaperSpigotからFoliaへサーバーを移行する場合の手順です。

### 1. 前提条件

- Foliaサーバーの準備が完了していること
- 通常版のデータバックアップが取得済みであること

### 2. 手順

1. **データバックアップ**
   ```bash
   cp -r plugins/IronDiscipline/ plugins/IronDiscipline_backup/
   ```

2. **プラグイン差し替え**
   ```bash
   rm plugins/IronDiscipline.jar
   cp IronDiscipline-dev-latest.jar plugins/
   ```

3. **LuckPermsからのデータ移行**（LuckPermsを使用していた場合）
   - LuckPermsプラグインを一時的にpluginsに配置
   - サーバー起動後に `/irondev migrate` を実行
   - 移行完了後、LuckPermsを削除
   
   詳細は [MIGRATION.md](../IronDiscipline-dev/docs/MIGRATION.md) を参照

4. **設定ファイル**
   - `config.yml` の構造は共通のため、そのまま使用可能
   - `ranks.meta_key` 設定はdev版では使用されません（DB管理のため）

5. **動作確認**
   ```
   /irondev status
   ```

### 3. 注意事項

- 両バージョンの**並行運用は非推奨**です
- 移行前に必ず**バックアップ**を取得してください
- Folia版ではすべてのスケジューリングがリージョン/エンティティスケジューラーで行われます

## Folia専用版 → 通常版への移行

Foliaを離れてPaperSpigotに戻る場合の手順です。

### 1. 手順

1. **データバックアップ**
   ```bash
   cp -r plugins/IronDisciplineDev/ plugins/IronDisciplineDev_backup/
   ```

2. **プラグイン差し替え**
   ```bash
   rm plugins/IronDiscipline-dev*.jar
   cp IronDiscipline-latest.jar plugins/
   ```

3. **データ移行**
   - 現時点ではdev版のDB → LuckPermsメタデータへの自動移行は未実装
   - 手動でLuckPermsに階級データを設定する必要があります
   - 今後のアップデートで逆方向の移行ツールの提供を検討中

4. **LuckPerms設置**
   - 通常版はLuckPermsが必須です
   - LuckPermsをpluginsに配置してから起動してください

## FAQ

### Q: 既存の設定ファイルはそのまま使えますか？
A: はい。`config.yml`の構造はほぼ同一です。ただし`ranks.meta_key`は通常版のみで使用されます。

### Q: 両バージョンを同じサーバーで使えますか？
A: いいえ。データの不整合が発生する可能性があるため推奨しません。

### Q: Discordボットはどちらでも動作しますか？
A: はい。Discord連携機能は両バージョンで同等に動作します。

### Q: プラグイン名が異なるのはなぜですか？
A: 通常版は`IronDiscipline`、dev版は`IronDisciplineDev`という名前で登録されます。これは設定フォルダやデータフォルダが衝突しないようにするためです。
