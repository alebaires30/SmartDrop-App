# SmartDrop — App Android

App de SmartDrop para usuarios comunes y administradores. Usa el **mismo backend Django** que la web (repositorio
de la web), así que primero tiene que estar corriendo la web en tu laptop o usarse el servidor publicado.

**La app no lleva ninguna llave** (ni de Supabase, ni MQTT, ni de Gotita/Anthropic, ni de ElevenLabs). Solo necesita
saber la dirección del servidor. Todo lo privado vive en el `.env` del backend.

## Requisitos

- Android Studio.
- JDK 17 o superior (`Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK`).
- Android SDK 35.
- Un emulador o un teléfono Android.
- La web de SmartDrop corriendo (ver el README del repositorio de la web).

## Ponerse al día

```powershell
git checkout master
git pull
```

En Android Studio: `File > Sync Project with Gradle Files` y luego **Run**.

## Primera vez

1. Clona el repositorio y ábrelo en Android Studio (la carpeta `SmartDrop-App`, no solo `app`).
2. Configura la dirección del servidor en `gradle.properties` (siguiente sección).
3. `File > Sync Project with Gradle Files`.
4. Levanta la web en tu laptop y dale **Run** a la app.
5. Entra con la misma cuenta que usas en la web.

## Dirección del servidor (`gradle.properties`)

Solo el nombre o la IP: sin `http://`, sin puerto y sin `/`.

| Propiedad | Para qué | Valor |
|---|---|---|
| `smartdropApiHost` | App de prueba (debug) contra la web de tu laptop. La app agrega `:8000`. | Ver tabla de abajo |
| `smartdropReleaseApiHost` | APK publicado (release), siempre por HTTPS. | El dominio del servidor publicado |

Valor de `smartdropApiHost` según cómo pruebes:

| Cómo pruebas | Valor | Además |
|---|---|---|
| Emulador de Android Studio | `10.0.2.2` | Nada más. |
| Teléfono por WiFi | La IP de tu laptop (`ipconfig`, ej. `192.168.x.x`) | Misma red WiFi; agrega esa IP a `ALLOWED_HOSTS` del `.env` de la web; permite el puerto 8000 en el firewall. |
| Teléfono por USB | `127.0.0.1` | Ejecuta `adb reverse tcp:8000 tcp:8000` cada vez que conectes el cable. |

Para la web en tu laptop usa `python manage.py runserver 0.0.0.0:8000` si pruebas con un teléfono, para que acepte
conexiones de la red.

No subas a Git tu IP personal en `gradle.properties`: déjala solo en tu copia local.

## APK release (solo quien publica)

- Usa `smartdropReleaseApiHost` con el dominio del servidor.
- La firma se configura en `keystore.properties` (con `storeFile`, `storePassword`, `keyAlias`, `keyPassword`) y el
  archivo `.jks`. **Ninguno de los dos se sube a Git** (ya están en `.gitignore`); se pasan en privado.
- Compilar: `Build > Generate Signed App Bundle / APK`, o `.\gradlew.bat :app:assembleRelease`.

## Si algo falla

| Problema | Qué revisar |
|---|---|
| La app no conecta | Que la web esté corriendo, que `smartdropApiHost` sea el correcto y que hiciste Sync después de cambiarlo. |
| Funciona en el emulador pero no en el teléfono | Misma red WiFi, IP en `ALLOWED_HOSTS`, firewall con el puerto 8000, web con `0.0.0.0:8000`. |
| Por USB dejó de conectar | Vuelve a ejecutar `adb reverse tcp:8000 tcp:8000`. |
| `Falta smartdropReleaseApiHost` al compilar | Agrega esa propiedad en `gradle.properties`. |
| Gotita no responde en la app | Las llaves de Gotita van en el `.env` de la web, no en la app; revisa el README de la web. |
