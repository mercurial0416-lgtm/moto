#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" \
  tests/support/org/json/JSONObject.java \
  app/src/main/java/com/motohub/next/ReceiptFiles.java \
  app/src/main/java/com/motohub/next/ReceiptTextParser.java \
  app/src/main/java/com/motohub/next/BatchDraft.java \
  app/src/main/java/com/motohub/next/BackupFormat.java \
  app/src/main/java/com/motohub/next/FuelEfficiency.java \
  app/src/main/java/com/motohub/next/ServiceIntervals.java \
  app/src/main/java/com/motohub/next/ServiceBatch.java \
  app/src/main/java/com/motohub/next/QuickServiceHistory.java \
  app/src/main/java/com/motohub/next/RecentRecordReuse.java \
  app/src/main/java/com/motohub/next/QuickLogDuplicate.java \
  tests/Ux3ConvenienceTest.java
java -cp "$out" com.motohub.next.Ux3ConvenienceTest
