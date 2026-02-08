[🇺🇸 English](VPS_DEPLOY_en_US.md) | [🇩🇪 Deutsch](VPS_DEPLOY_de_DE.md) | [🇪🇸 Español](VPS_DEPLOY_es_ES.md) | [🇨🇳 中文](VPS_DEPLOY_zh_CN.md) | [🇯🇵 日本語](VPS_DEPLOY_ja_JP.md)

# 汎用VPSデプロイガイド (Xserver, ConoHa, Linode等)

> ⚡ **LuckPerms非依存版** - LuckPermsのインストールは不要です

このプロジェクトは、Google Cloud Platform以外の一般的なVPS（Virtual Private Server）でも簡単に動作させることができます。

## 対応VPSの例
- **Xserver VPS** (日本・高速・安定)
- **ConoHa VPS** (日本・使いやすい)
- **Linode / DigitalOcean / Vultr** (海外・安価)

## 1. サーバーの準備

### 推奨スペック
- **OS**: Ubuntu 22.04 LTS または 24.04 LTS
- **CPU**: 2コア以上
- **メモリ**: 4GB以上 (推奨8GB)

### 手順
1. VPSの管理画面からインスタンス（サーバー）を作成します。
2. OSは **Ubuntu** を選択してください。
3. `root` パスワードを設定するか、SSH鍵を登録します。

## 2. セットアップスクリプトの実行

サーバーにSSH接続し、以下のコマンドを実行するだけで環境構築が完了します。

```bash
# 1. SSH接続 (PowerShell / Terminal)
ssh root@<サーバーIPアドレス>

# 2. スクリプトのダウンロードと実行
curl -O https://raw.githubusercontent.com/kaji11-jp/IronDiscipline-dev/main/scripts/setup-ubuntu.sh
sudo bash setup-ubuntu.sh
```

## 3. プラグインのアップロード

### ビルド＆アップロード
```bash
# ローカルでビルド
mvn clean package

# アップロード（LuckPermsは不要！）
scp target/IronDiscipline-dev-*.jar root@<サーバーIP>:/opt/minecraft/plugins/
```

### FileZilla / WinSCP を使う場合
1. ホスト: `<サーバーIP>`, ユーザー: `root`, パスワード: `(設定したもの)` で接続
2. `/opt/minecraft/plugins/` に `.jar` ファイルをドラッグ＆ドロップ

最後にサーバーを再起動して反映：
```bash
ssh root@<サーバーIP> "systemctl restart minecraft"
```

## 4. 自動アップデート

最新版への更新は1コマンドで完了します：

```bash
curl -sL https://raw.githubusercontent.com/kaji11-jp/IronDiscipline-dev/main/scripts/update-server.sh | sudo bash
```

## 5. 通常版からの移行

LuckPerms版からデータを移行する場合は [移行ガイド](MIGRATION.md) を参照してください。

## 6. ポート開放 (必要に応じて)

多くのVPSはデフォルトで全ポート開放されていますが、Xserver VPSなどの一部では管理画面でファイアウォール設定が必要です。

**開放が必要なポート:**
- TCP: `25565` (Java版)
- UDP: `19132` (統合版/スマホ - Geyser使用時)

## 7. Discord連携設定

1. コンフィグを開く
```bash
nano /opt/minecraft/plugins/IronDisciplineDev/config.yml
```
2. `bot_token` などを入力して保存 (`Ctrl+S`, `Ctrl+X`)
3. 再起動: `systemctl restart minecraft`
