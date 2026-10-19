#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${JSON_JAR:?Set org.json:json:20240303 JAR path}"
mkdir -p tests/classes
javac -encoding UTF-8 -cp "$JSON_JAR" -d tests/classes app/src/main/java/com/motohub/next/Store.java tests/StoreTest.java tests/FuelTest.java
java -cp "$JSON_JAR:tests/classes" StoreTest
java -cp "$JSON_JAR:tests/classes" FuelTest
