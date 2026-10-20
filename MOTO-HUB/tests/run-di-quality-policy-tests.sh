#!/usr/bin/env bash
# This check is pure JVM + sources. It never invokes Gradle / Android build tools.
set -euo pipefail
cd "$(dirname "$0")/.."
B=$(mktemp -d)
trap 'rm -rf "$B"' EXIT
javac -encoding UTF-8 -d "$B" \
 app/src/main/java/com/motohub/next/{UpdateReadiness,UpdateStoragePolicy,RideReadinessReport,SharingPrivacyPolicy,AutoPhotoCache}.java \
 tests/DiQualityPolicyTest.java
java -cp "$B" com.motohub.next.DiQualityPolicyTest
python3 tests/test_di_quality_source.py
