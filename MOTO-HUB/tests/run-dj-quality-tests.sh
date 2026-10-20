#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
BUILD="$(mktemp -d)";trap 'rm -rf "$BUILD"' EXIT
javac -encoding UTF-8 -d "$BUILD" \
  app/src/main/java/com/motohub/next/LocalDiagnosticReport.java tests/LocalDiagnosticsTest.java \
  app/src/main/java/com/motohub/next/UpdateCacheRetention.java tests/UpdateCacheRetentionTest.java
java -cp "$BUILD" com.motohub.next.LocalDiagnosticsTest
java -cp "$BUILD" com.motohub.next.UpdateCacheRetentionTest
python3 tests/test_dj_source.py
