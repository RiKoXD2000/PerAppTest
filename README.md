# Perchance Chat — Android App

Cliente móvil para https://perchance.org/0t7o0b20gx

## Requisitos

- Android Studio Hedgehog (2023.1.1) o más reciente
- JDK 17+
- Android SDK 34

## Cómo abrir y compilar

1. Abre **Android Studio**
2. `File → Open` y selecciona esta carpeta (`PerchanceApp/`)
3. Espera que Gradle sincronice (botón "Sync Now" si aparece)
4. Conecta tu Android o usa un emulador (API 26+)
5. Presiona ▶ Run

## Generar APK para instalar sin Play Store

`Build → Build Bundle(s) / APK(s) → Build APK(s)`

El APK queda en:
`app/build/outputs/apk/debug/app-debug.apk`

Cópialo a tu teléfono e instálalo (necesitas activar "Instalar apps desconocidas" en Ajustes).

## Características incluidas

- ✅ IndexedDB habilitado (guarda chats/personajes igual que el navegador)
- ✅ JavaScript completo
- ✅ Soporte de import/export de datos
- ✅ Botón Atrás navega dentro de la app
- ✅ Pantalla de error si no hay internet
- ✅ Barra de progreso de carga
- ✅ Edge-to-edge (sin barras de sistema feas)
- ✅ Links externos se abren en el navegador
- ✅ Funciona en Android 8+ (API 26)

## Personalización rápida

Para cambiar la URL → `MainActivity.kt`, línea:
```kotlin
private val TARGET_URL = "https://perchance.org/0t7o0b20gx"
```

Para cambiar el nombre de la app → `res/values/strings.xml`
