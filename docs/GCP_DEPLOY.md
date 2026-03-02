[🇺🇸 English](GCP_DEPLOY_en_US.md) | [🇩🇪 Deutsch](GCP_DEPLOY_de_DE.md) | [🇪🇸 Español](GCP_DEPLOY_es_ES.md) | [🇨🇳 中文](GCP_DEPLOY_zh_CN.md) | [🇯🇵 日本語](GCP_DEPLOY_ja_JP.md)

# IronDiscipline-dev GCP デプロイガイド

> ⚡ **Folia専用版** - Foliaサーバー向けの軍事/監獄RPプラグインです

## 前提条件

1. [Google Cloud SDK](https://cloud.google.com/sdk/docs/install) インストール済み
2. GCPプロジェクト作成済み
3. 課金有効化済み

## 方法1: 簡単デプロイ（推奨）

### 1. プラグインをビルド

```powershell
mvn clean package
```

### 2. GCSバケットにアップロード

```bash
# バケット作成
gsutil mb gs://irondiscipline-server

# JARアップロード（LuckPermsは不要！）
gsutil cp target/IronDiscipline-dev-*.jar gs://irondiscipline-server/
```

### 3. GCEインスタンス作成

```bash
gcloud compute instances create irondiscipline-mc \
    --zone=asia-northeast1-b \
    --machine-type=e2-medium \
    --image-family=ubuntu-2204-lts \
    --image-project=ubuntu-os-cloud \
    --boot-disk-size=30GB \
    --tags=minecraft-server \
    --metadata-from-file startup-script=scripts/gcp-startup.sh
```

### 4. ファイアウォール設定

```bash
gcloud compute firewall-rules create minecraft-port \
    --allow tcp:25565,udp:25565 \
    --target-tags=minecraft-server
```

### 5. 接続

```bash
# IP確認
gcloud compute instances describe irondiscipline-mc --zone=asia-northeast1-b \
    --format='get(networkInterfaces[0].accessConfigs[0].natIP)'
```

Minecraftで `<IP>:25565` に接続！

---

## 旧Paper+LuckPerms版からの移行

旧バージョン（Paper+LuckPerms版）からのデータ移行：

```bash
gcloud compute ssh irondiscipline-mc --zone=asia-northeast1-b

# 移行コマンド実行
/irondev migrate
```

詳細は [移行ガイド](MIGRATION.md) を参照してください。

---

## 料金目安（東京リージョン）

| マシンタイプ | RAM | 月額（概算） |
|-------------|-----|-------------|
| e2-micro | 1GB | 無料枠内 |
| e2-small | 2GB | ~$15 |
| e2-medium | 4GB | ~$30 |

---

## Discord Bot設定

1. サーバー起動後、config.ymlを編集：

```bash
gcloud compute ssh irondiscipline-mc --zone=asia-northeast1-b
sudo nano /opt/minecraft/plugins/IronDisciplineDev/config.yml
```

2. Discord設定を入力：

```yaml
discord:
  enabled: true
  bot_token: "YOUR_BOT_TOKEN"
  notification_channel_id: "CHANNEL_ID"
  guild_id: "SERVER_ID"
```

3. サーバー再起動：

```bash
sudo systemctl restart minecraft
```

---

## 便利コマンド

```bash
# ログ確認
sudo journalctl -u minecraft -f

# サーバー停止
sudo systemctl stop minecraft

# サーバー起動
sudo systemctl start minecraft

# 自動アップデート
curl -sL https://raw.githubusercontent.com/kaji11-jp/IronDiscipline-dev/main/scripts/update-server.sh | sudo bash
```
