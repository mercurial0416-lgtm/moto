#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
bin="$(mktemp -d)"
trap 'rm -rf "$bin"' EXIT
javac -encoding UTF-8 -d "$bin" \
  app/src/main/java/com/motohub/next/ServerSettings.java \
  tests/ServerSettingsTest.java
java -cp "$bin" com.motohub.next.ServerSettingsTest
