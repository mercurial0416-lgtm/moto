#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
classes="$(mktemp -d)"
trap 'rm -rf "$classes"' EXIT
javac -encoding UTF-8 -d "$classes" \
  app/src/main/java/com/motohub/next/RideHistorySessionRows.java \
  app/src/main/java/com/motohub/next/RideHistorySessionDistance.java \
  tests/RideHistorySessionRowsTest.java tests/RideHistorySessionDistanceTest.java
java -cp "$classes" com.motohub.next.RideHistorySessionRowsTest
java -cp "$classes" com.motohub.next.RideHistorySessionDistanceTest
python3 - <<'PY'
from pathlib import Path
ui=Path("app/src/main/java/com/motohub/next/RideUi.java").read_text()
assert 'RideHistorySessionRows.group(matched,entry->entry.groupKey)' in ui
assert 'RideHistorySessionDistance.select(entries,entry->entry.groupKey' in ui
assert 'RideHistoryDistanceFilter.accepts(distanceRange,km)' in ui
assert 'RideBikeBinding.readStrict(file)' in ui
assert 'RideMileageIntegrity.validCompleted(file)' in ui
assert 'RideHistoryFilter.COMPLETE.equals(status)' in ui
assert 'new File(file.getPath()+".imported").exists()' in ui
assert 'Set<String> unsafeSessions=new HashSet<>()' in ui
assert 'unsafeSessions.add(session)' in ui
assert 'if(!session.isEmpty()&&!unsafeSessions.contains(session))' in ui
assert 'groupKey=session+"|"+bikeBinding+"|"+category' in ui
assert 'if(expanded)for(HistoryEntry segment:row.segments)' in ui
assert 'showHistoryEntry(results,segment,entries,total,selected,summary)' in ui
assert 'RideHistoryLabel.filenameTime(part.file.getName())' in ui
assert 'earliest.date+" · 라이딩 1회"' in ui, 'ride header must show the original session start time'
assert 'recoverInterrupted(f)' in ui and 'RideFavorite.toggle(f)' in ui
print('PASS UI: display-only ride groups retain per-file recovery and favorite actions')
PY
