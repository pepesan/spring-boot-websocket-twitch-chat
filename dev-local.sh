#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"
local_env_file="${DEV_LOCAL_ENV_FILE-.env}"
if [[ -n "$local_env_file" && -f "$local_env_file" ]]; then
  set -a
  # .env is a trusted, user-owned shell-compatible file; do not commit it.
  source "$local_env_file"
  set +a
fi

java_path=java
if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
  java_path="$JAVA_HOME/bin/java"
fi
if [[ "$("$java_path" -version 2>&1)" != *'version "25.'* ]]; then
  java_path=
  for candidate in "$HOME"/.sdkman/candidates/java/25*; do
    if [[ -x "$candidate/bin/java" && "$("$candidate/bin/java" -version 2>&1)" == *'version "25.'* ]]; then
      export JAVA_HOME="$candidate"
      export PATH="$JAVA_HOME/bin:$PATH"
      java_path="$JAVA_HOME/bin/java"
      break
    fi
  done
fi
if [[ -z "$java_path" ]]; then
  echo 'Java 25 is required. Select Java 25 with SDKMAN or configure JAVA_HOME.' >&2
  exit 1
fi

export APP_DATA_DIR="${APP_DATA_DIR:-$PWD/.local/data}"
mkdir -p "$APP_DATA_DIR"
chmod 700 "$APP_DATA_DIR"
if [[ "$APP_DATA_DIR" == "$PWD/.local/data" ]]; then
  chmod 700 "$PWD/.local"
fi
export SPRING_PROFILES_ACTIVE=development
exec ./gradlew bootRun --args="--server.port=${SERVER_PORT:-8080}"
