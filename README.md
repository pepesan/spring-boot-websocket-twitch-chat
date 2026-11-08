# Twitch Stream Chat Bot

A Java 25 and Spring Boot application that connects to Twitch chat, executes commands loaded from `commands.json`, and receives Twitch EventSub events. The repository now includes a Vue development frontend alongside the original Spring-served static page.

## Requirements

- Linux development machine with Java 25. SDKMAN is optional; select Java 25 before running the Gradle wrapper.
- Node.js 24 and npm 11 for the frontend.
- Network access for Gradle/npm dependencies and Twitch OAuth/API calls.
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

## Start the backend and frontend locally

In the first terminal:

```sh
./dev-local.sh
```

The script requires Java 25, activates the `development` profile, creates a private data directory, and starts the backend on port 8080. By default, Flyway stores the local database at `.local/data/oauth-credentials.sqlite`; this path is ignored by Git and separate from the normal application database. The development profile overrides the database and Twitch settings used for local runs; local Twitch values come from `.env`.

In a second terminal:

```sh
cd frontend
npm ci
npm run dev
```

Open `http://localhost:5173`. Vite proxies `/account`, `/callback`, and `/websocket` to the backend on port 8080. Keep the browser host as `localhost` so the OAuth session cookie matches the callback host.

The required order is: create an application account (password at least 12 characters), verify the email using the sent link, sign in, then choose **Connect with Twitch**. The development profile expects an SMTP server at `localhost:1025`; for example, run Mailpit with `docker run --rm -p 1025:1025 -p 8025:8025 axllent/mailpit` and inspect messages at `http://localhost:8025`. The verification link goes through Vite to the backend and returns to the frontend. OAuth start requires the signed-in application session; Twitch's callback remains public but validates its session-bound, single-use `state`.

Each new Twitch authorization is linked in SQLite to the signed-in creator account. An authorization already owned by another creator cannot be reassigned.

Flyway applies the schema migrations on first backend start. The application loads `src/main/resources/commands.json` and synchronizes those commands into SQLite by command name. Online command editing and per-channel command management are not implemented.

After signing in, the live chat is at `http://localhost:5173/chat`. The OBS browser-source URL in local development is `http://localhost:5173/embed/chat`; it has a transparent background and displays the same read-only WebSocket chat. The WebSocket endpoint is unauthenticated and the overlay has no revocable access key, so keep both services local and do not expose them to the Internet.

The optional `DEVELOPMENT_CREATOR_EMAIL` and `DEVELOPMENT_CREATOR_PASSWORD` variables seed a pre-verified application account for local sign-in; they are not Twitch credentials and are not required if you register through the UI.

## Commands and events

The current JSON command file defines the `!` prefix and command responses, including `!help`. Commands are evaluated against messages from the connected Twitch channel and normal responses are sent back to that same channel. EventSub handlers are registered by the backend; receiving real events requires a valid Twitch authorization and a suitable event in the authorized channel.

## Tests

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

The Cucumber E2E script starts its own local Vite server; stop any separately started frontend first. Tests use no real Twitch consent or fabricated chat messages.

## Current scope

Twitch is the only integrated provider. Implemented: Vue 3 + Pinia + Vuetify UI, light/dark theme, English/Spanish preferences, creator-account registration/email verification/sign-in, account-gated Twitch OAuth, read-only live chat, and a local transparent OBS view. Creator/channel management, guided multi-step onboarding, online/per-channel command editing, `!pole`, questions, overlay access control/revocation, and overlay styling customization remain pending. See [`BACKLOG.md`](BACKLOG.md) for planned work and [`CHANGELOG.md`](CHANGELOG.md) for verified behavior.