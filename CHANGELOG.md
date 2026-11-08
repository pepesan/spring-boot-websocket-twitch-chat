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
- Actualización de las dependencias directas de Twitch4J, Events4J, H2, Caffeine y bibliotecas auxiliares.

### OAuth seguro — fase inicial

- Inicio de OAuth Authorization Code Grant desde backend; el HTML ya no contiene Client ID, redirect URI, scopes ni un `state` constante.
- El backend genera un `state` aleatorio de un solo uso, vinculado a la sesión y válido durante diez minutos; rechaza state inválido, reutilizado o caducado y consentimiento denegado.
- El intercambio devuelve una respuesta tipada con access token, refresh token, expiración y scopes; no persiste el código de autorización.
- El callback y el intercambio dejan de registrar parámetros/cuerpos OAuth o valores de tokens; un mensaje de prueba no se envía automáticamente al chat.
- Pruebas unitarias del inicio OAuth, state, callback, denegación, canje URL-encoded y ausencia de secretos en logs; verificadas con respuestas HTTP simuladas.

### OAuth extensible y credenciales por proveedor — fase actual

- Definido un contrato OAuth común (`OAuthProvider`) con registro de proveedores, respuestas de token/identidad neutrales y un adaptador real de Twitch. La URL de autorización, intercambio, renovación, validación e scopes dependen del adaptador.
- El inicio y callback usan rutas por proveedor (`/callback/{providerId}/start` y `/callback/{providerId}`); `state` es de un solo uso y está aislado por proveedor dentro de la sesión. Un registro separado entrega la autorización al conector de la plataforma.
- `ChannelAuthorization` y SQLite identifican credenciales por proveedor + ID externo estable. El esquema SQLite v2 migra las filas v1 de Twitch conservando ciphertext; access/refresh tokens siguen cifrados con Google Tink.
- El callback valida identidad, Client ID y scopes requeridos antes de persistir. La conexión del canal existente se delega al conector Twitch.
- Pruebas unitarias verifican contrato/selección de proveedor, aislamiento de state, validación antes de persistir, identidad por proveedor, cifrado y migración v1→v2. Smoke: aplicación Spring Boot 4.1.2/Java 25 arranca y `/callback/twitch/start` responde HTTP 302; sin conectar a Twitch real.

### Migración de eventos de canal

- Sustituidas las suscripciones PubSub por EventSub WebSocket para follows, cheers, raids, suscripciones, regalos y mensajes de suscripción; cada condición usa el ID estable del canal autorizado.
- Pruebas unitarias verifican siete registros, direcciones de raid y rechazo de autorizaciones de otros proveedores. No se realizó una conexión autenticada real a Twitch.

### Limitaciones aún abiertas

- Falta gestionar el ciclo de vida tras el consentimiento: validación al arranque y cada hora, renovación reactiva con persistencia atómica del refresh token rotatorio, revocación y parada/reconexión por canal.
- Solo está implementado el adaptador Twitch; no hay adaptador YouTube ni de otras plataformas.
- La gestión simultánea de todos los canales, onboarding guiado, interfaz Vue, chat unificado/fuentes OBS, comandos online y despliegue/instalador Linux siguen pendientes.

## Registro de siguientes cambios

Para cada fase terminada, añadir una entrada fechada en `Unreleased` o en la versión publicada, describiendo solo cambios que hayan pasado sus pruebas y smoke checks. No copiar aquí credenciales, valores de `.properties` ni resultados que los revelen.
