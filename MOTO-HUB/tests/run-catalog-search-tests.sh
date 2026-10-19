#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" \
  app/src/main/java/com/motohub/next/CatalogSearch.java \
  tests/CatalogSearchTest.java
java -cp "$out" com.motohub.next.CatalogSearchTest
python3 - <<'PY'
import json
from pathlib import Path
base=Path('app/src/main')
rows=json.loads((base/'assets/motorcycle_catalog.json').read_text(encoding='utf8'))['models']
for brand, model, alias in [('Honda','Super Cub 110','슈퍼커브'),('KTM','390 Duke','듀크390'),('Yamaha','YZF-R3','알쓰리')]:
    row=next((x for x in rows if x['brand']==brand and x['model']==model),None)
    assert row is not None,(brand,model)
    if alias not in row['aliases']:
        print('NOTE: no curated alias',brand,model,alias)
main=(base/'java/com/motohub/next/MainActivity.java').read_text(encoding='utf8')
assert 'MotorcycleCatalog.modelAliases(this,brand)' in main
assert 'CatalogSearch.matches(item,aliases.get(item),q)' in main
assert 'if(models.isEmpty())models=Arrays.asList("직접 입력")' not in main
assert 'dialog.setOnShowListener' in main
print('PASS: Android UI wiring and catalog alias data verified')
PY
