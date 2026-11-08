# Twitch Stream Chat Bot

A Java 25 and Spring Boot application that connects Twitch chat, executes commands loaded from `commands.json`, and receives Twitch EventSub events. The current web chat is a small static page backed by a WebSocket; it is not the planned Vue frontend.

## Requirements

- Linux development machine with Java 25. SDKMAN is optional; select Java 25 before running the Gradle wrapper.
- Network access for Gradle dependencies and Twitch OAuth/API calls.
- A Twitch application registered in the [Twitch Developer Console](https://dev.twitch.tv/console/apps).

The wrapper downloads the pinned Gradle version. The current local runtime is Gradle 9.5.1.

## Configure Twitch locally

1. Copy the safe template and protect the local file:

   ```sh
   cp .env.example .env
   chmod 600 .env
   ```

2. In the Twitch Developer Console, set the OAuth redirect URL to exactly `http://localhost:8080/callback/twitch`.
3. Generate a Tink keyset:

   ```sh
   ./gradlew generateTwitchCredentialKeyset
   ```

   Keep the generated Base64 value secret. Put it only in the ignored `.env` as `TWITCH_TOKEN_ENCRYPTION_KEYSET`; back it up securely. If the key is lost, persisted Twitch tokens cannot be decrypted.
4. Fill `TWITCH_CLIENT_ID`, `TWITCH_CLIENT_SECRET`, and `TWITCH_TOKEN_ENCRYPTION_KEYSET` in `.env`. Do not commit `.env`, copy credentials into documentation, or use production secrets for local development.

The local Twitch adapter requests `chat:read`, `chat:edit`, `moderator:read:followers`, `bits:read`, and `channel:read:subscriptions`.

## Start the backend locally

```sh
./dev-local.sh
```

The script requires Java 25, activates the `development` profile, creates a private data directory, and starts the backend on port 8080. By default, Flyway stores the local database at `.local/data/oauth-credentials.sqlite`; this path is ignored by Git and separate from the normal application database. The development profile overrides the database and Twitch settings used for local runs; local Twitch values come from `.env`.

Flyway applies the schema migrations on first start. The application loads `src/main/resources/commands.json` at startup and synchronizes those commands into SQLite by command name. To change this initial command set, edit that file and restart the backend. Online command editing and per-channel command management are not implemented yet.

Open `http://localhost:8080/login.html` and select the Twitch authorization button. Twitch redirects to `/callback/twitch`; after a successful callback, open `http://localhost:8080/` for the current chat page. The backend validates and stores OAuth credentials encrypted in SQLite, then connects Twitch chat and registers its configured EventSub subscriptions. A real Twitch consent and live-chat test has not been performed as part of this setup; use a test channel and account when you verify it.

The optional `DEVELOPMENT_CREATOR_EMAIL` and `DEVELOPMENT_CREATOR_PASSWORD` variables seed a separate local account for exercising the account module; they are not required for the Twitch OAuth path.

## Commands and events

The current JSON command file defines the `!` prefix and command responses, including `!help`. Commands are evaluated against messages from the connected Twitch channel and normal responses are sent back to that same channel. EventSub handlers are registered by the backend; receiving real events requires a valid Twitch authorization and a suitable event in the authorized channel.

## Tests

```sh
./gradlew test --warning-mode all
```

Unit tests use simulated provider responses and isolated databases; they do not log in to Twitch or send chat messages.

## Current scope

Twitch is the only integrated provider. The Vue 3 + Pinia + Vuetify frontend, light/dark theme, English/Spanish UI, creator channel-management UI, and OBS chat overlay with its own stable/revocable URL are planned, not implemented. See [`BACKLOG.md`](BACKLOG.md) for planned work and [`CHANGELOG.md`](CHANGELOG.md) for verified behavior.