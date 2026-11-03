#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
src=Path("app/src/main/java/com/motohub/next")
read=lambda name:(src/name).read_text(encoding="utf8")
summary=read("RecordMonthlySummary.java")
stats=read("RideStatsUi.java")
obd=read("DeveloperObdScreen.java")
log=read("ObdLocalDiagnosticLog.java")
restore=read("RestoreArchive.java")
main=read("MainActivity.java")
mapui=read("RideMapActivity.java")
replay=read("RideReplayTimeline.java")
checks={
 "future/planned expenses excluded": 'item.optBoolean("scheduleOnly")||future(' in summary and 'LocalDate.now(ZoneId.of("Asia/Seoul"))' in summary,
 "both cost screens explain unpaid": 'totals.scheduledOrFuture' in read("HubScreens.java") and 'unpaidExcluded' in stats,
 "confirmed unsupported OBD rows hidden": 'unsupported?android.view.View.GONE:android.view.View.VISIBLE' in obd,
 "OBD PID bitmap not mistaken for real reply": '"observed_mask"' in log and 'ObdLocalDiagnosticLog.observed(activity,pid)' in obd,
 "GPS map does not join loss intervals": 'MapPathContinuity.validLink' in mapui and 'RideReplayTimeline.gapAt(replaySections,index)' in mapui,
 "full restore verifies committed JSON and media": 'verifyInstalled(File internal)' in restore and 'sameContent(from,installed)' in restore and 'ready.verifyInstalled(getFilesDir())' in main,
 "post-restore audit never rolls back silently": '복원 후 파일 재검증 실패' in main and 'before-import.json' in main,
 "source does not publish unsigned APK": 'PUBLISH_UNSIGNED' not in summary and 'replaySections' in mapui
}
for name,okay in checks.items():print(("PASS" if okay else "FAIL")+" v573: "+name)
assert all(checks.values()),"v573 reliability regressions"
PY
