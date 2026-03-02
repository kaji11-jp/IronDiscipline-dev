[🇺🇸 English](VPS_DEPLOY_en_US.md) | [🇩🇪 Deutsch](VPS_DEPLOY_de_DE.md) | [🇪🇸 Español](VPS_DEPLOY_es_ES.md) | [🇨🇳 中文](VPS_DEPLOY_zh_CN.md) | [🇯🇵 日本語](VPS_DEPLOY_ja_JP.md)

# 通用VPS部署指南

> ⚡ **Folia专用版** - 适用于Folia服务器的军事/监狱RP插件

## 支持的VPS
- **阿里云 / 腾讯云** (中国)
- **Linode / DigitalOcean / Vultr** (国际)

## 1. 服务器准备

- **操作系统**: Ubuntu 22.04 LTS
- **CPU**: 2核以上
- **内存**: 4GB以上（推荐8GB）

## 2. 运行安装脚本

```bash
ssh root@<服务器IP>
curl -O https://raw.githubusercontent.com/kaji11-jp/IronDiscipline-dev/main/scripts/setup-ubuntu.sh
sudo bash setup-ubuntu.sh
```

## 3. 上传插件

```bash
scp target/IronDiscipline-dev-*.jar root@<服务器IP>:/opt/minecraft/plugins/
```

## 4. 自动更新

```bash
curl -sL https://raw.githubusercontent.com/kaji11-jp/IronDiscipline-dev/main/scripts/update-server.sh | sudo bash
```

## 迁移

详见 [迁移指南](MIGRATION_en_US.md)。
