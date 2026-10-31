#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
javac -encoding UTF-8 -d "$tmp" app/src/main/java/com/motohub/next/FuelAnomalyPolicy.java tests/UX83FuelSafetyTest.java
java -cp "$tmp" com.motohub.next.UX83FuelSafetyTest
python3 - <<'PY'
from pathlib import Path
p=Path('app/src/main/java/com/motohub/next')
def s(file):return (p/file).read_text()
main=s('MainActivity.java');ride=s('RideUi.java');notes=s('RideNotes.java')
fuel=s('QuickEntryUi.java');batch=s('ServiceBatch.java');audit=s('GarageIntegrity.java')
assert 'String savedMemo=RideNotes.memo(stableDb,file)' in ride
assert 'rideNotesSnapshot=liveNotes==null?' in ride
assert 'RideNotes.memo(a.db,file)' not in ride
assert 'date+" "+memo' in ride
assert 'RideStorageManager.confirm(this,track,()->close())' in main
assert 'RideNotes.clearDeleted(next,track.getName())' not in main
storage=s('RideStorageManager.java')
assert 'static void confirm(MainActivity a,File track)' in storage
assert 'RideTrash.move(a.getFilesDir(),track)' in storage
assert 'restoreDraftButton.setVisibility(View.GONE)' in fuel
assert 'restoreBatchDraftButton.setVisibility(View.GONE)' in main
assert 'BackupGpsProof.requireFullCurrent(this,db.toString(2))' in main
assert 'BackupGpsProof.recordVerifiedZip(this,checkedBackup.trackSha256,checkedBackup.sidecarSha256,snapshot,"FULL".equals(part))' in main
assert 'afterSuccess.run()' in storage
assert 'BackupGpsProof.status(a,track)' in storage
backup=s('BackupGpsProof.java')
assert 'full_db_sha256' in backup and 'requireFullCurrent' in backup
assert 'BackupGpsProof.fullStatus(a)' in s('HubScreens.java')
repair=s('JsonOrphanRepair.java')
merger=s('JsonDataMerger.java')
assert 'class JsonOrphanRepair' in repair and '_manualRelinkPlaceholder' in repair
assert 'selectOrphanJsonMergeBikes' in main
assert 'confirmed.containsKey(id)' in merger and '_manualRelinkPlaceholder' in merger
assert 'status.setText("✓ 모델명·연식 자동 입력 완료' in main
assert '이전 주유 날짜 확인' in fuel and '오늘 날짜로 변경' in fuel
assert 'FuelAnomalyPolicy.unusualLiters' in fuel and 'FuelAnomalyPolicy.lowOdometer' in fuel
assert 'FuelAnomalyPolicy.unusualAmount' in fuel
assert 'MaintenanceDueOverrides.apply' in main and 'dueKms' in main and 'dueDates' in main
assert 'GarageIntegrity.findings(checked)' in main and 'edit(found.collection,record)' in main
assert 'approvedRideBikeId' in main and 'final String fixedBikeId=a.approvedRideBikeId;' in ride
assert '기록할 바이크:' in ride and '이 바이크로 기록 시작' in ride
assert 'apksigner' in Path('../.github/workflows/moto-fixed-signed.yml').read_text()
publisher=Path('../.github/workflows/moto-publish-signed-on-version.yml').read_text()
assert 'cp "$APK" "$RUNNER_TEMP/MOTO-HUB.apk"' in publisher
assert 'gh release create "$TAG" "$RUNNER_TEMP/MOTO-HUB.apk"' in publisher
assert 'releases/download/$TAG/MOTO-HUB.apk' in publisher
ci=Path('../.github/workflows/moto-ux-validation.yml').read_text()
assert 'CODE="$(tr -d \'[:space:]\' < release-code.txt)"' in ci
assert '-PmotoReleaseCode="$CODE"' in ci and "versionCode='$CODE'" in ci
assert int(Path('release-code.txt').read_text().strip())>=553
assert 'bash tests/run-ux83-stabilization-tests.sh' in ci
print("PASS UX84: GPS reversible quarantine, full-backup guard, manual JSON relink, bike autofill, draft consistency; UX83 fuel/date warnings, per-item service overrides, audit drilldown and fixed GPS bike")
PY
