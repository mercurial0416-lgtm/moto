#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
# Static regression checks for the Android UI path when no Android SDK is available.
python3 - <<'PY'
from pathlib import Path
s=Path('app/src/main/java/com/motohub/next/MotorcyclePhoto.java').read_text()
checks={
 'new photo request invalidates old request': 'PhotoRequestGuard.begin(target)' in s,
 'preview resets previous bike image': 'target.setImageResource(BikeVisual.superCub(model)?R.drawable.garage_super_cub:R.drawable.garage_model_placeholder);' in s,
 'cached image decoded separately from metadata': 'android.graphics.Bitmap cached=decodeLocalSampled(file,1200);' in s and 'PhotoDecodeScale.sampleSize(' in s and 'PHOTO_DISK.execute' in s,
 'metadata parsing has its own fallback': 'if(attribution.isFile())try{meta=new JSONObject(readSmall(attribution));}catch(Exception ignored){}' in s,
 'valid cached photo assigned from guarded UI callback': s.index('if(attribution.isFile())try{meta=') < s.index('target.setImageBitmap(loaded);') < s.index('boolean accepted=PhotoLookupQueue.submit'),
 'offline cache returns on successful background submit': 'PHOTO_DISK.execute(()->{' in s and 'showInternal(a,target,credit,brand,model,year,garageBikeId,skipInvalidLocal,true)' in s and s.index('PHOTO_DISK.execute(()->{') < s.index('boolean accepted=PhotoLookupQueue.submit'),
 'catalog cannot change garage photo': 'final boolean garageMode=PhotoViewPolicy.hasGarageOwner(garageBikeId);' in s,
 'photo cache fix version not regressed': __import__("re").search(r"versionCode\s+(45[8-9]|4[6-9][0-9]|[5-9][0-9]{2});\s*versionName\s+\'4\.0\.0-alpha6-[a-z]+\'", Path("app/build.gradle").read_text()) is not None,
}
for name,ok in checks.items():
 print(('PASS' if ok else 'FAIL')+': '+name)
if not all(checks.values()):raise SystemExit(1)
print(f'Photo cache UI source checks PASS ({len(checks)} checks)')
PY
