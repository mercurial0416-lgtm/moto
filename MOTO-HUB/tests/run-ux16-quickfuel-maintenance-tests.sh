#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${JSON_JAR:?CI supplies org.json Java jar}"
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -cp "$JSON_JAR" -d "$out" \
 app/src/main/java/com/motohub/next/{Store,FuelRecordValidation,QuickRecordDraft,MaintenanceNotificationSummary,MaintenanceAlertWindow,GarageBikeStatus,LatestServiceByType,RecentRecordReuse,MaintenancePlanner,HomeMaintenanceStatus,MaintenanceNoticeText}.java \
 tests/Ux16QuickLogMaintenanceTest.java
java -cp "$JSON_JAR:$out" com.motohub.next.Ux16QuickLogMaintenanceTest
python3 - <<'PY'
from pathlib import Path
root=Path("app/src/main/java/com/motohub/next")
ui=(root/"QuickEntryUi.java").read_text()
home=(root/"GarageHomeUi.java").read_text()
records=(root/"HubScreens.java").read_text()
main=(root/"MainActivity.java").read_text()
job=(root/"DailyMaintenanceJob.java").read_text()
assert 'QuickRecordDraft.fuel(' in ui and 'QuickRecordDraft.maintenance(' in ui
assert '주유 금액 (원)' in ui and '실제 주유량 (L)' in ui
assert 'extra.setVisibility(View.GONE)' in ui
assert '저장' in ui and 'a.write(next)' in ui
assert 'QuickLogDuplicate.exists(' in ui, 'warn on accidental double taps'
assert 'a.pickDate(date)' in ui and 'a.pickDate(nextDate)' in ui
assert 'maintenance_opt_in' in ui and 'requestPermissions(' in ui and 'DailyMaintenanceJob.schedule(a)' in ui
assert 'new QuickEntryUi(a).fuel()' in home and 'new QuickEntryUi(a).maintenance()' in home
assert 'new QuickEntryUi(a).fuel()' in records and 'new QuickEntryUi(a).maintenance()' in records
assert 'new QuickEntryUi(this).fuel()' in main and 'new QuickEntryUi(this).maintenance()' in main
assert main.count('DailyMaintenanceJob.checkSoon(this)')>=4, 'original editor changes retrigger deadline checks'
assert 'MaintenanceNotificationSummary.from(db,today)' in job
assert '.setContentText(reminder.detail)' in job and '.setVisibility(Notification.VISIBILITY_PRIVATE)' in job
assert 'if(remindersEnabled(this)&&!RideService.running)' in job, 'never alert without opt-in or while riding'
print('PASS UX16 quick forms, notification consent, privacy, duplicate warning, save and scheduling wiring')
PY
