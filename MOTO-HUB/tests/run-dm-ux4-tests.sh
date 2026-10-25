#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
javac -encoding UTF-8 -d "$TMP" app/src/main/java/com/motohub/next/ReceiptCameraFiles.java tests/ReceiptCameraFilesTest.java
java -cp "$TMP" com.motohub.next.ReceiptCameraFilesTest
javac -encoding UTF-8 -d "$TMP" app/src/main/java/com/motohub/next/ReceiptTextParser.java tests/ReceiptTextParserChoicesTest.java tests/ReceiptTextParserTest.java
java -cp "$TMP" com.motohub.next.ReceiptTextParserChoicesTest
java -cp "$TMP" com.motohub.next.ReceiptTextParserTest
python3 tests/test_dm_camera_ocr_wiring.py
