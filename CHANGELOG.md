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

### Persistencia segura de autorizaciones — base de fase

- Añadido el almacén SQLite `data/twitch-chat.sqlite`, con esquema versionado, upsert por broadcaster ID, aislamiento entre canales y permisos de fichero restringidos en sistemas POSIX.
- Los access/refresh tokens se cifran con Google Tink 1.24.0 (AES-256-GCM). El keyset se configura fuera de SQLite mediante `TWITCH_TOKEN_ENCRYPTION_KEYSET`; `./gradlew generateTwitchCredentialKeyset` genera uno nuevo.
- Pruebas unitarias cubren persistencia/reapertura, canales independientes, actualización de login/tokens, cifrado en disco, ausencia de clave y descifrado con keyset incorrecto.
- Esta base aún no está conectada al callback OAuth; la credencial activa sigue en la configuración global hasta validar identidad Twitch en la fase siguiente.

### Limitaciones aún abiertas

- Falta validar con Twitch la identidad/scopes del canal, renovar tokens y ejecutar la validación periódica.
- PubSub sigue pendiente de sustitución por EventSub; el onboarding guiado, Vue y el despliegue en Rivendel están planificados, no implementados.

## Registro de siguientes cambios

Para cada fase terminada, añadir una entrada fechada en `Unreleased` o en la versión publicada, describiendo solo cambios que hayan pasado sus pruebas y smoke checks. No copiar aquí credenciales, valores de `.properties` ni resultados que los revelen.
