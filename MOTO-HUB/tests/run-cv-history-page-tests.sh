#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" app/src/main/java/com/motohub/next/RideHistoryPage.java tests/RideHistoryPageTest.java
java -cp "$out" com.motohub.next.RideHistoryPageTest
python3 - <<'PYTEST'
from pathlib import Path
ui=Path('app/src/main/java/com/motohub/next/RideUi.java').read_text()
assert 'if(page.size()<historyVisibleLimit)page.add(entry);' in ui and 'final int shown=page.size();' in ui, 'visible results remain capped while doing one scan'
assert 'historyVisibleLimit=RideHistoryPage.PAGE_SIZE;' in ui
assert 'RideHistoryPage.nextLimit(historyVisibleLimit)' in ui
assert '라이딩 더 보기 · 남은 ' in ui
imp=Path('app/src/main/java/com/motohub/next/GpxImporter.java').read_text()
assert 'createdDone=done.createNewFile()' in imp and 'createdImported=imported.createNewFile()' in imp
assert 'Files.deleteIfExists(dest.toPath())' in imp
assert 'catch(IOException ignored)' not in imp
print('PASS: paginated UI and GPX import rollback wiring')
PYTEST
