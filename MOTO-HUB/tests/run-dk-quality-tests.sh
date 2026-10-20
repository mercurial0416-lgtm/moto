#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" tests/support/org/json/JSONObject.java \
 app/src/main/java/com/motohub/next/{ServiceBatch,QuickLogDuplicate,QuickServiceHistory,RecentRecordReuse}.java \
 tests/ServiceBatchTest.java
java -cp "$out" com.motohub.next.ServiceBatchTest
javac -encoding UTF-8 -d "$out" \
 app/src/main/java/com/motohub/next/{BackupFormat,TrackFile,BackupBundle,BackupIntegrity,BackupReadiness,RestoreArchive,RestoreCapacity,BackupCrypto}.java \
 tests/ReceiptArchiveRoundtripTest.java
java -cp "$out" com.motohub.next.ReceiptArchiveRoundtripTest
python3 tests/test_dk_ui_wiring.py
