#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)";trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" \
 app/src/main/java/com/motohub/next/{BackupFormat,BackupBundle,BackupIntegrity,RestoreArchive,RestoreCapacity,TrackFile,ReceiptFiles}.java \
 tests/MultiReceiptArchiveTest.java
java -cp "$out" com.motohub.next.MultiReceiptArchiveTest
