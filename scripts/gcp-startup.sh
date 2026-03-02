#!/bin/bash
# GCP インスタンス起動時のスタートアップスクリプト

set -e

# ログ設定
exec > >(tee /var/log/minecraft-setup.log)
exec 2>&1

echo "====== IronDiscipline Folia Server Setup ======"

# パッケージインストール
apt-get update
apt-get install -y openjdk-21-jre-headless screen curl jq unzip

# Minecraft ユーザー作成
useradd -r -m -d /opt/minecraft minecraft || true

# ディレクトリ作成
mkdir -p /opt/minecraft/{plugins,world}
cd /opt/minecraft

# Folia ダウンロード
VERSION="1.20.4"
echo "Folia $VERSION をダウンロード中..."
RESPONSE=$(curl -s "https://api.papermc.io/v2/projects/folia/versions/$VERSION/builds")
BUILD=$(echo "$RESPONSE" | jq -r '.builds[-1].build')
HASH=$(echo "$RESPONSE" | jq -r '.builds[-1].downloads.application.sha256')

if [ "$BUILD" == "null" ] || [ -z "$BUILD" ]; then
    echo "Error: Folia build info fetch failed for version $VERSION."
    exit 1
fi

curl -o folia.jar -L "https://api.papermc.io/v2/projects/folia/versions/$VERSION/builds/$BUILD/downloads/folia-$VERSION-$BUILD.jar"
echo "$HASH folia.jar" | sha256sum -c -

# EULA同意
echo "eula=true" > eula.txt

# server.properties
cat > server.properties << 'EOF'
server-port=25565
motd=\u00a76\u00a7l[Iron Discipline] \u00a7f\u00a7l\u9244\u306e\u898f\u5f8b
max-players=50
online-mode=true
enable-command-block=false
spawn-protection=0
allow-flight=true
view-distance=10
simulation-distance=8
EOF

# IronDiscipline.jar をGCSからダウンロード (See docs/GCP_DEPLOY.md)
echo "IronDiscipline をダウンロード中..."
gsutil cp gs://irondiscipline-server/IronDiscipline-latest.jar plugins/

# 権限設定
chown -R minecraft:minecraft /opt/minecraft

# systemdサービス作成
cat > /etc/systemd/system/minecraft.service << 'EOF'
[Unit]
Description=Minecraft Folia Server (IronDiscipline)
After=network.target

[Service]
User=minecraft
WorkingDirectory=/opt/minecraft
ExecStart=/usr/bin/java -Xms3G -Xmx3G \
    -XX:+UseG1GC \
    -XX:+ParallelRefProcEnabled \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UnlockExperimentalVMOptions \
    -XX:+DisableExplicitGC \
    -XX:+AlwaysPreTouch \
    -XX:G1NewSizePercent=30 \
    -XX:G1MaxNewSizePercent=40 \
    -XX:G1HeapRegionSize=8M \
    -XX:G1ReservePercent=20 \
    -XX:G1HeapWastePercent=5 \
    -XX:G1MixedGCCountTarget=4 \
    -XX:InitiatingHeapOccupancyPercent=15 \
    -XX:G1MixedGCLiveThresholdPercent=90 \
    -XX:G1RSetUpdatingPauseTimePercent=5 \
    -XX:SurvivorRatio=32 \
    -XX:+PerfDisableSharedMem \
    -XX:MaxTenuringThreshold=1 \
    -Dusing.aikars.flags=https://mcflags.emc.gs \
    -Daikars.new.flags=true \
    -jar folia.jar nogui
ExecStop=/bin/kill -SIGTERM $MAINPID
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# サービス有効化・起動
systemctl daemon-reload
systemctl enable minecraft
systemctl start minecraft

echo "====== Setup Complete ======"
echo "Server starting..."
