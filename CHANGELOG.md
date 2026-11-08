# Changelog

Este archivo registra comportamiento implementado y verificado. Las ideas pendientes se mantienen en [`BACKLOG.md`](BACKLOG.md); no se anuncian aquí como funcionalidades terminadas.

## Estado inicial del proyecto

- Chat de Twitch mediante Twitch4J/IRC: conexión a un canal, recepción de mensajes y ejecución de comandos.
- La configuración `commands.json` se lee al iniciar; los comandos se insertan o actualizan en el repositorio JPA por nombre.
- Comandos configurables con texto/tipo de salida y reglas de owner/moderador; hay además lógica para comandos como uptime, moderadores, suscripciones y shoutout.
- Consulta de datos de Twitch mediante Helix.
- Interfaz estática que recibe mensajes del chat desde un WebSocket del backend.
- Persistencia original H2 en memoria y configuración de Twitch global, no por canal.
- El código aún contenía llamadas a Twitch PubSub; Twitch lo retiró el 14-04-2025, por lo que esas suscripciones de eventos no son funcionales y están pendientes de migración a EventSub.

## Cambios implementados en esta rama

### Base de aplicación

- Actualización a Spring Boot 4.1.2 y Java 25; Gradle Wrapper actualizado a Gradle 9.5.1.
- Actualización de dependencias directas de Twitch4J, Events4J, Caffeine y bibliotecas auxiliares; H2 queda solo para pruebas.

### Arranque local para el camino Twitch — 2026-11-08

- Añadidos el perfil Spring `development`, la plantilla versionada `.env.example`, el `.env` ignorado por Git y `dev-local.sh`; el lanzador selecciona Java 25 de SDKMAN si el Java activo no cumple y crea un directorio de datos privado.
- El perfil local usa SQLite independiente en `.local/data/oauth-credentials.sqlite`; Flyway aplica las cuatro migraciones y Hibernate valida el esquema. V4 añade la relación opcional entre autorización y cuenta creadora sin reasignar las filas existentes.
- Documentados en inglés y español el registro de la aplicación Twitch, los scopes requeridos, la clave Tink, el arranque, OAuth, `commands.json` y la prueba manual pendiente.
- Corregida la clase principal del task Gradle `generateTwitchCredentialKeyset`.
- Verificación: suite `./gradlew test --warning-mode all` y arranque real del script con Java 25 y una base temporal vacía; Flyway llegó a V4, `commands.json` se cargó y `/login.html` y `/` devolvieron HTTP 200. No se autorizó una cuenta Twitch ni se enviaron mensajes.


### OAuth seguro — fase inicial

- Inicio de OAuth Authorization Code Grant desde backend; el HTML ya no contiene Client ID, redirect URI, scopes ni un `state` constante.
- El backend genera un `state` aleatorio de un solo uso, vinculado a la sesión y válido durante diez minutos; rechaza state inválido, reutilizado o caducado y consentimiento denegado.
- El intercambio devuelve una respuesta tipada con access token, refresh token, expiración y scopes; no persiste el código de autorización.
- El callback y el intercambio dejan de registrar parámetros/cuerpos OAuth o valores de tokens; un mensaje de prueba no se envía automáticamente al chat.
- Pruebas unitarias del inicio OAuth, state, callback, denegación, canje URL-encoded y ausencia de secretos en logs; verificadas con respuestas HTTP simuladas.

### OAuth extensible y credenciales por proveedor — fase actual

- Definido un contrato OAuth común (`OAuthProvider`) con registro de proveedores, respuestas de token/identidad neutrales y un adaptador real de Twitch. La URL de autorización, intercambio, renovación, validación e scopes dependen del adaptador.
- El inicio y callback usan rutas por proveedor (`/callback/{providerId}/start` y `/callback/{providerId}`); `state` es de un solo uso y está aislado por proveedor dentro de la sesión. Un registro separado entrega la autorización al conector de la plataforma.
- El callback valida identidad, Client ID y scopes requeridos antes de persistir; la conexión al chat se delega al conector de Twitch.
- El smoke HTTP existente verificó `/callback/twitch/start` con HTTP 302; no se hizo conexión autenticada a Twitch.
- `ChannelAuthorization` se persiste como entidad JPA por proveedor + ID externo estable. El adaptador cifra access/refresh tokens con Tink antes de entregarlos a JPA.
- Flyway administra el esquema SQLite; V1 crea las tablas y V2 migra bases legacy `user_version` 1/2 a la versión 3, copiando el ciphertext sin descifrarlo. Hibernate usa `ddl-auto=validate`.
- El fichero conserva la ruta por defecto `data/oauth-credentials.sqlite`; Flyway/JPA comparten datasource. La entidad de comandos y el usuario Twitch existentes también usan JPA. H2 es solo dependencia de pruebas.
- Los repositorios de credenciales y comandos no se exponen por Spring Data REST. En sistemas POSIX, el fichero queda `rw-------` y el directorio `rwx------`.
- Pruebas verifican persistencia JPA/SQLite, cifrado en disco, migraciones legacy v1/v2 y arranque de Spring Boot con validación del esquema; `gradle test --warning-mode all` pasó.

### Ciclo de vida y conexiones aisladas por canal

- Al arrancar y cada hora se validan las autorizaciones persistidas. Si expira un access token, se renueva y los tokens rotatorios se guardan en SQLite; ante refresh inválido, el canal queda marcado para reautorización y se desconecta.
- Si la validación falla por un error transitorio, no se invalida la autorización ni se desconecta un chat activo. Al arrancar, se restauran las conexiones autorizadas; si cambian los tokens, se reconecta solo el canal afectado.
- Twitch mantiene una conexión independiente por ID de usuario del proveedor. Las respuestas de comandos integrados se envían por la conexión del canal de origen; el prefijo configurable se interpreta como tal.
- Pruebas unitarias cubren refresh, concurrencia con refresh token rotatorio, fallos transitorios, restauración y aislamiento al desconectar un canal.

### Migración de eventos de canal

- Sustituidas las suscripciones PubSub por EventSub WebSocket para follows, cheers, raids, suscripciones, regalos y mensajes de suscripción; cada condición usa el ID estable del canal autorizado.
- Pruebas unitarias verifican siete registros, direcciones de raid y rechazo de autorizaciones de otros proveedores. No se realizó una conexión autenticada real a Twitch.

### Limitaciones aún abiertas

- La revocación explícita desde la interfaz aún no está implementada; un refresh token inválido sí marca el canal para volver a autorizarlo.
- Solo está implementado el adaptador Twitch; no hay adaptador YouTube ni de otras plataformas.
- La interfaz para gestionar varios canales y comandos, onboarding guiado, Vue, chat unificado/fuentes OBS y despliegue/instalador Linux siguen pendientes. El runtime ya mantiene conexiones Twitch aisladas por canal.

## Registro de siguientes cambios

Para cada fase terminada, añadir una entrada fechada en `Unreleased` o en la versión publicada, describiendo solo cambios que hayan pasado sus pruebas y smoke checks. No copiar aquí credenciales, valores de `.properties` ni resultados que los revelen.
