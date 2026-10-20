#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PYTEST'
from pathlib import Path
import re
s=Path('app/src/main/java/com/motohub/next/MainActivity.java').read_text()
start=s.index(' void saveGaragePhoto(Uri uri,String bikeId)throws Exception{')
end=s.index(' void hideKeyboard()',start)
method=s[start:end]
checks={
 'garage photo uses atomic staging': 'AtomicFile photoDisk=new AtomicFile(file);' in method,
 'commit only after successful JPEG compression': method.index('if(!bitmap.compress(')<method.index('photoDisk.finishWrite(photoOut);'),
 'failed writes rollback': 'if(photoOut!=null)photoDisk.failWrite(photoOut);' in method,
 'bitmap recycled on all outcomes': 'finally{bitmap.recycle();}' in method,
 'old photo is never deleted before commit': '!file.delete()' not in method and 'file.delete()' not in method,
 'old photo is never replaced by a raw rename': 'renameTo(file)' not in method,
 'photo remains associated with requested bike': 'garagePhotoFile(bikeId)' in method and 'garagePhotoTarget=' not in method,
 'portrait EXIF preserved': 'ReceiptImageOrientation.rotate(bitmap,ReceiptImageOrientation.readDegrees(this,uri))' in method,
 'photo work remains off the UI thread': 'GARAGE_PHOTO_SAVER.execute(()->{try{saveGaragePhoto(uri,bikeId);}' in s and 'runOnUiThread(' in method,
 'photo saves are serialized': 'Executors.newSingleThreadExecutor' in s,
 'release version incremented': bool((match:=re.search(r"versionCode\s+(\d+);\s*versionName\s+'4\.0\.0-alpha6-[a-z]+'", Path('app/build.gradle').read_text())) and int(match.group(1))>=459),
}
for name,ok in checks.items():print(('PASS' if ok else 'FAIL')+': '+name)
if not all(checks.values()):raise SystemExit(1)
print(f'Garage photo atomic save static regression checks PASS ({len(checks)} checks)')
PYTEST
