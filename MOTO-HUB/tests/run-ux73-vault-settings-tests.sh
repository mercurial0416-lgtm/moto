#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
p=Path("app/src/main/java/com/motohub/next")
r=lambda name:(p/name).read_text()
archive=r("BackupBundle.java")
restorer=r("RestoreArchive.java")
format=r("BackupFormat.java")
trash=r("RideTrash.java")
activity=r("MainActivity.java")
hub=r("HubScreens.java")
settings=hub[hub.index("void settings(){"):hub.index("void advancedSettings(){")]
assert "recoverInterrupted(File files)" in trash
assert 'RideTrash.recoverInterrupted(getFilesDir())' in activity
assert 'RideTrash.entries(internal)' in archive
assert 'isTrashTrack' in format and 'isTrashSidecar' in format
assert 'String prefix="rides-trash/"+name+"/";' in archive
assert 'public final String preferences' in restorer
assert 'MOTO-HUB-preferences.json' in archive and 'MOTO-HUB-preferences.json' in format
assert "UiSettingsArchive.restore(this,ready.preferences)" in activity
assert "void importUiSettingsFromZip()" in activity
assert 'UiSettingsArchive.check(preferences)' in archive
assert "BackupVolumeExporter.export(this,folder" in activity
assert "MOTO-HUB-VOLUMES-v1" in r("BackupVolumeExporter.java")
assert "readback.entryNames.equals(expected)" in r("BackupVolumeExporter.java")
assert "showPage(MainActivity a,boolean deleted,String search,int offset)" in r("RideStorageManager.java")
assert "RideCompletionInbox.restored(a,restored)" in r("RideStorageManager.java")
assert "30일 복구함" not in r("RideStorageManager.java")
assert "고급 설정" in settings and "this::advancedSettings" in settings
assert 'MotoWidgetPin.request' not in settings
assert 'SigningIdentity.current(a)' not in settings
assert 'this::selfCheck' not in settings
assert "void advancedSettings()" in hub
assert 'motoReleaseCode=543' in Path("../.github/workflows/moto-ux-validation.yml").read_text()
print("PASS UX73 vault ZIP and interrupted moves, multi-volume readback, compact settings, preferences and paged history")
PY
