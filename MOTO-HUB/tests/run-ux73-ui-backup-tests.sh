#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
p=Path("app/src/main/java/com/motohub/next")
g=lambda x:(p/x).read_text()
hub=g("HubScreens.java")
home=g("GarageHomeUi.java")
ride=g("RideTrash.java")
store=g("RideStorageManager.java")
rest=g("RestoreArchive.java")
export=g("BackupBundle.java")
main=g("MainActivity.java")
archive=g("BatchArchiveExport.java")
assert hub.index('line("고급 설정 · 문제 해결"')>hub.index('void settings(){')
assert 'void advancedSettings(){' in hub and 'void backupMore(){' in hub
base=hub[hub.index('void basicBackup(){'):hub.index('void backupMore(){')]
assert base.count('a.action(')==3 and '전체 ZIP 백업' in base and '전체 ZIP 복원' in base
assert '홈 버튼 편집 · 순서' not in home and 'new GarageHomeUi(a).editHomeShortcuts()' in hub
assert 'recoverInterrupted(File files)' in ride and 'RideTrash.recoverInterrupted(getFilesDir())' in main
assert 'RideCompletionInbox.record(a,restored)' in store
assert 'void showPage(MainActivity a,' in store and 'GPS 날짜 검색' in store
assert '30일 복구함' not in store
assert 'BackupFormat.isTrashAsset(name)' in rest
assert 'List<File> retired=trashTracks(internal)' in export
assert 'rides-trash/' in archive and 'TARGET_BYTES=175_000_000L' in archive
assert 'BatchArchiveExport.write(part,snapshot,id,output)' in main
assert 'verified.entryNames.equals(part.expectedEntries())' in main
assert 'DevicePreferencesBackup.restore(this,ready.preferences)' in main
assert 'MOTO-HUB-preferences.json' in export and 'MOTO-HUB-preferences.json' in rest
assert 'motoReleaseCode=$CODE' in Path("../.github/workflows/moto-ux-validation.yml").read_text()
print("PASS UX73 simplified settings, backup data retention, batch validation and GPS recovery guards")
PY
