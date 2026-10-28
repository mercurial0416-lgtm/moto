#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
dir="$(mktemp -d)"
trap 'rm -rf "$dir"' EXIT
javac -encoding UTF-8 -d "$dir" \
  app/src/main/java/com/motohub/next/UpdateRecoveryPolicy.java \
  app/src/main/java/com/motohub/next/UpdateErrorText.java \
  tests/UpdateRecoveryPolicyTest.java \
  tests/UpdateErrorTextTest.java
java -cp "$dir" com.motohub.next.UpdateRecoveryPolicyTest
java -cp "$dir" com.motohub.next.UpdateErrorTextTest
python3 - <<'PY'
from pathlib import Path
src=Path("app/src/main/java/com/motohub/next")
read=lambda file:(src/file).read_text(encoding="utf-8")
update,backup,snapshot,quick,garage,service,listing,ride=map(read,[
 "AppUpdater.java","LocalSafetyBackup.java","UpdateInstallSnapshot.java","QuickEntryUi.java",
 "GarageHomeUi.java","ServiceStatusUi.java","HubScreens.java","RideService.java"])
checks={
 "pinned file retained":'UpdateInstallSnapshot.isPinned(context,candidate.getName())' in backup,
 "old backup protected":'last_good_snapshot' in snapshot,
 "pinned name tested":'UpdateRecoveryPolicy.isPinned(filename' in snapshot,
 "cancel button":'cancelDownloadButton=a.button("다운로드 중단"' in update,
 "cancel signal":'cancelRequested.set(true)' in update,
 "cancel observed":'checkCancelled();' in update,
 "cancellation handled separately":'catch(DownloadCancelledException cancelled)' in update,
 "partial retained":'m.sha256+".part.apk"' in update,
 "resume guarded":'UpdateResumePolicy.validPartial(' in update,
 "checksum enforced":'!hex(digest.digest()).equals(m.sha256)' in update,
 "installation signing":'validateArchive(f,m)' in update,
 "input alerts collapsed":'LinearLayout alertOptions=a.col();alertOptions.setVisibility(View.GONE);' in quick,
 "empty schedule blocked":'if(kmInterval==0&&monthsInterval==0)' in quick,
 "spinner fits text":'spinner.setMinimumHeight(a.dp(56))' in quick,
 "small-screen home":'screenWidthDp<360' in garage,
 "small-screen maintenance":'screenWidthDp<360' in service,
 "retired listings renderer":'void listings(){' not in listing,
 "ride notify safe":'void notifySafely(String message)' in ride,
 "ride available check":'boolean notificationsAvailable()' in ride,
 "no raw telemetry notify":'.notify(404,notification(status))' not in ride,
 "readable errors":'UpdateErrorText.forError(e)' in update,
 "preinstall protected":'UpdateInstallSnapshot.create(a,a.disk)' in update,
 "GPS install guard":'UpdateSafety.canInstall(' in update,
}
failed=[name for name,ok in checks.items() if not ok]
assert not failed,failed
print(f"PASS UX43 integration guards: {len(checks)}/{len(checks)}")
PY
