# SELAE TV Signage — App de señalización de botes

App Android para televisores con Android TV que muestra los próximos botes de SELAE
en pantalla completa, con actualización automática y arranque al encender el TV.

---

## 📁 Estructura del proyecto

```
selae-tv/
├── app/
│   ├── build.gradle                        # Dependencias del módulo
│   └── src/main/
│       ├── AndroidManifest.xml             # Permisos, receiver de arranque
│       ├── java/com/selae/signage/
│       │   ├── SelaeApplication.kt         # Application class
│       │   ├── config/
│       │   │   └── AppConfig.kt            # ★ CREDENCIALES POR CLIENTE ★
│       │   ├── model/
│       │   │   └── JackpotModels.kt        # Modelos de datos JSON
│       │   ├── network/
│       │   │   └── NetworkClient.kt        # Llamada HTTP + caché
│       │   ├── receiver/
│       │   │   └── BootReceiver.kt         # Arranque automático
│       │   └── ui/
│       │       ├── MainActivity.kt         # Pantalla principal
│       │       ├── JackpotAdapter.kt       # RecyclerView adapter
│       │       └── FormatUtils.kt          # Fechas y moneda en español
│       └── res/
│           ├── layout/
│           │   ├── activity_main.xml       # Layout pantalla TV
│           │   ├── item_jackpot_card.xml   # Tarjeta de juego
│           │   └── item_addon_row.xml      # Fila de addon
│           ├── values/
│           │   ├── colors.xml
│           │   ├── dimens.xml
│           │   ├── strings.xml
│           │   └── themes.xml
│           └── drawable/
│               ├── bg_main_gradient.xml
│               ├── bg_card_gradient.xml
│               ├── ic_logo_placeholder.xml
│               └── ...
├── build.gradle                            # Build raíz
├── settings.gradle
└── gradle.properties
```

---

## ⚙️ Configuración por cliente

### Archivo a modificar: `app/src/main/java/com/selae/signage/config/AppConfig.kt`

```kotlin
object AppConfig {
    const val TOKEN: String = "TU_TOKEN_AQUI"   // ← Reemplazar
    const val PASS: String  = "TU_PASS_AQUI"    // ← Reemplazar
    const val APP_ID: String = "TU_APP_ID_AQUI" // ← Reemplazar

    // Opcional: cambiar intervalo de refresco (minutos)
    const val REFRESH_INTERVAL_MINUTES: Long = 30L
}
```

> **Flujo por cliente:** modifica `AppConfig.kt` → compila → entrega la APK release firmada.
> Cada cliente recibe una APK con sus propias credenciales hardcodeadas.

---

## 🔨 Compilar desde Android Studio

### Requisitos previos
- Android Studio Hedgehog (2023.1) o superior
- JDK 17
- SDK Android API 34 instalado
- SDK Build-Tools 34.x

### Pasos

1. **Abrir el proyecto**
   ```
   File → Open → selae-tv/
   ```

2. **Editar credenciales**
   Abre `AppConfig.kt` y sustituye `TOKEN`, `PASS` y `APP_ID`.

3. **Compilar APK debug** (para pruebas)
   ```
   Build → Build Bundle(s) / APK(s) → Build APK(s)
   ```
   Salida: `app/build/outputs/apk/debug/app-debug.apk`

4. **Compilar APK release** (para producción)

   Primero, crea un keystore si no tienes uno:
   ```
   Build → Generate Signed Bundle / APK → APK → Create new...
   ```
   Rellena los datos y guarda el `.jks`.

   Luego firma la release:
   ```
   Build → Generate Signed Bundle / APK → APK → Release
   ```
   Salida: `app/build/outputs/apk/release/app-release.apk`

---

## 🖥️ Compilar desde línea de comandos (CI/CD)

### Un cliente, compilación manual

```bash
# 1. Editar credenciales
sed -i 's/{{token_Plus}}/TOKEN_REAL/' app/src/main/java/com/selae/signage/config/AppConfig.kt
sed -i 's/{{pass_Plus}}/PASS_REAL/' app/src/main/java/com/selae/signage/config/AppConfig.kt
sed -i 's/{{Web_Service}}/APPID_REAL/' app/src/main/java/com/selae/signage/config/AppConfig.kt

# 2. Compilar APK debug
./gradlew assembleDebug

# 3. Compilar APK release (requiere keystore configurado en build.gradle)
./gradlew assembleRelease

# Salida: app/build/outputs/apk/release/app-release.apk
```

### Script para múltiples clientes

```bash
#!/bin/bash
# build_client.sh — genera APK por cliente
# Uso: ./build_client.sh NOMBRE_CLIENTE TOKEN PASS APP_ID

CLIENT=$1
TOKEN=$2
PASS=$3
APP_ID=$4

echo "▶ Compilando APK para cliente: $CLIENT"

# Restaurar AppConfig a plantilla antes de editar
git checkout app/src/main/java/com/selae/signage/config/AppConfig.kt

# Sustituir credenciales
sed -i "s|{{token_Plus}}|$TOKEN|g"   app/src/main/java/com/selae/signage/config/AppConfig.kt
sed -i "s|{{pass_Plus}}|$PASS|g"     app/src/main/java/com/selae/signage/config/AppConfig.kt
sed -i "s|{{Web_Service}}|$APP_ID|g" app/src/main/java/com/selae/signage/config/AppConfig.kt

# Compilar
./gradlew assembleRelease

# Copiar con nombre del cliente
cp app/build/outputs/apk/release/app-release.apk \
   ./dist/selae-signage-${CLIENT}.apk

echo "✅ APK generada: dist/selae-signage-${CLIENT}.apk"
```

```bash
mkdir -p dist
chmod +x build_client.sh

# Ejemplo de uso:
./build_client.sh ClienteA abc123token passXYZ ws_app_001
./build_client.sh ClienteB def456token pass789 ws_app_002
```

### Configurar firma en `app/build.gradle` (para release sin interacción)

```groovy
android {
    signingConfigs {
        release {
            storeFile     file(System.getenv("KEYSTORE_PATH") ?: "keystore.jks")
            storePassword System.getenv("KEYSTORE_PASS") ?: "password"
            keyAlias      System.getenv("KEY_ALIAS")     ?: "selae"
            keyPassword   System.getenv("KEY_PASS")      ?: "password"
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

---

## 📺 Instalar en el Android TV

### Desde ADB (recomendado para despliegue masivo)

```bash
# 1. Activar depuración ADB en el TV
#    Ajustes → Información del dispositivo → Compilación (pulsar 7 veces)
#    → Ajustes para desarrolladores → Depuración ADB → Activar

# 2. Conectar (TV y PC en la misma red WiFi)
adb connect 192.168.1.XXX:5555

# 3. Instalar la APK
adb install -r selae-signage-ClienteA.apk

# 4. Lanzar manualmente la primera vez
adb shell am start -n com.selae.signage/.ui.MainActivity

# A partir de entonces arranca sola al encender el TV.
```

### Desinstalación

```bash
adb uninstall com.selae.signage
```

---

## 🔄 Comportamiento esperado

| Evento | Comportamiento |
|--------|---------------|
| TV encendido | App arranca automáticamente (BootReceiver) |
| Primera carga | Muestra overlay de carga → datos del webservice |
| Cada 30 min | Refresco silencioso en segundo plano |
| Sin red | Muestra banner de aviso + datos del caché |
| Sin red y sin caché | Muestra mensaje de error claro |
| Logo no disponible | Muestra estrella dorada placeholder |

---

## 🛠️ Personalización rápida

| Qué cambiar | Dónde |
|-------------|-------|
| Credenciales | `AppConfig.kt` → `TOKEN`, `PASS`, `APP_ID` |
| Intervalo refresco | `AppConfig.kt` → `REFRESH_INTERVAL_MINUTES` |
| Colores / marca | `colors.xml` |
| Tamaños de texto | `dimens.xml` |
| Textos en pantalla | `strings.xml` |
| Logo cabecera | Sustituir `drawable/ic_selae_logo.xml` por PNG |

---

## 📋 Requisitos mínimos del dispositivo

- Android TV con **Android 6.0 (API 23)** o superior
- Recomendado: **Android 9+ (API 28)**
- Resolución: 1920×1080 (Full HD) — también funciona en 1280×720
- Conexión a internet (WiFi o Ethernet)

---

## ⚠️ Notas importantes

1. **El webservice debe aceptar peticiones POST con JSON.** Si el endpoint requiere
   parámetros en query string (GET), edita el método `fetchNextJackpots()` en
   `NetworkClient.kt` cambiando `.post(...)` por `.get()` y añadiendo los params
   como `HttpUrl.Builder` query parameters.

2. **Adaptar el modelo de datos.** Si la respuesta real del webservice usa nombres
   de campo distintos a los modelados en `JackpotModels.kt`, añade las anotaciones
   `@SerializedName` correspondientes. El modelo incluye múltiples alias comunes.

3. **Logo SELAE en cabecera.** El drawable `ic_selae_logo.xml` es un placeholder.
   Sustituirlo por el PNG/SVG oficial de SELAE para uso en producción.

4. **Pantallas muy pequeñas (< 5 juegos).** El grid ajusta columnas dinámicamente
   (ver `calculateOptimalColumns()` en `MainActivity.kt`). Mínimo 3 columnas.
