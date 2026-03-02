#!/bin/bash
# ===========================================
#  Ubuntu/Debian 汎用セットアップスクリプト
# ===========================================
# 対応: Xserver VPS, ConoHa VPS, Linode, DigitalOcean etc.
# 動作環境: Ubuntu 22.04 LTS / 24.04 LTS (Debian 12も可)
# サーバー: Folia (マルチスレッド Paper フォーク)

set -e

# ルート権限チェック
if [ "$EUID" -ne 0 ]; then
  echo "エラー: このスクリプトは root 権限で実行してください (sudo bash setup-ubuntu.sh)"
  exit 1
fi

echo "====== IronDiscipline Folia Server Setup ======"

# 1. パッケージ更新 & ツールインストール
echo "[1/6] パッケージを更新中..."
apt-get update
# Java 21, screen, curl, jq
apt-get install -y openjdk-21-jre-headless screen curl jq unzip

# 2. ユーザー作成
echo "[2/6] Minecraftユーザーを作成中..."
useradd -r -m -d /opt/minecraft minecraft || true

# 3. ディレクトリ準備
echo "[3/6] ディレクトリを作成中..."
mkdir -p /opt/minecraft/{plugins,world}
cd /opt/minecraft

# 4. Folia ダウンロード
VERSION="1.20.4"
echo "[4/6] Folia $VERSION をダウンロード中..."

# PaperMC API からビルド情報とハッシュを取得
RESPONSE=$(curl -s "https://api.papermc.io/v2/projects/folia/versions/$VERSION/builds")
BUILD=$(echo "$RESPONSE" | jq -r '.builds[-1].build')
HASH=$(echo "$RESPONSE" | jq -r '.builds[-1].downloads.application.sha256')

if [ "$BUILD" == "null" ] || [ -z "$BUILD" ]; then
    echo "Error: Folia build info fetch failed for version $VERSION."
    echo "Available versions: https://api.papermc.io/v2/projects/folia"
    exit 1
fi

echo "Build: $BUILD"
echo "Expected Hash: $HASH"

# ダウンロード
curl -o folia.jar -L "https://api.papermc.io/v2/projects/folia/versions/$VERSION/builds/$BUILD/downloads/folia-$VERSION-$BUILD.jar"

# ハッシュ検証
echo "$HASH folia.jar" | sha256sum -c -

# EULA同意
echo "eula=true" > eula.txt

# server.properties (基本設定)
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


# 5. サービス化 (systemd)
echo "[5/6] 基本設定を作成中..."

echo "[6/6] 自動起動設定を作成中..."
cat > /etc/systemd/system/minecraft.service << 'EOF'
[Unit]
Description=Minecraft Folia Server (IronDiscipline)
After=network.target

[Service]
User=minecraft
WorkingDirectory=/opt/minecraft
ExecStart=/usr/bin/java -Xms4G -Xmx4G \
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

# 権限修正
chown -R minecraft:minecraft /opt/minecraft

# サービス起動
systemctl daemon-reload
systemctl enable minecraft
systemctl start minecraft

echo ""
echo "======================================"
echo "  セットアップ完了！"
echo "======================================"
echo "サーバーIP: $(curl -s ifconfig.me):25565"
echo ""
echo "【次のステップ】"
echo "1. ローカルでビルドした 'IronDiscipline-dev-2.0.0-dev.jar' をSFTP等でアップロード"
echo "   場所: /opt/minecraft/plugins/"
echo "2. サーバーを再起動: sudo systemctl restart minecraft"
echo "3. ログ確認: sudo journalctl -u minecraft -f"
echo ""
