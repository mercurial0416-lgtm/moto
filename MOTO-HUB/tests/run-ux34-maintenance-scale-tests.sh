#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${JSON_JAR:?CI supplies org.json Java jar}"
dir="$(mktemp -d)"
trap 'rm -rf "$dir"' EXIT
javac -encoding UTF-8 -cp "$JSON_JAR" -d "$dir" \
 app/src/main/java/com/motohub/next/{Store,MaintenanceNotificationSummary,MaintenanceAlertWindow,GarageBikeStatus,LatestServiceByType,RecentRecordReuse,MaintenancePlanner,HomeMaintenanceStatus,MaintenanceNoticeText}.java \
 tests/MaintenanceIndexScaleTest.java
java -cp "$JSON_JAR:$dir" com.motohub.next.MaintenanceIndexScaleTest
python3 - <<'PY'
from pathlib import Path
s=Path('app/src/main/java/com/motohub/next/MaintenanceNotificationSummary.java').read_text()
assert 'Map<String,List<JSONObject>> byBike=new HashMap<>()' in s
assert 'byBike.computeIfAbsent(' in s
assert 'for(int j=0;j<services.length();j++)' in s
assert 'List<JSONObject> history=byBike.getOrDefault(' in s
assert 'signature.append(bikeId)' in s
assert 'append(service.optString("nextDate"))' in s
print("PASS indexed maintenance summary wiring")
PY
