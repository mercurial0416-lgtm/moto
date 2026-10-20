#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
javac -encoding UTF-8 -d "$TMP" tests/support/org/json/JSONObject.java \
 app/src/main/java/com/motohub/next/{RecentRecordReuse,LatestServiceByType}.java \
 tests/LatestServiceByTypeTest.java
java -cp "$TMP" com.motohub.next.LatestServiceByTypeTest
bash tests/run-quick-log-tests.sh
bash tests/run-dm-ux4-tests.sh
