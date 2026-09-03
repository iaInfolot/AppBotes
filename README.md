# Infolot TV App — Señalización de botes y resultados (Android TV)

App para televisores Android TV que muestra en pantalla completa los próximos botes
y los últimos resultados de los juegos de Infolot/SELAE (Euromillón, La Primitiva,
Bonoloto, LotoTurf, EuroDreams, El Gordo, Lotería Nacional, Quiniela, QuiniGol,
Pleno al 15...), con actualización automática y arranque al encender el TV.

---

## 🧩 Arquitectura

La app es un **cascarón nativo mínimo (WebView en modo kiosco) que carga una página
web empaquetada dentro del propio APK**. Toda la lógica de negocio y estilos viven
en un único HTML, pero ya no se sirven por red — solo los datos siguen viniendo de
fuera:

```
┌───────────────────────────────────────────────┐        ┌───────────────────────┐
│  APK Android (kiosco)                          │  HTTP  │  webservice.infolot.es│
│  WebView a pantalla completa                   │───────▶│  /ws/...  (vía proxy) │
│  ├─ assets/infolot-tv-app.html  ─┐              │        │  proxy.php            │
│  ├─ assets/logos/*.png           ├─ WebViewAssetLoader   └───────────────────────┘
│  └─ (servidos como https://appassets.androidplatform.net/assets/...)             │
└───────────────────────────────────────────────┘
```

- **La app Android NO contiene lógica de negocio.** Solo abre un `WebView` en modo
  kiosco (sin barra de sistema, botón atrás bloqueado, navegación con mando D-Pad)
  y carga siempre la misma URL fija, servida localmente por `WebViewAssetLoader`
  (librería `androidx.webkit`) desde `app/src/main/assets/`:
  `https://appassets.androidplatform.net/assets/infolot-tv-app.html`
- **Hasta la versión 1.5.3, `infolot-tv-app.html` se servía remoto desde GitHub
  Pages** — así se podía actualizar la pantalla de todos los TVs desplegados sin
  recompilar el APK. Se abandonó porque mantener el repo público en GitHub Pages
  dejó de ser viable; ahora **cualquier cambio en `infolot-tv-app.html` o
  `logos/` requiere generar y publicar una versión nueva del APK** (ver
  "Compilar el APK" más abajo).
- El HTML se autentica contra el webservice con `token` + `pass` + `app_id`
  (valores por defecto embebidos en el propio HTML, ver `DEFAULTS`).
- `proxy.php` es un proxy servidor (alojado aparte, en `app.gesloto.es`) que
  reenvía las peticiones al webservice real para esquivar las restricciones CORS
  del WebView/navegador. `WebViewAssetLoader` sirve la pantalla bajo un origen
  `https://` real (no `file://`) precisamente para que este `fetch()` al
  webservice se siga comportando igual que cuando la pantalla era remota.

---

## 📁 Estructura del proyecto

```
AppBotes/
├── app/
│   ├── build.gradle                        # Dependencias del módulo (core-ktx, appcompat, androidx.webkit)
│   └── src/main/
│       ├── AndroidManifest.xml              # Permisos, receiver de arranque, launcher TV
│       ├── assets/
│       │   ├── infolot-tv-app.html          # ★ TODA la lógica y estilo de la pantalla ★
│       │   └── logos/*.png                  #   (empaquetados en el APK, servidos por WebViewAssetLoader)
│       ├── java/es/infolot/tv/
│       │   ├── MainActivity.kt              # WebView en modo kiosco, carga APP_URL fija
│       │   └── BootReceiver.kt              # Arranca la app al encender el TV
│       └── res/
│           ├── drawable/, drawable-xhdpi/tv_banner.png  # Banner del launcher de Android TV
│           ├── mipmap-*/ic_launcher.png     # Icono de la app
│           ├── values/strings.xml           # Nombre de la app
│           ├── values/themes.xml            # Tema (pantalla completa)
│           └── xml/network_security_config.xml
├── proxy.php                                # Proxy CORS hacia webservice.infolot.es (se aloja aparte)
├── privacidad.html                          # Política de privacidad — se aloja en infolot.es, no en este repo
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

## 🔨 Compilar el APK

Como `infolot-tv-app.html` y `logos/` viven dentro de `app/src/main/assets/`,
**cualquier cambio en la pantalla también requiere generar un APK/AAB nuevo y
publicarlo** — ya no hay forma de actualizar los TVs desplegados sin pasar por
Play Store.

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
| Carga de la Activity | El WebView carga `infolot-tv-app.html` empaquetado en el APK vía `WebViewAssetLoader` |
| Error de red | Reintenta recargar la página cada 30s |
| Datos del webservice | Refresco periódico según `DEFAULTS`/config guardada |

---

## 📋 Requisitos mínimos del dispositivo

- Android TV con **Android 5.0 (API 21)** o superior (`minSdk 21`)
- Resolución recomendada: 1920×1080 (Full HD)
- Conexión a internet (WiFi o Ethernet)

---

## ⚠️ Notas importantes

1. **Un solo HTML, el mismo para todos los TVs.** No hay credenciales
   "por cliente" hardcodeadas en el APK como en versiones antiguas de este
   proyecto — la fuente de verdad es `app/src/main/assets/infolot-tv-app.html`
   (más lo que cada TV tenga guardado en su `localStorage`). Al ir empaquetado
   en el APK, todos los TVs con la misma versión instalada llevan exactamente
   el mismo HTML.
2. **`proxy.php` se despliega aparte** (actualmente en `app.gesloto.es`), no
   forma parte del build de Android.
3. **`privacidad.html` se despliega aparte, en infolot.es** — tampoco forma
   parte del build de Android; solo vive en este repo como referencia/histórico.
4. **`versionCode`/`versionName`** del APK están en `app/build.gradle` — hace
   falta subirlos en **todos** los envíos ahora, incluidos los que antes solo
   habrían sido un cambio de HTML sin recompilar.
