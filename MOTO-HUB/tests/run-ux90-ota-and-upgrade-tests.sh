#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
import importlib.util
import json
import time
from pathlib import Path
import tempfile
import zipfile

script=Path("tools/compare_full_backups.py")
spec=importlib.util.spec_from_file_location("moto_upgrade_check",script)
module=importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
code=Path("../.github/workflows/moto-publish-signed-on-version.yml").read_text()
assert "vars.MOTO_AUTO_PUBLISH != 'false'" in code, "Future signed releases must auto-attempt"
assert "Require fixed keystore secrets" in code
assert "gh release download" in code, "Interrupted publication must be resumable"
assert "resumed-signature.txt" in code and "test \"$CERT\"" in code
assert "grep -F \"versionCode='$CODE'\"" in code
assert 'gh release create' in code and 'git -C release-meta push origin HEAD:main' in code
assert "212c5b39d0082ab4bed78f8da2b719819cd80a20bc116c84f8bdfabca507021b" in code

def archive(path,*,photo=True,changed=False,add=False,part=False,draft_age=None):
    json_data={k:[] for k in module.COLLECTIONS}
    json_data["vehicles"]=[{"id":"bike","name":"Duke"}]
    json_data["expenses"]=[{"id":"fuel","amount":12000 if not changed else 500}]
    if add:json_data["services"]=[{"id":"service","type":"Oil"}]
    with zipfile.ZipFile(path,"w",compression=zipfile.ZIP_DEFLATED) as dest:
        dest.writestr("MOTO-HUB-data.json",json.dumps(json_data))
        if photo: dest.writestr("rides/1690000000000-12345678-1234-1234-1234-123456789abc.track","gps")
        dest.writestr("README-KO.txt","MOTO-HUB-PART:MEDIA\n" if part else "MOTO HUB 전체 백업\n")
        if draft_age is not None:
            dest.writestr("MOTO-HUB-preferences.json",json.dumps({
                "schema":1,"order":"ride,nav,fuel,service",
                "fuelDrafts":{"bike":{"savedAt":int(time.time()*1000-draft_age)}}}))

with tempfile.TemporaryDirectory() as folder:
    before=Path(folder)/"old.zip";after=Path(folder)/"new.zip"
    archive(before);archive(after)
    assert module.compare(before,after)["safe"]
    archive(after,add=True)
    assert module.compare(before,after)["safe"],"Adding a normal record is not data loss"
    archive(after,photo=False)
    assert not module.compare(before,after)["safe"],"Losing GPS must be detected"
    archive(after,changed=True)
    assert not module.compare(before,after)["safe"],"Changing expense records must be detected"
    archive(after,part=True)
    try:module.compare(before,after)
    except ValueError:pass
    else:raise AssertionError("Split ZIP was misclassified as a full backup")
    # Natural 7-day draft expiration must not trigger a false data-loss warning.
    archive(before,draft_age=9*24*60*60*1000);archive(after)
    assert module.compare(before,after)["safe"],"An expired unsent fuel form can disappear normally"
    archive(before,draft_age=24*60*60*1000);archive(after)
    assert not module.compare(before,after)["safe"],"Disappearing recent fuel form is real data loss"
    after.write_bytes(b"not a zip")
    try:module.compare(before,after)
    except zipfile.BadZipFile:pass
    else:raise AssertionError("Invalid ZIP accepted")
print("PASS UX90: automatic signed release, resumable manifest and offline upgrade-data audit")
PY
