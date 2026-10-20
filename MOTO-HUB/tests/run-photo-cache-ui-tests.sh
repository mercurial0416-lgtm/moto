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
 'cached image decoded separately from metadata': 'try{cached=decodeLocalSampled(file,1200);}catch(Exception ignored){}' in s and 'PhotoDecodeScale.sampleSize(' in s,
 'metadata parsing has its own fallback': 'if(attribution.isFile())try{meta=new JSONObject(readSmall(attribution));}catch(Exception ignored){}' in s,
 'valid cached photo always assigned after metadata fallback': s.index('if(attribution.isFile())try{meta=') < s.index('target.setImageBitmap(cached);') < s.index('credit.setText(garageMode?"사진 검색 중'),
 'offline cache exits before online search': s.index('target.setImageBitmap(cached);') < s.index('    return;\n   }\n  }\n  credit.setText(garageMode?"사진 검색 중'),
 'catalog cannot change garage photo': 'final boolean garageMode=PhotoViewPolicy.hasGarageOwner(garageBikeId);' in s,
 'photo cache fix version not regressed': __import__("re").search(r"versionCode\s+(45[8-9]|4[6-9][0-9]|[5-9][0-9]{2});\s*versionName\s+\'4\.0\.0-alpha6-[a-z]+\'", Path("app/build.gradle").read_text()) is not None,
}
for name,ok in checks.items():
 print(('PASS' if ok else 'FAIL')+': '+name)
if not all(checks.values()):raise SystemExit(1)
print(f'Photo cache UI source checks PASS ({len(checks)} checks)')
PY
