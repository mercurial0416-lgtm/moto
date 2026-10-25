#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
bin=$(mktemp -d)
trap 'rm -rf "$bin"' EXIT
javac -encoding UTF-8 -d "$bin" app/src/main/java/com/motohub/next/ReceiptSlots.java app/src/main/java/com/motohub/next/BackupFormat.java app/src/main/java/com/motohub/next/TrackFile.java app/src/main/java/com/motohub/next/BackupBundle.java app/src/main/java/com/motohub/next/BackupIntegrity.java tests/BackupBundleTest.java tests/ModelPhotoMetadataTest.java
java -cp "$bin" com.motohub.next.BackupBundleTest
java -cp "$bin" com.motohub.next.ModelPhotoMetadataTest
