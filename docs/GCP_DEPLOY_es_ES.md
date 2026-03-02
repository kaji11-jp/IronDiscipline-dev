[🇺🇸 English](GCP_DEPLOY_en_US.md) | [🇩🇪 Deutsch](GCP_DEPLOY_de_DE.md) | [🇪🇸 Español](GCP_DEPLOY_es_ES.md) | [🇨🇳 中文](GCP_DEPLOY_zh_CN.md) | [🇯🇵 日本語](GCP_DEPLOY_ja_JP.md)

# Guía de Implementación en GCP para IronDiscipline-dev

> ⚡ **Exclusivo para Folia** - Plugin de RP militar/prisión para servidores Folia

## Requisitos Previos

1. [Google Cloud SDK](https://cloud.google.com/sdk/docs/install) instalado
2. Proyecto GCP creado
3. Facturación habilitada

## Implementación Rápida

### 1. Compilar Plugin

```bash
mvn clean package
```

### 2. Subir a Bucket GCS

```bash
gsutil mb gs://irondiscipline-server
gsutil cp target/IronDiscipline-dev-*.jar gs://irondiscipline-server/
```

### 3. Crear Instancia GCE

```bash
gcloud compute instances create irondiscipline-mc \
    --zone=us-central1-a \
    --machine-type=e2-medium \
    --image-family=ubuntu-2204-lts \
    --image-project=ubuntu-os-cloud
```

## Migración desde la versión antigua Paper+LuckPerms

```
/irondev migrate
```

Ver [Guía de Migración](MIGRATION_en_US.md) para detalles.

## Actualización Automática

```bash
curl -sL https://raw.githubusercontent.com/kaji11-jp/IronDiscipline-dev/main/scripts/update-server.sh | sudo bash
```
