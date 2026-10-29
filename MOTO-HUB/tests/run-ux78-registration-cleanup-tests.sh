#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
main=Path("app/src/main/java/com/motohub/next/MainActivity.java").read_text()
assert 'if(key.equals("model")&&form.equals("listings"))' in main
assert 'if(key.equals("name")&&form.equals("vehicles")||' not in main
assert 'if(key.equals("year")&&!form.equals("vehicles"))' in main
assert 'if(key.equals("vehicles"))label(p,"선택하면 이름·연식이 자동 입력됩니다.' in main
assert 'void applyCatalogYear(' in main
assert 'if(name!=null)name.setText(show);if(y!=null)y.setText(String.valueOf(year));' in main
assert 'field(essentials.contains(x[0])?p:optional,f,old,x[0],x[1],x[2]);' in main
assert '"바이크 이름 *"' in main and '"연식 *"' in main
assert 'showBikeCatalog(f,preview,status)' in main
print("PASS UX78 bike entry: no duplicate name/year chips, still auto/manual editable")
PY
