#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
DIR="$(mktemp -d)";trap 'rm -rf "$DIR"' EXIT
javac -encoding UTF-8 -d "$DIR" \
 app/src/main/java/com/motohub/next/OdometerOcrCandidates.java \
 tests/OdometerOcrCandidatesTest.java
java -cp "$DIR" com.motohub.next.OdometerOcrCandidatesTest
python3 - <<'PY'
from pathlib import Path
a=Path('app/src/main/java/com/motohub/next/MainActivity.java').read_text()
q=Path('app/src/main/java/com/motohub/next/QuickEntryUi.java').read_text()
assert '갤러리에서 계기판 사진 선택' in q and 'chooseQuickOdometerPhoto(odo)' in q
assert '계기판 바로 촬영' in q and 'startQuickOdometerCamera(odo)' in q
assert 'ReceiptOcr.scan(this,uri' in a and 'OdometerOcrCandidates.fromText(recognized)' in a
assert 'if(request==57){scanQuickOdometerPhoto(uri);return;}' in a
assert 'target.setText(candidate)' in a and 'setSingleChoiceItems' in a
print('PASS odometer OCR uses offline recognition and user-confirmed entry')
PY
