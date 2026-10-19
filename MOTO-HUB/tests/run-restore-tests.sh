#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p /tmp/moto-hub-restore-test
javac -encoding UTF-8 -d /tmp/moto-hub-restore-test app/src/main/java/com/motohub/next/BackupFormat.java app/src/main/java/com/motohub/next/TrackFile.java app/src/main/java/com/motohub/next/RestoreArchive.java tests/RestoreArchiveTest.java
java -cp /tmp/moto-hub-restore-test com.motohub.next.RestoreArchiveTest
