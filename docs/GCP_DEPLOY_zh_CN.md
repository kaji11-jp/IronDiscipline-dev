[🇺🇸 English](GCP_DEPLOY_en_US.md) | [🇩🇪 Deutsch](GCP_DEPLOY_de_DE.md) | [🇪🇸 Español](GCP_DEPLOY_es_ES.md) | [🇨🇳 中文](GCP_DEPLOY_zh_CN.md) | [🇯🇵 日本語](GCP_DEPLOY_ja_JP.md)

# IronDiscipline-dev GCP 部署指南

> ⚡ **不依赖LuckPerms版本** - 无需安装LuckPerms

## 前提条件

1. 已安装 [Google Cloud SDK](https://cloud.google.com/sdk/docs/install)
2. 已创建GCP项目
3. 已启用计费

## 快速部署

### 1. 编译插件

```bash
mvn clean package
```

### 2. 上传到GCS存储桶

```bash
gsutil mb gs://irondiscipline-server
gsutil cp target/IronDiscipline-dev-*.jar gs://irondiscipline-server/
```

### 3. 创建GCE实例

```bash
gcloud compute instances create irondiscipline-mc \
    --zone=asia-east1-a \
    --machine-type=e2-medium \
    --image-family=ubuntu-2204-lts \
    --image-project=ubuntu-os-cloud
```

## 从标准版迁移

```
/irondev migrate
```

详见 [迁移指南](MIGRATION_en_US.md)。

## 自动更新

```bash
curl -sL https://raw.githubusercontent.com/kaji11-jp/IronDiscipline-dev/main/scripts/update-server.sh | sudo bash
```
