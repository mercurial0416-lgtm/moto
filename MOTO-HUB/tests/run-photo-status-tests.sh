#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" \
  app/src/main/java/com/motohub/next/PhotoSearchStatus.java \
  tests/PhotoSearchStatusTest.java
java -cp "$out" com.motohub.next.PhotoSearchStatusTest
python3 - <<'PY2'
from pathlib import Path
p=Path('app/src/main/java/com/motohub/next/MotorcyclePhoto.java').read_text()
checks={
 'lookup reports classified failures': 'candidateWithStatus(brand,model,year)' in p,
 'network error kept for UI': 'trace.searchFailed(error)' in p,
 'image download network failure kept for UI': 'trace.downloadFailed(downloadError)' in p,
 'missing photos identified': 'trace.searchSucceeded(candidates.size())' in p,
 'download failures identified': 'trace.imageFailed()' in p,
 'retry is clickable for catalog': 'credit.setOnClickListener(v->show(a,target,credit,brand,model,year,null));' in p,
 'retry and manual choice available in garage': 'new String[]{"사진 다시 검색","내 사진 등록"}' in p,
 'old requests invalidated on retry': 'PhotoRequestGuard.begin(target)' in p,
 'catalog manual photo guard retained': 'final boolean garageMode=PhotoViewPolicy.hasGarageOwner(garageBikeId);' in p,
}
for name,ok in checks.items():print(('PASS' if ok else 'FAIL')+': '+name)
assert all(checks.values())
print('PASS: photo failure UI static checks',len(checks))
PY2
