#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" \
 app/src/main/java/com/motohub/next/{LocalBackupFilePolicy,LocalBackupValidationCache}.java \
 tests/Ux13BackupCacheTest.java
java -cp "$out" com.motohub.next.Ux13BackupCacheTest
python3 - <<'PY'
from pathlib import Path
base=Path("app/src/main/java/com/motohub/next")
back=(base/"LocalSafetyBackup.java").read_text()
cache=(base/"LocalBackupValidationCache.java").read_text()
policy=(base/"LocalBackupFilePolicy.java").read_text()
ui=(base/"HubScreens.java").read_text()
assert "LocalBackupValidationCache.verify(file,()->{" in back
assert "try{read(file);return true;}" in back
assert "byte[] bytes=LocalBackupFilePolicy.readBounded(file);" in back, "Chosen restore never uses the preview cache"
assert back.count("LocalBackupFilePolicy.readBounded(")>=3, "All post-write readbacks bounded"
assert "MAX_AGE_NANOS=15_000_000_000L" in cache and "MAX_ENTRIES=64" in cache
assert "if(total>MAX_BYTES)" in policy and "requireReadable(file);" in policy
assert "LocalSafetyBackup.read(selected)" in ui, "Restore still validates chosen snapshot"
assert "LOCAL_BACKUP_IO.execute(()->{" in ui, "Picker validation stays off UI looper"
print("PASS UX13 chosen restore read bypasses picker cache and remains bounded")
PY
