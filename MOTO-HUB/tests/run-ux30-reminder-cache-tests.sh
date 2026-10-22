#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
bin="$(mktemp -d)"
trap 'rm -rf "$bin"' EXIT
javac -encoding UTF-8 -d "$bin" \
 app/src/main/java/com/motohub/next/{ReceiptSlots,BackupFormat,AutoPhotoCache,MaintenanceNoticeThrottle}.java \
 tests/{AutoPhotoCacheStrictTest,MaintenanceNoticeThrottleTest,ModelPhotoMetadataTest}.java
java -cp "$bin" com.motohub.next.AutoPhotoCacheStrictTest
java -cp "$bin" com.motohub.next.MaintenanceNoticeThrottleTest
java -cp "$bin" com.motohub.next.ModelPhotoMetadataTest
python3 - <<'PY'
from pathlib import Path
j=Path("app/src/main/java/com/motohub/next/DailyMaintenanceJob.java").read_text()
c=Path("app/src/main/java/com/motohub/next/AutoPhotoCache.java").read_text()
b=Path("app/src/main/java/com/motohub/next/BootRideReminder.java").read_text()
assert 'MaintenanceNoticeThrottle.shouldNotify(' in j
assert 'prefs.getLong("last_maintenance_notice_at",0L)' in j
assert 'putLong("last_maintenance_notice_at",now)' in j
assert j.count('remove("last_maintenance_notice_at")')==2
assert 'if(enabled&&!RideService.running)sendMaintenanceReminder(db)' in j
assert 'DailyMaintenanceJob.schedule(context);' in b and 'DailyMaintenanceJob.checkSoon(context);' in b
assert 'BackupFormat.automaticallyCachedModelMetadata(json)' in c
assert 'json.matches(' not in c
print("PASS opted-in repeat schedule and protected cache cleanup wiring")
PY
