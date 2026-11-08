# Bot de chat de Twitch

Aplicación Java 25 y Spring Boot que conecta con el chat de Twitch, ejecuta comandos cargados desde `commands.json` y recibe eventos mediante Twitch EventSub. El repositorio incluye ahora un frontend de desarrollo Vue junto a la página estática original servida por Spring.

## Requisitos

- Linux con Java 25. SDKMAN es opcional; selecciona Java 25 antes de ejecutar el wrapper de Gradle.
- Node.js 24 y npm 11 para el frontend.
- Acceso a Internet para dependencias de Gradle/npm y para OAuth/API de Twitch.
- Una aplicación registrada en [Twitch Developer Console](https://dev.twitch.tv/console/apps).

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

## Arrancar el backend y el frontend en local

En la primera terminal:

```sh
./dev-local.sh
```

El script exige Java 25, activa el perfil `development`, crea un directorio privado para los datos y arranca el backend en el puerto 8080. Por defecto, Flyway guarda la base local en `.local/data/oauth-credentials.sqlite`; Git ignora esa ruta y queda separada de la base normal de la aplicación. El perfil de desarrollo sobrescribe la base de datos y la configuración Twitch usada en local; los valores locales de Twitch se proporcionan en `.env`.

En una segunda terminal:

```sh
cd frontend
npm ci
npm run dev
```

Abre `http://localhost:5173`. Vite redirige `/account`, `/callback` y `/websocket` al backend del puerto 8080. Mantén `localhost` como hostname del navegador para que la cookie de sesión OAuth coincida con el hostname del callback.

El orden obligatorio es: crear una cuenta de la aplicación (contraseña de al menos 12 caracteres), verificar el correo con el enlace recibido, iniciar sesión y, después, pulsar **Conectar con Twitch**. El perfil de desarrollo espera un servidor SMTP en `localhost:1025`; por ejemplo, arranca Mailpit con `docker run --rm -p 1025:1025 -p 8025:8025 axllent/mailpit` y consulta los mensajes en `http://localhost:8025`. El enlace de verificación pasa por Vite hacia el backend y vuelve al frontend. El inicio de OAuth exige una sesión autenticada de la aplicación; el callback de Twitch sigue siendo público, pero valida el `state` de un solo uso vinculado a la sesión.

Cada autorización nueva de Twitch queda vinculada en SQLite a la cuenta de creador con sesión iniciada. No se puede reasignar a otro creador una autorización que ya tenga propietario.

En el primer arranque del backend, Flyway aplica las migraciones. La aplicación carga `src/main/resources/commands.json` y sincroniza los comandos en SQLite por nombre. Todavía no existe edición online ni gestión de comandos por canal.

Después de iniciar sesión, el chat está en `http://localhost:5173/chat`. La URL local de fuente de navegador para OBS es `http://localhost:5173/embed/chat`; tiene fondo transparente y muestra el mismo chat WebSocket de solo lectura. El WebSocket no requiere autenticación y la vista no tiene una clave revocable; mantén ambos servicios en local y no los expongas a Internet.

Las variables opcionales `DEVELOPMENT_CREATOR_EMAIL` y `DEVELOPMENT_CREATOR_PASSWORD` crean una cuenta local pre-verificada para iniciar sesión; no son credenciales Twitch ni hacen falta si te registras desde la interfaz.

## Comandos y eventos

El fichero JSON actual define el prefijo `!` y las respuestas de los comandos, incluido `!help`. Los comandos se interpretan sobre los mensajes del canal Twitch conectado y las respuestas normales se envían al mismo canal de origen. El backend registra los manejadores EventSub; para recibir eventos reales se necesita una autorización Twitch válida y que ocurra un evento compatible en el canal autorizado.

## Pruebas

Backend:

```sh
./gradlew test --warning-mode all
```

Frontend:

```sh
cd frontend
npm ci
npm run unit
npx playwright install chromium
npm run e2e
npm run build
```

El comando Cucumber E2E arranca su propio servidor Vite local; detén antes cualquier frontend iniciado por separado. Las pruebas no usan consentimiento real de Twitch ni mensajes de chat inventados.

## Alcance actual

Twitch es el único proveedor integrado. Ya están implementados el frontend Vue 3 + Pinia + Vuetify, temas claro/oscuro, preferencias en inglés/español, registro/verificación/inicio de sesión, OAuth Twitch protegido por cuenta, chat de solo lectura y una vista OBS local transparente. Siguen pendientes la gestión de creadores/canales, onboarding guiado en varios pasos, edición online o por canal de comandos, `!pole`, preguntas, control/revocación del acceso al overlay y personalización de su estilo. Consulta [`BACKLOG.md`](BACKLOG.md) para el trabajo pendiente y [`CHANGELOG.md`](CHANGELOG.md) para el comportamiento verificado.