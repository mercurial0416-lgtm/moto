#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
TEMP="$(mktemp -d)"
trap 'rm -rf "$TEMP"' EXIT
javac -encoding UTF-8 -d "$TEMP" tests/support/org/json/JSONObject.java \
  app/src/main/java/com/motohub/next/{BackupFormat,ReceiptSlots,BatchDraft,ReceiptTextHints,FuelTrend,ServiceBatch,QuickServiceHistory,QuickLogDuplicate,RecentRecordReuse}.java \
  tests/ReceiptUx3Test.java
java -cp "$TEMP" com.motohub.next.ReceiptUx3Test
javac -encoding UTF-8 -d "$TEMP" app/src/main/java/com/motohub/next/{ReceiptSlots,BackupFormat,TrackFile,BackupBundle,BackupIntegrity,BackupReadiness,RestoreArchive,RestoreCapacity,BackupCrypto}.java \
  tests/ReceiptMultiArchiveTest.java
java -cp "$TEMP" com.motohub.next.ReceiptMultiArchiveTest
python3 tests/test_ux3_source.py

javac -encoding UTF-8 -d "$TEMP" app/src/main/java/com/motohub/next/ManualServiceIntervals.java tests/ManualIntervalsTest.java
java -cp "$TEMP" com.motohub.next.ManualIntervalsTest
