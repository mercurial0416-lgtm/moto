#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"; trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" app/src/main/java/com/motohub/next/OptionalFieldDisclosure.java tests/OptionalFieldDisclosureTest.java
java -cp "$out" com.motohub.next.OptionalFieldDisclosureTest
python3 - <<'PY'
from pathlib import Path
src=Path('app/src/main/java/com/motohub/next/MainActivity.java').read_text()
for token in ['boolean revealSaved=existing!=null&&hasPopulatedOptionalFields(f)', 'formOptionalKeys.add(x[0])', 'revealOptionalDraft();formDirty=true;persistDraft()', 'formOptionalSection.setVisibility(View.VISIBLE);updateOptionalButton();', 'formOptionalToggle.setContentDescription']:
 assert token in src, token
print('PASS: BY optional UI wiring 5 checks')
PY
