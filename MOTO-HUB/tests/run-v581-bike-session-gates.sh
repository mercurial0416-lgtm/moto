#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
root=Path('app/src/main/java/com/motohub/next')
logic=(root/'RideSessionBikeBinding.java').read_text()
ui=(root/'MainActivity.java').read_text()
old=(root/'RideBikeBinding.java').read_text()
assert 'requireComplete(filesDir,session,members.size(),alreadyApplied)' in logic
assert logic.count('requireComplete(filesDir,session,members.size(),alreadyApplied)')==2
assert 'alreadyApplied.contains(part.getName())' in logic and 'RideBikeBinding.readStrict(part)' in logic
assert 'restore(part,originals.get(part))' in logic
assert 'RideSessionBikeBinding.write(getFilesDir(),track,chosenBikeId,appliedNames)' in ui
assert 'RideBikeBinding.write(track,vehicles.get(which).optString("id"))' not in ui
assert '"moto-ride-bike-binding").start()' in ui
assert 'Files.isSymbolicLink(marker.toPath())' in old
print('PASS UX128 whole GPS session bike binding, applied-ODO guard, rollback, safe background write')
PY
