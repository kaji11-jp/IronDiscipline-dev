# IronDiscipline Minecraft Server
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="IronDiscipline"
LABEL description="Minecraft Folia Server with IronDiscipline Plugin"

# 環境変数
ENV MINECRAFT_VERSION=1.20.4
ENV MEMORY=2G
ENV SERVER_PORT=25565

# 作業ディレクトリ
WORKDIR /minecraft

# 必要なパッケージ
RUN apk add --no-cache curl jq

# サーバーJARダウンロードスクリプト
COPY scripts/download-folia.sh /minecraft/
RUN chmod +x /minecraft/download-folia.sh

# プラグインコピー
COPY target/IronDiscipline-dev-2.0.0-dev.jar /minecraft/plugins/

# 設定ファイル
COPY docker/server.properties /minecraft/
COPY docker/eula.txt /minecraft/

# ポート公開
EXPOSE 25565/tcp
EXPOSE 25565/udp

# ボリューム（永続化）
VOLUME ["/minecraft/world", "/minecraft/plugins/IronDiscipline"]

# 起動
COPY scripts/start.sh /minecraft/
RUN chmod +x /minecraft/start.sh

ENTRYPOINT ["/minecraft/start.sh"]
