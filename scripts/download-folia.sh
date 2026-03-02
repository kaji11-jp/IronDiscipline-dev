#!/bin/bash
# Folia ダウンロードスクリプト

VERSION=${MINECRAFT_VERSION:-1.20.4}

echo "Fetching Folia build info for version $VERSION..."

# PaperMC API から最新 Folia ビルド取得
RESPONSE=$(curl -s "https://api.papermc.io/v2/projects/folia/versions/$VERSION/builds")
BUILD=$(echo "$RESPONSE" | jq -r '.builds[-1].build')
HASH=$(echo "$RESPONSE" | jq -r '.builds[-1].downloads.application.sha256')

if [ "$BUILD" == "null" ] || [ -z "$BUILD" ]; then
    echo "Error: Could not fetch Folia build info for version $VERSION"
    echo "Available versions: https://api.papermc.io/v2/projects/folia"
    exit 1
fi

echo "Downloading Folia $VERSION build $BUILD..."
echo "Expected Hash: $HASH"

curl -o folia.jar -L "https://api.papermc.io/v2/projects/folia/versions/$VERSION/builds/$BUILD/downloads/folia-$VERSION-$BUILD.jar"

echo "$HASH folia.jar" | sha256sum -c -

echo "Download complete!"
