#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
dir="$(mktemp -d)"
trap 'rm -rf "$dir"' EXIT
javac -encoding UTF-8 -d "$dir" \
 app/src/main/java/com/motohub/next/PhotoLookupQueue.java tests/PhotoLookupQueueTest.java
java -cp "$dir" com.motohub.next.PhotoLookupQueueTest
python3 - <<'PY'
from pathlib import Path
s=Path('app/src/main/java/com/motohub/next/MotorcyclePhoto.java').read_text()
assert 'PhotoLookupQueue.submit(()->{' in s
assert 'if(!PhotoRequestGuard.isCurrent(target,requestToken))return;' in s
assert 'if(!accepted&&PhotoRequestGuard.isCurrent(target,requestToken))' in s
assert 'if(selected){' in s and 'manualOverride=current;' in s
assert 'if(selectedImage!=null){' in s
assert 'if(image!=null&&!image.isRecycled())image.recycle();' in s
assert '사진 요청이 많아요 · 탭해서 다시 검색' in s
print('PASS photo async cache/selection/overload wiring')
PY
