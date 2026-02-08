[🇺🇸 English](GCP_DEPLOY_en_US.md) | [🇩🇪 Deutsch](GCP_DEPLOY_de_DE.md) | [🇪🇸 Español](GCP_DEPLOY_es_ES.md) | [🇨🇳 中文](GCP_DEPLOY_zh_CN.md) | [🇯🇵 日本語](GCP_DEPLOY_ja_JP.md)

# IronDiscipline-dev GCP Deployment Guide

> ⚡ **LuckPerms Independent Version** - No LuckPerms installation required

## Prerequisites

1. [Google Cloud SDK](https://cloud.google.com/sdk/docs/install) installed
2. GCP project created
3. Billing enabled

## Method 1: Easy Deploy (Recommended)

### 1. Build Plugin

```powershell
mvn clean package
```

### 2. Upload to GCS Bucket

```bash
# Create bucket
gsutil mb gs://irondiscipline-server

# Upload JAR (No LuckPerms needed!)
gsutil cp target/IronDiscipline-dev-*.jar gs://irondiscipline-server/
```

### 3. Create GCE Instance

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

### 4. Firewall Configuration

```bash
gcloud compute firewall-rules create minecraft-port \
    --allow tcp:25565,udp:25565 \
    --target-tags=minecraft-server
```

### 5. Connect

```bash
# Get IP
gcloud compute instances describe irondiscipline-mc --zone=asia-northeast1-b \
    --format='get(networkInterfaces[0].accessConfigs[0].natIP)'
```

Connect in Minecraft to `<IP>:25565`!

---

## Migration from Standard Version

To migrate from existing LuckPerms server:

```bash
gcloud compute ssh irondiscipline-mc --zone=asia-northeast1-b

# Run migration
/irondev migrate
```

See [Migration Guide](MIGRATION_en_US.md) for details.

---

## Pricing (Tokyo Region)

| Machine Type | RAM | Monthly (approx) |
|-------------|-----|-----------------|
| e2-micro | 1GB | Free tier |
| e2-small | 2GB | ~$15 |
| e2-medium | 4GB | ~$30 |

---

## Auto Update

```bash
curl -sL https://raw.githubusercontent.com/kaji11-jp/IronDiscipline-dev/main/scripts/update-server.sh | sudo bash
```
