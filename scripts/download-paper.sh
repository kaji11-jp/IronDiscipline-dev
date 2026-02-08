#!/bin/bash
# Paper MC ダウンロードスクリプト

VERSION=${MINECRAFT_VERSION:-1.21.1}

echo "Fetching Paper build info for version $VERSION..."

# Paper API から最新ビルド取得
RESPONSE=$(curl -s "https://api.papermc.io/v2/projects/paper/versions/$VERSION/builds")
BUILD=$(echo "$RESPONSE" | jq -r '.builds[-1].build')
HASH=$(echo "$RESPONSE" | jq -r '.builds[-1].downloads.application.sha256')

if [ "$BUILD" == "null" ] || [ -z "$BUILD" ]; then
    echo "Error: Could not fetch build info"
    exit 1
fi

echo "Downloading Paper $VERSION build $BUILD..."
echo "Expected Hash: $HASH"

curl -o paper.jar -L "https://api.papermc.io/v2/projects/paper/versions/$VERSION/builds/$BUILD/downloads/paper-$VERSION-$BUILD.jar"

echo "$HASH paper.jar" | sha256sum -c -

echo "Download complete!"
