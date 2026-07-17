# Infolot TV App — Señalización de botes y resultados (Android TV)

App para televisores Android TV que muestra en pantalla completa los próximos botes
y los últimos resultados de los juegos de Infolot/SELAE (Euromillón, La Primitiva,
Bonoloto, LotoTurf, EuroDreams, El Gordo, Lotería Nacional, Quiniela, QuiniGol,
Pleno al 15...), con actualización automática y arranque al encender el TV.

---

## 🧩 Arquitectura

La app es un **cascarón nativo mínimo (WebView en modo kiosco) que carga una página
web remota**. Toda la lógica de negocio, estilos y datos viven fuera del APK:

```
┌─────────────────────────┐        ┌──────────────────────────────┐        ┌───────────────────────┐
│   APK Android (kiosco)  │  HTTP  │  infolot-tv-app.html          │  HTTP  │  webservice.infolot.es│
│   WebView a pantalla    │───────▶│  (GitHub Pages)               │───────▶│  /ws/...  (vía proxy) │
│   completa              │        │  HTML + CSS + JS, un archivo  │        │  proxy.php            │
└─────────────────────────┘        └──────────────────────────────┘        └───────────────────────┘
```

- **La app Android NO contiene lógica de negocio.** Solo abre un `WebView` en modo
  kiosco (sin barra de sistema, botón atrás bloqueado, navegación con mando D-Pad)
  y carga siempre la misma URL fija:
  `https://iainfolot.github.io/AppBotes/infolot-tv-app.html`
- **Actualizar la pantalla para todos los TVs desplegados = editar y publicar
  `infolot-tv-app.html`** en GitHub Pages. No hace falta recompilar ni
  redistribuir el APK salvo que cambie algo del propio shell nativo (permisos,
  arranque, WebView...).
- El HTML se autentica contra el webservice con `token` + `pass` + `app_id`
  (valores por defecto embebidos en el propio HTML, ver `DEFAULTS`).
- `proxy.php` es un proxy servidor (alojado aparte, en `app.gesloto.es`) que
  reenvía las peticiones al webservice real para esquivar las restricciones CORS
  del WebView/navegador.

---

## 📁 Estructura del proyecto

```
AppBotes/
├── app/
│   ├── build.gradle                        # Dependencias del módulo (mínimas: core-ktx, appcompat)
│   └── src/main/
│       ├── AndroidManifest.xml              # Permisos, receiver de arranque, launcher TV
│       ├── java/es/infolot/tv/
│       │   ├── MainActivity.kt              # WebView en modo kiosco, carga APP_URL fija
│       │   └── BootReceiver.kt              # Arranca la app al encender el TV
│       └── res/
│           ├── drawable/tv_banner.png       # Banner del launcher de Android TV
│           ├── mipmap-*/ic_launcher.png     # Icono de la app
│           ├── values/strings.xml           # Nombre de la app
│           ├── values/themes.xml            # Tema (pantalla completa)
│           └── xml/network_security_config.xml
├── infolot-tv-app.html                      # ★ TODA la lógica y estilo de la pantalla ★
│                                             #   (publicado en GitHub Pages, se carga por URL)
├── proxy.php                                # Proxy CORS hacia webservice.infolot.es (se aloja aparte)
├── privacidad.html                          # Política de privacidad (requerida por Google Play)
├── build.gradle                             # Build raíz
├── settings.gradle
└── gradle.properties
```

---

## 🖥️ `infolot-tv-app.html` — dónde está cada cosa

Archivo único (HTML + CSS + JS vanilla, sin build ni frameworks). Puntos clave:

| Qué | Dónde (aprox.) |
|-----|----------------|
| Variables de tema/colores/tipografías | `:root { ... }` al inicio del `<style>` |
| Estilos de las tarjetas de bote (vista "Botes") | `.jackpot-card`, `.card-*` |
| Estilos de los bloques de resultados (vista "Resultados") | `.result-block`, `.result-block-*`, `.rb-body`, `.ball-*` |
| Config por defecto (URL, token, pass, app_id, colores de marca...) | `const DEFAULTS = {...}` |
| Carga/guardado de config en `localStorage` | `loadConfig()`, `persistConfig()` |
| Llamadas al webservice | `fetch(...)` dentro de `loadAllData()`, `fetchResults()`, etc. |
| **Construcción del contenido y estilo de cada bloque de resultado** (Euromillón, Bonoloto...) | `buildBlockHTML(gameId, draws, availW)` |
| **Envoltorio DOM de cada bloque** (`.result-block`) | `makeBlock(gameId, draws, availW)` |
| **Distribución en columnas de la vista Resultados** (qué juego va en qué columna/orden) | `_renderResults()` → `COL_ORDER` |

> Los colores de cada juego (barra superior, fondo/texto de las bolas, complementario,
> reintegro...) **no están hardcodeados por juego**: vienen del webservice
> (`game.fg_color`, `game.number_bg_color`, etc., dentro de `gamesMap`). Si quieres
> cambiar el color de un juego concreto sin tocar el backend, hay que sobreescribirlo
> en `buildBlockHTML()`.

---

## ⚙️ Configuración

La configuración (URL del webservice, credenciales, colores de marca, tema
claro/oscuro, orientación, intervalos de refresco, modo demo...) se gestiona:

- Desde el propio menú de la app (botón "☰ MENÚ" en pantalla), que guarda en
  `localStorage` bajo la clave definida en `STORAGE_KEY`.
- O editando los valores por defecto en `const DEFAULTS = {...}` dentro de
  `infolot-tv-app.html`, que se usan si no hay nada guardado en `localStorage`
  (o tras pulsar "restablecer").

---

## 🔨 Compilar el APK (shell nativo)

Solo hace falta si cambia algo del propio wrapper Android (permisos, URL de
arranque, comportamiento del WebView, icono, nombre de la app...). Un cambio en
`infolot-tv-app.html` **no requiere recompilar**.

### Requisitos previos
- Android Studio o JDK 17 + Android SDK (compileSdk/targetSdk 35)
- Gradle 8.9 / AGP 8.7.3 (ver `gradle/wrapper/gradle-wrapper.properties` y `build.gradle`)

### Desde línea de comandos

```bash
./gradlew assembleDebug      # APK debug: app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease    # APK release (requiere firma configurada)
```

---

## 📺 Instalar en el Android TV

```bash
# 1. Activar depuración ADB en el TV
#    Ajustes → Información del dispositivo → Compilación (pulsar 7 veces)
#    → Ajustes para desarrolladores → Depuración ADB → Activar

# 2. Conectar (TV y PC en la misma red WiFi)
adb connect 192.168.1.XXX:5555

# 3. Instalar la APK
adb install -r app-release.apk

# 4. Lanzar manualmente la primera vez
adb shell am start -n es.infolot.tv/.MainActivity

# A partir de entonces arranca sola al encender el TV (BootReceiver).
```

Desinstalación: `adb uninstall es.infolot.tv`

---

## 🔄 Comportamiento esperado

| Evento | Comportamiento |
|--------|---------------|
| TV encendido | App arranca automáticamente (`BootReceiver`) |
| Carga de la Activity | El WebView limpia caché y carga `infolot-tv-app.html?v=timestamp` (evita servir una versión cacheada vieja) |
| Error de red | Reintenta recargar la página cada 30s |
| Datos del webservice | Refresco periódico según `DEFAULTS`/config guardada |

---

## 📋 Requisitos mínimos del dispositivo

- Android TV con **Android 5.0 (API 21)** o superior (`minSdk 21`)
- Resolución recomendada: 1920×1080 (Full HD)
- Conexión a internet (WiFi o Ethernet)

---

## ⚠️ Notas importantes

1. **Un solo HTML publicado sirve a todos los TVs.** No hay credenciales
   "por cliente" hardcodeadas en el APK como en versiones antiguas de este
   proyecto — la fuente de verdad es `infolot-tv-app.html` en GitHub Pages
   (más lo que cada TV tenga guardado en su `localStorage`).
2. **`proxy.php` se despliega aparte** (actualmente en `app.gesloto.es`), no
   forma parte del build de Android ni se publica junto al HTML.
3. **`versionCode`/`versionName`** del APK están en `app/build.gradle` — solo
   hace falta subirlos si se publica una nueva versión en Google Play.
