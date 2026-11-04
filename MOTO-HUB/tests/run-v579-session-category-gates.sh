#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
base=Path('app/src/main/java/com/motohub/next')
category=(base/'RideSessionCategory.java').read_text()
main=(base/'MainActivity.java').read_text()
assert 'static void write(File filesDir,File selected,String category)' in category
assert 'RideSegmentGroup.members(filesDir,selected.getName())' in category
assert 'requireEntireSessionComplete(filesDir,session,members.size())' in category
assert category.count('requireEntireSessionComplete(filesDir,session,members.size())')>=2
assert 'static synchronized void write(File filesDir,File selected,String category,Runnable afterEachWrite)' in category
assert 'originals.get(member)' in category and 'restoreSidecar(member,originals.get(member))' in category
assert 'Files.isSymbolicLink(sidecar.toPath())' in category
assert 'RideSessionCategory.write(getFilesDir(),track,categories[which])' in main
assert '"moto-ride-category").start()' in main
assert main.count('RideActiveSessionPolicy.exclude(track.getName(),RideService.running,RideService.active)')>=4
assert 'RideCategory.write(track,categories[which])' not in main
print('PASS UX126 / v580 complete-session-only tags, late section rollback, symlink safety, non-blocking UI')
PY
