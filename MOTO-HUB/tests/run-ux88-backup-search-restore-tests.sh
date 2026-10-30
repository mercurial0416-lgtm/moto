#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
p=Path("app/src/main/java/com/motohub/next")
main=(p/"MainActivity.java").read_text()
gps=(p/"BackupGpsProof.java").read_text()
integrity=(p/"BackupIntegrity.java").read_text()
hub=(p/"HubScreens.java").read_text()
trash=(p/"RideTrash.java").read_text()
storage=(p/"RideStorageManager.java").read_text()
backup=(p/"BackupBundle.java").read_text()
format=(p/"BackupFormat.java").read_text()
preferences=(p/"DevicePreferencesBackup.java").read_text()
drafts=(p/"FuelDraftBackup.java").read_text()
tests=Path("app/src/test/java/com/motohub/next/UX88BackupProofAndDraftTest.java").read_text()
checks={
 "readback-sidecar SHA calculated":'sidecarHashes.put(name,hex.toString())' in integrity,
 "readback track+metadata recorded":'checkedBackup.trackSha256,checkedBackup.sidecarSha256' in main,
 "GPS old proof not allowed to remove":'sidecar_sha256' in gps and 'if(raw==null||raw.isEmpty())throw new IOException' in gps,
 "GPS added marker blocked":'백업 후 추가됐습니다' in gps,
 "GPS missing marker blocked":'백업된 GPS 부속 파일이 사라졌습니다' in gps,
 "GPS changed marker blocked":'GPS 부속 파일이 백업 이후 변경됐습니다' in gps,
 "all GPS metadata types audited":'for(String suffix:RideTrash.SIDECARS)' in gps,
 "global results date sorted":'newestFirst(hits);' in hub and 'hits.add(new UnifiedSearchHit(ride.date' in hub,
 "all search hits paged together":'UnifiedSearchHit hit=hits.get(i);' in hub,
 "fuel drafts bounded before export":'FuelDraftBackup.snapshot(c)' in preferences and 'MAX_DRAFTS=32' in drafts,
 "fuel drafts validated before database import":'FuelDraftBackup.validate(json.getJSONObject("fuelDrafts"))' in preferences,
 "fuel drafts restored only after JSON commit":'DevicePreferencesBackup.restore(this,ready.preferences,db)' in main,
 "fuel drafts only on known bikes":'Store.find(safe,"vehicles",bike)==null' in drafts,
 "no clearing unrelated fuel drafts":'edit.putString("fuel_"+bike,incoming.toString())' in drafts,
 "backup preference space consistent":'return 32_768;' in format and 'if(ui.length>32_768)' in backup,
 "GPS restored successfully before cleanup":'folder.delete();' in trash and 'GPS는 복원됐으나 빈 복구함 정리에 실패' not in trash,
 "restored GPS cleanup warning distinguished":'빈 복구함 폴더는 다음 실행 때 정리됩니다.' in storage,
 "real APK/JVM regressions present":'zipProofRejectsChangedOrMissingGpsSidecars' in tests and 'fullZipDraftSnapshotOnlyRestoresKnownBike' in tests and 'globalSearchReallySortsGpsBetweenLedgerDates' in tests,
}
for name,ok in checks.items():print(('PASS' if ok else 'FAIL')+' UX88: '+name)
assert all(checks.values())
PY
