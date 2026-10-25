#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
s=Path("app/src/main/java/com/motohub/next/MainActivity.java").read_text()
assert '{"name","바이크 이름 *",""}' in s, "New bike name must start empty"
assert '{"year","연식 *",""}' in s, "New bike/listing year must start empty"
assert 'bike()==null?"슈퍼커브 110"' not in s, "Hard-coded Super Cub example must be removed"
assert 'bike()==null?"2024"' not in s, "Hard-coded year example must be removed"
assert '{"year","연식","2024"}' not in s, "Listing year must not be a mistaken actual value"
assert 'old.has(key)?old.optString(key):def' in s, "Existing bike fields must still show their saved values"
assert 'if(name!=null)name.setText(show);if(y!=null)y.setText(String.valueOf(year));' in s, "Real model/year picker must still fill selected values"
assert 'if(k.equals("year")&&(n<1900||n>LocalDate.now().getYear()+2||n%1!=0))' in s, "Year validation must remain active"
assert 'key.equals("year")?"연식 4자리 입력 또는 모델 선택"' in s, "Year input guidance must be clearly distinguished from saved values"
print("PASS new-bike/listing forms have no fabricated name/year prefill; saved data and picker preserved")
PY
