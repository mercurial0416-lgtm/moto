#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
src=Path("app/src/main/java/com/motohub/next")
bundle=(src/"BackupBundle.java").read_text()
preflight=(src/"BackupReadiness.java").read_text()
assert "File[] found=folder.listFiles(selector);" in bundle
assert "if(found==null)throw new IOException" in bundle
assert "if(folders==null)throw new IOException" in bundle
assert "File[] tracks=gps?listIfPresent(rides," in bundle
assert "File[] selectedModels=media?listIfPresent(modelFolder," in bundle
assert "total+=readmeBytes.length;" in bundle
assert "File[] found=folder.listFiles(filter);" in preflight
assert "if(found==null)throw new IOException" in preflight
for suffix in (".done",".imported",".recovered",".unmerged",
               ".mergedinto",".bike",".category",".favorite"):
    assert '"'+suffix+'"' in preflight, suffix
assert (Path("app/src/test/java/com/motohub/next")/
        "UX99BackupEnumerationTest.java").is_file()
print("UX99 backup source safety checks PASS")
PY
