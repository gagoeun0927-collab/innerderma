#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

export JAVA_HOME="${JAVA_HOME:-/opt/temurin-21}"
export PATH="$JAVA_HOME/bin:$PATH"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$ROOT_DIR/.gradle}"

exec "$ROOT_DIR/gradlew" \
  --no-daemon \
  --init-script "$ROOT_DIR/scripts/gradle-wsl.init.gradle" \
  "$@"
