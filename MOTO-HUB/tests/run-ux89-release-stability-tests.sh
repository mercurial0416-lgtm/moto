#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
root=Path("app/src/main/java/com/motohub/next")
trash=(root/"RideTrash.java").read_text()
drafts=(root/"FuelDraftBackup.java").read_text()
prefs=(root/"DevicePreferencesBackup.java").read_text()
audit=(root/"UpdateDataFootprint.java").read_text()
install=(root/"UpdateInstallSnapshot.java").read_text()
hub=(root/"HubScreens.java").read_text()
unit=Path("app/src/test/java/com/motohub/next/UX89FinalHardeningTest.java").read_text()
checks={
 "empty trash cleanup is not fatal":'if(leftover.length==0)folder.delete();' in trash and 'if(!folder.delete())throw new IOException("빈 GPS 복구함 폴더 정리 실패");' not in trash,
 "unexpected leftovers remain fatal":'GPS 복원 후 복구함에 알 수 없는 잔여 파일이 있습니다.' in trash,
 "draft rollback is scoped to fuel IDs":'static boolean rollback(Context c,Map<String,?> previous)' in drafts and 'RideBikeBinding.valid(key.substring(5))' in drafts,
 "draft rollback invoked on any settings failure":'boolean fuelOk=FuelDraftBackup.rollback(c,oldFuel);' in prefs,
 "draft rollback failure is reported":'!homeOk||!navOk||!notifyOk||!fuelOk' in prefs,
 "update schema6 fingerprints draft values":'JsonCanonicalDigest.hash(FuelDraftBackup.snapshot(context))' in audit and 'fuelDraftSha256=' in audit,
 "old schema4 baseline handled unchanged":'if(original.startsWith("schema4;"))after=UpdateDataFootprint.captureLegacy(context,current);' in install,
 "old schema5 baseline handled unchanged":'else if(original.startsWith("schema5;"))after=UpdateDataFootprint.captureLegacy5(context,current);' in install,
 "new schema6 compares draft hash":'else if(original.startsWith("schema6;"))after=UpdateDataFootprint.capture(context,current);' in install,
 "GPS search uses true recorded time":'Long started=RideHistoryLabel.startTime(track);' in hub and 'ride.startedAt' in hub,
 "same-day record time only when known":'static long ledgerSortTime(JSONObject record,String day)' in hub,
 "same-day sorted before pagination":'Long.compare(right.sortTime,left.sortTime)' in hub and 'newestFirst(hits);' in hub,
 "IME debounce prevents repeated full sort":'input.postDelayed(searchRefresh,180);' in hub and 'input.removeCallbacks(searchRefresh);' in hub,
 "search dismiss cleans callback":'dialog.setOnDismissListener(d->input.removeCallbacks(searchRefresh));' in hub,
 "Android regression exercises backup and search":'draftsChangeOnlyNewUpdateFingerprintAndLegacyScopesStayStable' in unit and 'rollbackRestoresOldFuelWithoutTouchingUnrelatedPrefs' in unit and 'sameDateSortUsesRealTimesAndUnknownTimestampsStayStable' in unit,
}
for name,ok in checks.items():print(("PASS" if ok else "FAIL")+" UX89: "+name)
assert all(checks.values()),"UX89 stabilization regression"
PY
