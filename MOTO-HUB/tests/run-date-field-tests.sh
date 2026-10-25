#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" app/src/main/java/com/motohub/next/DateFieldChoices.java tests/DateFieldChoicesTest.java
java -cp "$out" com.motohub.next.DateFieldChoicesTest
python3 - <<'PY2'
from pathlib import Path
src=Path('app/src/main/java/com/motohub/next/MainActivity.java').read_text(encoding='utf-8')
assert 'if(DateFieldChoices.isDateField(key))' in src
assert 'boolean optional=DateFieldChoices.canClear(form,key);' in src
assert '"선택 안 함"' in src
assert 'if("@clear".equals(value)){target.setText("");return;}' in src
assert '"@clear".equals(values[i]) && selected.isEmpty()' in src
assert 'if((key.equals("expenses")||key.equals("services"))&&item.optString("date").isEmpty())' in src
print('PASS: Android date field UI integration checked')
PY2
