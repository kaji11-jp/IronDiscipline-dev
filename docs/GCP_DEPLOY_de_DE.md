[🇺🇸 English](GCP_DEPLOY_en_US.md) | [🇩🇪 Deutsch](GCP_DEPLOY_de_DE.md) | [🇪🇸 Español](GCP_DEPLOY_es_ES.md) | [🇨🇳 中文](GCP_DEPLOY_zh_CN.md) | [🇯🇵 日本語](GCP_DEPLOY_ja_JP.md)

# IronDiscipline-dev GCP Bereitstellungsleitfaden

> ⚡ **Folia Exklusiv** - Militär/Gefängnis-RP-Plugin für Folia-Server

## Voraussetzungen

1. [Google Cloud SDK](https://cloud.google.com/sdk/docs/install) installiert
2. GCP-Projekt erstellt
3. Abrechnung aktiviert

## Schnelle Bereitstellung

### 1. Plugin bauen

```bash
mvn clean package
```

### 2. In GCS-Bucket hochladen

```bash
gsutil mb gs://irondiscipline-server
gsutil cp target/IronDiscipline-dev-*.jar gs://irondiscipline-server/
```

### 3. GCE-Instanz erstellen

```bash
gcloud compute instances create irondiscipline-mc \
    --zone=europe-west3-a \
    --machine-type=e2-medium \
    --image-family=ubuntu-2204-lts \
    --image-project=ubuntu-os-cloud
```

## Migration von alter Paper+LuckPerms Version

```
/irondev migrate
```

Siehe [Migrationsleitfaden](MIGRATION_en_US.md) für Details.

## Automatisches Update

```bash
curl -sL https://raw.githubusercontent.com/kaji11-jp/IronDiscipline-dev/main/scripts/update-server.sh | sudo bash
```
