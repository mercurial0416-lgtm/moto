#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
javac -encoding UTF-8 -d "$OUT" app/src/main/java/com/motohub/next/TrackFile.java tests/Ux7TrackSafetyTest.java
java -cp "$OUT" com.motohub.next.Ux7TrackSafetyTest
bash tests/run-garage-photo-atomic-tests.sh
python3 - <<'PY'
from pathlib import Path
main=Path("app/src/main/java/com/motohub/next/MainActivity.java").read_text()
ocr=Path("app/src/main/java/com/motohub/next/ReceiptOcr.java").read_text()
assert 'if(request==52)garagePhotoTarget="";' in main
assert 'final String bikeId=garagePhotoTarget;garagePhotoTarget=""' in main
assert 'GARAGE_PHOTO_SAVER.execute(()->{try{saveGaragePhoto(uri,bikeId);}' in main
assert 'ReceiptImageOrientation.rotate(bitmap,ReceiptImageOrientation.readDegrees(this,uri))' in main
assert 'callback.failure("화면이 닫혀 영수증 인식이 취소됐습니다.")' in ocr
assert 'if(cleanup!=null)cleanup.run()' in main
print("PASS UX7 photo cancellation, EXIF, serialized save and OCR temp cleanup wiring")
PY
