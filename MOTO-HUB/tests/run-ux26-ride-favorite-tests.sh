#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
DIR="$(mktemp -d)";trap 'rm -rf "$DIR"' EXIT
javac -encoding UTF-8 -d "$DIR" \
 app/src/main/java/com/motohub/next/{ReceiptSlots,BackupFormat,TrackFile,RideFavorite,BackupBundle,BackupIntegrity,RestoreArchive,RestoreCapacity}.java \
 tests/RideFavoriteTest.java
java -cp "$DIR" com.motohub.next.RideFavoriteTest
python3 - <<'PY'
from pathlib import Path
s=Path('app/src/main/java/com/motohub/next/RideUi.java').read_text()
assert 'RideFavorite.toggle(f)' in s and 'RideFavorite.isFavorite(f)' in s
assert 'live||interrupted||a.readOnly?null:()->{' in s
hub=Path('app/src/main/java/com/motohub/next/HubScreens.java').read_text()
assert 'Button star=a.button(starred?"★":"☆",false,favorite)' in hub
assert '라이딩 즐겨찾기 해제' in hub and '라이딩 즐겨찾기' in hub
print('PASS ride favorite UI wiring')
PY
