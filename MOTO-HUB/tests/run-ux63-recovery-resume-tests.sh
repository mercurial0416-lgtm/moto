#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
root=Path("app/src/main/java/com/motohub/next")
main=(root/"MainActivity.java").read_text()
ride=(root/"RideUi.java").read_text()
notice=(root/"AppUpdateNotice.java").read_text()
policy=(root/"InterruptedRideReminderPolicy.java").read_text()
widget=(root/"MotoWidgetProvider.java").read_text()
assert 'SNOOZE_MS=24L*60*60*1000' in policy
assert 'candidate.equals(acknowledged)' in policy
assert '"acknowledged"' in main and '"snoozed_file"' in main
assert '"seen",filename' not in main
assert 'prefs.edit().putString("acknowledged",filename)' in main
assert 'setNegativeButton("내일 다시 알림"' in main
assert 'if(!RideService.running&&!RideService.autoArmed&&!readOnly' in main
assert 'recoveryReminderChecking.compareAndSet(false,true)' in main
assert 'interruptedRecoveryPromptVisible' in main
assert 'suppressUpdatePromptUntilElapsed' in main
assert 'a.suppressUpdatePrompt()' in notice
assert 'a.suppressUpdatePromptUntilElapsed=' in ride
assert 'rideStateSignature()!=lastRenderedRideState' in main
assert 'if("라이딩".equals(tab))lastRenderedRideState=rideStateSignature();' in main
assert 'renderPreservingPosition()' in main
assert 'final String[] tabs={"홈","라이딩","차고","더보기"}' in main
assert 'if(java.util.Arrays.equals(ids,lastWidgetIds)&&navigator.equals(lastNavigationChoice))return;' in widget
assert 'MotoWidgetProvider.refresh(this)' in main
assert 'new RideUi(this).requestStart(true)' in main
assert 'rideStartConsent.consume(' in ride
assert 'RideService.STOP' in ride
print("PASS UX63: GPS crash recovery, snooze and consent, nav return, no modal collision, idle widgets")
PY
