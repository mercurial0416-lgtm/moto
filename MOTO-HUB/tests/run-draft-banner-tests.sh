#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" app/src/main/java/com/motohub/next/DraftBanner.java tests/DraftBannerTest.java
java -cp "$out" com.motohub.next.DraftBannerTest
python3 - <<'PY2'
from pathlib import Path
s=Path('app/src/main/java/com/motohub/next/MainActivity.java').read_text(encoding='utf8')
assert 'DraftBanner.label(draftType)' in s
assert 'this::resumeDraft' in s
assert 'this::confirmDiscardDraft' in s
assert 'setNegativeButton("취소",null)' in s
method=s[s.index(' void confirmDiscardDraft(){'):s.index(' void persistDraft(){')]
assert 'remove("active")' in method
assert 'write(next)' not in method and 'Store.remove(' not in method
assert 'render();' in method
print('PASS: draft UI, confirmation and draft-only deletion wiring')
PY2
