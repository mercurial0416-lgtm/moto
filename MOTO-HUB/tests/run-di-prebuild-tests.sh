#!/usr/bin/env bash
# Pure Java & source checks only. NEVER invokes Gradle/Android assemble or signs APKs.
set -euo pipefail
cd "$(dirname "$0")/.."
B=$(mktemp -d)
trap 'rm -rf "$B"' EXIT
javac -encoding UTF-8 -d "$B" \
  app/src/main/java/com/motohub/next/{BackupFormat,PrivateCacheCleanup,ListPageLimit,UpdateDownloadPolicy}.java \
  tests/{PrivateCacheCleanupTest,ListPageLimitTest,UpdateDownloadPolicyTest}.java
java -cp "$B" com.motohub.next.PrivateCacheCleanupTest
java -cp "$B" com.motohub.next.ListPageLimitTest
java -cp "$B" com.motohub.next.UpdateDownloadPolicyTest
python3 tests/test_di_prebuild.py
python3 tests/test_di_update_manifest.py
