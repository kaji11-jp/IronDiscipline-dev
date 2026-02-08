[🇺🇸 English](CROSS_PLAY_en_US.md) | [🇩🇪 Deutsch](CROSS_PLAY_de_DE.md) | [🇪🇸 Español](CROSS_PLAY_es_ES.md) | [🇨🇳 中文](CROSS_PLAY_zh_CN.md) | [🇯🇵 日本語](CROSS_PLAY_ja_JP.md)

# Guía de Juego Cruzado (Bedrock)

Este servidor admite conexiones desde la Edición Bedrock (Móvil, Switch, PS4/5, Xbox) utilizando **Geyser** y **Floodgate**.

## 1. Plugins Requeridos
Coloque los siguientes dos plugins en la carpeta `plugins`.
(El script de despliegue `scripts/gcp-startup.sh` ya incluye los comandos de descarga, pero siga estos pasos para la instalación manual)

- **Geyser**: Plugin principal que traduce la comunicación entre las ediciones Java y Bedrock.
- **Floodgate**: Plugin de autenticación que permite a los jugadores de Bedrock iniciar sesión sin una cuenta de Java.

```bash
# Ir al directorio de plugins
cd /opt/minecraft/plugins

# Descargar
curl -o Geyser-Spigot.jar -L "https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest/downloads/spigot"
curl -o Floodgate-Spigot.jar -L "https://download.geysermc.org/v2/projects/floodgate/versions/latest/builds/latest/downloads/spigot"

# Reiniciar servidor
sudo systemctl restart minecraft
```

## 2. Abrir Puerto (UDP 19132)
La Edición Bedrock utiliza **`19132 (UDP)`**, a diferencia de los `25565 (TCP)` de la Edición Java.
Necesita abrir este puerto en su firewall.

### Para GCP (Ejecutar en su PC local)
```powershell
gcloud compute firewall-rules create geyser-port --allow udp:19132 --target-tags=minecraft-server
```

### Para Xserver VPS / ConoHa, etc.
Agregue la siguiente regla en la configuración de "Firewall" o "Grupo de Seguridad" en su panel de control del VPS.
- Protocolo: **UDP**
- Número de Puerto: **19132**
- Origen: **Todos (0.0.0.0/0)**

## 3. Método de Conexión
- **Dirección del Servidor**: La misma dirección IP que la Edición Java
- **Puerto**: `19132` (Por defecto)

## Nota: Sobre las Skins
Floodgate refleja automáticamente las skins de Bedrock en la Edición Java.
