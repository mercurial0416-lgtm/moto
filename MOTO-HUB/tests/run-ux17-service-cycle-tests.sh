#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${JSON_JAR:?CI supplies org.json Java jar}"
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -cp "$JSON_JAR" -d "$out" \
 app/src/main/java/com/motohub/next/{Store,FuelRecordValidation,QuickRecordDraft,ServiceCycle,FuelAutoAmount,MaintenanceNotificationSummary,MaintenanceAlertWindow,GarageBikeStatus,LatestServiceByType,RecentRecordReuse,MaintenancePlanner,HomeMaintenanceStatus,MaintenanceNoticeText}.java \
 tests/Ux17ServiceCycleTest.java tests/FuelAutoAmountTest.java
java -cp "$JSON_JAR:$out" com.motohub.next.Ux17ServiceCycleTest
java -cp "$JSON_JAR:$out" com.motohub.next.FuelAutoAmountTest
python3 - <<'PY'
from pathlib import Path
root=Path('app/src/main/java/com/motohub/next')
quick=(root/'QuickEntryUi.java').read_text()
dashboard=(root/'ServiceStatusUi.java').read_text()
job=(root/'DailyMaintenanceJob.java').read_text()
garage=(root/'HubScreens.java').read_text()
assert 'FuelAutoAmount fuelAuto=new FuelAutoAmount()' in quick
assert 'fuelAuto.update(' in quick and 'amount.setText(nextAmount)' in quick
assert 'fuelAuto.reset()' in quick and quick.index('calculator.setVisibility(View.GONE);\n    fuelAuto.reset();') < quick.index('amount.setText(String.valueOf((long)previous.optDouble("amount")))'), 'last fill restore cannot be overwritten by active calculator'
assert 'if(full.isChecked()){extra.setVisibility(View.VISIBLE);return;}' in quick, 'full tank requires visible odometer field'
assert '지난 주유 금액·리터 불러오기' in quick
assert 'alertKmBefore' in quick and 'alertDaysBefore' in quick
assert 'kmBeforeChoices={100,300,500}' in quick and 'daysBeforeChoices={7,15,30}' in quick
assert 'ServiceCycle.completed(' in dashboard
assert 'Store.upsert(next,"services",done)' in dashboard
assert 'DailyMaintenanceJob.checkSoon(a)' in dashboard
assert 'Store.find(next,"vehicles",bike.optString("id")).put("odo",km)' in dashboard
assert 'QuickLogDuplicate.same(x,done,"services")' in dashboard
assert 'MaintenanceAlertWindow.status(item,a.today(),bike.optDouble("odo"))' in dashboard
assert 'new ServiceStatusUi(a).render(a.content)' in garage
assert 'String stamp="service:"+reminder.fingerprint' in job, 'no daily spam for identical reminders'
assert 'static void clearReminderState(Context context)' in job, 'opt-out clears saved alert state'
assert '.remove("last_maintenance_notice").apply()' in job, 'old dedupe key removed on opt-out'
assert 'else if(!enabled)clearReminderState(this)' in job, 'background opt-out clears old alerts'
assert 'if(!manager.areNotificationsEnabled()){clearReminderState(this);return;}' in job, 'revoked OS notification access clears old alerts'
settings=(root/'HubScreens.java').read_text()
assert 'if(enabled)DailyMaintenanceJob.clearReminderState(a)' in settings, 'settings opt-out clears pending badge and dedupe'
print('PASS UX17 native UI, service rearm, explicit mileage and opt-in reminder lifecycle wiring')
PY
