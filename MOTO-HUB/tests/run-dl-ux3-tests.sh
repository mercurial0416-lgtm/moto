#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
javac -encoding UTF-8 -d "$TMP" tests/support/org/json/JSONObject.java \
 app/src/main/java/com/motohub/next/{ReceiptSlots,BackupFormat,BatchDraftPolicy,RecentRecordReuse,QuickServiceHistory,QuickLogDuplicate,ServiceBatch,ServiceBatchPreset,FuelStats}.java \
 tests/Ux3ConvenienceTest.java
java -cp "$TMP" com.motohub.next.Ux3ConvenienceTest
javac -encoding UTF-8 -d "$TMP" \
 app/src/main/java/com/motohub/next/{ReceiptSlots,BackupFormat,TrackFile,BackupBundle,BackupIntegrity,BackupReadiness,RestoreArchive,RestoreCapacity,BackupCrypto}.java \
 tests/ReceiptSlotsRoundTripTest.java
java -cp "$TMP" com.motohub.next.ReceiptSlotsRoundTripTest
javac -encoding UTF-8 -d "$TMP" app/src/main/java/com/motohub/next/ReceiptTextParser.java tests/ReceiptTextParserTest.java
java -cp "$TMP" com.motohub.next.ReceiptTextParserTest
python3 tests/test_dl_ux3_wiring.py
