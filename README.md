# AnimatedNametags

Plugin para **Paper 1.21+** que reemplaza los nametags por **TextDisplays** con:

- ✅ Soporte completo de **MiniMessage**
- ✅ **Varias líneas** por nametag
- ✅ **Degradados y rainbow animados** (cambian constantemente por ticks)
- ✅ Opción para **ocultar los nametags vanilla**
- ✅ Opción para **ocultar los nametags de TAB** (vía API de TAB)
- ✅ Formatos por **permiso/prioridad** (admin, vip, default…)
- ✅ **PlaceholderAPI** opcional + conversión de colores legacy (`&c`, `&#ff0000`)
- ✅ Se atenúa al agacharse, se oculta con invisibilidad

## Instalación
1. Descarga el `.jar` desde **Releases** (o desde *Actions → Build → Artifacts*).
2. Ponlo en `plugins/` y reinicia.
3. Edita `plugins/AnimatedNametags/config.yml` y usa `/ant reload`.

## Cómo funciona la animación
En tus líneas usa `{phase}` como fase del degradado:

```yaml
lines:
  - "<gradient:#ff0055:#ffaa00:#ff0055:{phase}><bold>ADMIN</bold></gradient>"
  - "<rainbow:{phase}><player></rainbow>"
```

`{phase}` va de `-1` a `1` en bucle; `animation-speed` controla qué tan rápido avanza.
También existen `{phase_rev}` (sentido contrario) y `{tick}`.

Placeholders internos: `<player>`, `<displayname>`, `<health>`, `<world>`, `<ping>`.

## Ocultar TAB y vanilla
```yaml
settings:
  hide-vanilla-nametags: true
  hide-tab-nametags: true
```
Para TAB se usa `NameTagManager#hideNametag`. Si prefieres, también puedes poner
`scoreboard-teams.enabled: false` en el `config.yml` de TAB.

## Comandos y permisos
| Comando | Permiso | Descripción |
|---|---|---|
| `/ant reload` | `animatednametags.admin` | Recarga la config |
| `/ant refresh` | `animatednametags.admin` | Reaplica nametags y ocultado |
| `/ant toggle [jugador]` | `animatednametags.toggle` / `.admin` | Activa/desactiva el nametag |

Formatos: `animatednametags.format.admin`, `animatednametags.format.vip` (o el permiso que pongas en el config).

## Compilar
```bash
mvn clean package
```
El jar queda en `target/AnimatedNametags-1.0.0.jar`.

## Publicar un release con el .jar
```bash
git tag v1.0.0
git push origin v1.0.0
```
GitHub Actions compila y adjunta el `.jar` al release automáticamente.

## Notas
- Si un jugador ya está en otro team del scoreboard (por otro plugin), el ocultado vanilla de este plugin no lo toca.
- Si tu nametag queda muy alto o bajo, ajusta `settings.y-offset`.
- Subir `update-interval-ticks` reduce tráfico de paquetes en servidores grandes.
