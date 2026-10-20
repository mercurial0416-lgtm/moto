#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" \
 app/src/main/java/com/motohub/next/LocalBackupFilePolicy.java \
 tests/Ux11BackupRetentionTest.java
java -cp "$out" com.motohub.next.Ux11BackupRetentionTest
python3 - <<'PY'
from pathlib import Path
p=Path('app/src/main/java/com/motohub/next/LocalSafetyBackup.java').read_text()
assert 'File[] points=LocalBackupFilePolicy.eligible(folder,' in p
assert 'File[] snapshots=LocalBackupFilePolicy.eligible(folder,' in p
assert 'for(int i=0;i<points.length-12;i++)points[i].delete();' in p
assert 'for(int i=0;i<snapshots.length-7;i++)snapshots[i].delete();' in p
assert 'read(file);' in p and 'read(destination);' in p, 'only rotate after newly written backup validates'
print('PASS UX11 rotation is restricted to non-symlink, size-bounded backup candidates')
PY
