# Bot de chat de Twitch

Aplicación Java 25 y Spring Boot que conecta con el chat de Twitch, ejecuta comandos cargados desde `commands.json` y recibe eventos mediante Twitch EventSub. El chat web actual es una página estática pequeña conectada por WebSocket; todavía no es el frontend Vue planificado.

## Requisitos

- Linux con Java 25. SDKMAN es opcional; selecciona Java 25 antes de ejecutar el wrapper de Gradle.
- Acceso a Internet para descargar dependencias y comunicarse con OAuth/API de Twitch.
- Una aplicación de Twitch registrada en [Twitch Developer Console](https://dev.twitch.tv/console/apps).

El wrapper descarga la versión fijada de Gradle. La versión local actual es Gradle 9.5.1.

## Configurar Twitch en local

1. Copia la plantilla segura y protege el fichero local:

   ```sh
   cp .env.example .env
   chmod 600 .env
   ```

2. En Twitch Developer Console, configura exactamente esta URL de redirección OAuth: `http://localhost:8080/callback/twitch`.
3. Genera un keyset de Tink:

   ```sh
   ./gradlew generateTwitchCredentialKeyset
   ```

   El valor Base64 generado es secreto. Guárdalo solo en el `.env` ignorado por Git como `TWITCH_TOKEN_ENCRYPTION_KEYSET` y conserva una copia protegida. Si se pierde la clave, no se podrán descifrar los tokens persistidos.
4. Rellena `TWITCH_CLIENT_ID`, `TWITCH_CLIENT_SECRET` y `TWITCH_TOKEN_ENCRYPTION_KEYSET` en `.env`. No subas `.env`, no copies credenciales a documentación y no uses secretos de producción para desarrollo local.

El adaptador actual solicita los scopes `chat:read`, `chat:edit`, `moderator:read:followers`, `bits:read` y `channel:read:subscriptions`.

## Arrancar el backend en local

```sh
./dev-local.sh
```

El script exige Java 25, activa el perfil `development`, crea un directorio privado para los datos y arranca el backend en el puerto 8080. Por defecto, Flyway guarda la base local en `.local/data/oauth-credentials.sqlite`; Git ignora esa ruta y queda separada de la base normal de la aplicación. El perfil de desarrollo sobrescribe la base de datos y la configuración Twitch usada en local; los valores locales de Twitch se proporcionan en `.env`.

En el primer arranque, Flyway aplica las migraciones. La aplicación carga `src/main/resources/commands.json` y sincroniza los comandos en SQLite por nombre. Para cambiar este conjunto inicial, edita ese fichero y reinicia el backend. Todavía no existe edición online ni gestión de comandos por canal.

Abre `http://localhost:8080/login.html` y pulsa el botón de autorización con Twitch. Twitch redirige a `/callback/twitch`; cuando termine correctamente, abre `http://localhost:8080/` para ver el chat actual. El backend valida y guarda los tokens OAuth cifrados en SQLite, conecta el chat de Twitch y registra las suscripciones EventSub configuradas. No se ha probado un consentimiento Twitch real ni se han enviado mensajes en directo; haz esa comprobación manualmente con una cuenta y canal de prueba.

Las variables opcionales `DEVELOPMENT_CREATOR_EMAIL` y `DEVELOPMENT_CREATOR_PASSWORD` crean una cuenta local separada para probar el módulo de cuentas; no hacen falta para el flujo OAuth de Twitch.

## Comandos y eventos

El fichero JSON actual define el prefijo `!` y las respuestas de los comandos, incluido `!help`. Los comandos se interpretan sobre los mensajes del canal Twitch conectado y las respuestas normales se envían al mismo canal de origen. El backend registra los manejadores EventSub; para recibir eventos reales se necesita una autorización Twitch válida y que ocurra un evento compatible en el canal autorizado.

## Pruebas

```sh
./gradlew test --warning-mode all
```

Las pruebas unitarias simulan las respuestas del proveedor y usan bases aisladas; no inician sesión en Twitch ni envían mensajes al chat.

## Alcance actual

Twitch es el único proveedor integrado. El frontend Vue 3 + Pinia + Vuetify, tema claro/oscuro, interfaz en inglés/español, gestión de canales del creador y vista de chat para OBS con URL propia estable y revocable están planificados, no implementados. Consulta [`BACKLOG.md`](BACKLOG.md) para el trabajo pendiente y [`CHANGELOG.md`](CHANGELOG.md) para el comportamiento verificado.