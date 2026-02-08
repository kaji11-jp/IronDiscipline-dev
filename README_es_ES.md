[🇺🇸 English](README_en_US.md) | [🇩🇪 Deutsch](README_de_DE.md) | [🇪🇸 Español](README_es_ES.md) | [🇨🇳 中文](README_zh_CN.md) | [🇯🇵 日本語](README_ja_JP.md)

# IronDiscipline-dev (Disciplina de Hierro - Independiente de LuckPerms)

Plugin integral de gestión y disciplina para servidores de Minecraft.
Diseñado para servidores de RP militar y de prisiones.

> ⚡ **¡Esta versión NO depende de LuckPerms!** Los datos de rango se almacenan en una base de datos dedicada para mayor rendimiento y compatibilidad.

## Diferencias con la Versión Estándar

| Elemento | Estándar (IronDiscipline) | Dev (IronDiscipline-dev) |
|---|---|---|
| Almacenamiento de Rangos | Metadatos de LuckPerms | BD propia (H2/MySQL) |
| LuckPerms | Requerido | No requerido (opcional para migración) |
| Rendimiento | Vía API | BD directa + Caché |

## Características

- **Sistema de Rangos**: Gestión de permisos por rango
- **PTS (Permiso para Hablar)**: Sistema de permiso de habla
- **Integración con Discord**: Vinculación de cuentas, sincronización de roles
- **Sistema de Advertencias**: Advertencias con castigo automático
- **Sistema de Exámenes**: Exámenes de promoción con GUI
- **Migración de Datos**: Migración fácil desde LuckPerms con `/irondev migrate`

## Requisitos

- Java 17+
- Paper / Spigot / Folia 1.18+
- MySQL, SQLite o H2 Database (predeterminado)

## Instalación

1. Descargar el JAR más reciente de [Releases](https://github.com/kaji11-jp/IronDiscipline-dev/releases)
2. Colocar en la carpeta `plugins` del servidor
3. Iniciar el servidor
4. Editar `plugins/IronDisciplineDev/config.yml` según sea necesario

## Migración desde Versión Estándar

```
/irondev migrate
```

## Comandos

### 🔧 Comandos de Versión Dev
| Comando | Descripción | Permiso |
|---|---|---|
| `/irondev migrate` | Migrar datos desde LuckPerms | `iron.admin` |
| `/irondev status` | Mostrar estado | `iron.admin` |

## Compilar

```bash
mvn clean package
```

## Licencia

MIT License
