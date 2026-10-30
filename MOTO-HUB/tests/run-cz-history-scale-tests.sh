#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" \
 app/src/main/java/com/motohub/next/TrackFile.java \
 app/src/main/java/com/motohub/next/RideBikeBinding.java \
 app/src/main/java/com/motohub/next/RideHistoryBikeFilter.java \
 app/src/main/java/com/motohub/next/RideHistoryLabel.java \
 app/src/main/java/com/motohub/next/RideHistoryPeriod.java \
 app/src/main/java/com/motohub/next/RideHistorySort.java \
 app/src/main/java/com/motohub/next/RideHistoryFilter.java \
 app/src/main/java/com/motohub/next/RideHistorySearch.java \
 tests/RideHistoryScopeCountsTest.java
java -cp "$out" com.motohub.next.RideHistoryScopeCountsTest
python3 - <<'PY'
from pathlib import Path
src=Path('app/src/main/java/com/motohub/next/RideUi.java').read_text()
assert src.index('bikeRides.removeIf(file->!RideHistoryPeriod.include(file,now,period))') < src.index('RideHistoryFilter.counts(bikeRides,activeName,recording)')
assert 'final String[] terms=RideHistorySearch.terms(a.rideHistoryQuery)' in src
assert 'RideHistorySearch.matchesIndex(entry.searchKey,terms)' in src
assert 'this.searchKey=RideHistorySearch.index(date+" "+memo,status)' in src
assert 'String savedMemo=RideNotes.memo(stableDb,file)' in src
assert 'rideNotesSnapshot=liveNotes==null?' in src
assert 'org.json.JSONObject stableNotes=new org.json.JSONObject(rideNotesSnapshot)' in src
assert 'RideNotes.memo(a.db,file)' not in src
assert 'Sharing.start(' not in src
print('PASS: date-scoped counts, indexed multi-term UI search and local-only safety checks')
PY
