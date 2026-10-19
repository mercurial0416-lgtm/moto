#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" \
  app/src/main/java/com/motohub/next/CatalogYearChoices.java \
  tests/CatalogYearChoicesTest.java
java -cp "$out" com.motohub.next.CatalogYearChoicesTest
python3 - <<'PY2'
from pathlib import Path
s=Path('app/src/main/java/com/motohub/next/MainActivity.java').read_text(encoding='utf8')
assert 'CatalogYearChoices.labels(now)' in s
assert 'CatalogYearChoices.selectedIndex(selectedYear,now)' in s
assert 'CatalogYearChoices.yearAt(pos,now)' in s
assert 'setNeutralButton("연식 직접 입력",null)' in s
assert 'CatalogYearChoices.valid(year,now)' in s
assert 'applyCatalogYear(brand,model,year,fields,photo,status)' in s
print('PASS: Android catalog year picker wiring checked')
PY2
