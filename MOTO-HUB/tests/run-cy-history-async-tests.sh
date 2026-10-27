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
 app/src/main/java/com/motohub/next/RideHistorySort.java \
 app/src/main/java/com/motohub/next/RideHistoryPeriod.java \
 tests/RideHistoryAsyncDataTest.java
java -cp "$out" com.motohub.next.RideHistoryAsyncDataTest
python3 - <<'PY'
from pathlib import Path
src=Path('app/src/main/java/com/motohub/next/RideUi.java').read_text()
render=src.split(' void render(){',1)[1].split(' void loadHistoryAsync(',1)[0]
worker=src.split(' void loadHistoryAsync(',1)[1].split(' void populateHistory(',1)[0]
ui=src.split(' void populateHistory(',1)[1].split(' void chooseMerge(',1)[0]
assert 'HISTORY_EXECUTOR.execute(()->{' in worker
assert 'TrackFile.tracks(files)' in worker and 'RideHistorySort.order(' in worker
assert 'TrackFile.tracks(' not in render and 'RideHistoryBikeFilter.select(' not in render
assert 'historyPanel.getParent()!=requestedContent' in worker
assert 'generation!=HISTORY_GENERATION.get()' in worker
assert 'a.content!=requestedContent' in worker
assert 'a.runOnUiThread(' in worker and 'populateHistory(historyPanel,data)' in worker
assert 'loadHistoryAsync(historyPanel,a.content)' in render
assert '목록 다시 불러오기' in worker
assert 'search.addTextChangedListener(' in ui and 'historyPanel.addView(results)' in ui
merge=src.split(' void chooseMerge(){',1)[1].split(' void showMergeChoice(',1)[0]
assert 'HISTORY_EXECUTOR.execute(()->{' in merge and 'TrackFile.tracks(a.getFilesDir())' in merge
assert 'request!=MERGE_GENERATION.get()' in merge and 'showMergeChoice(candidates,names)' in merge
assert '!hero.isAttachedToWindow()' in src and 'OBD2 데이터를 읽는 중' not in src
assert 'if(results.isAttachedToWindow())' in ui
assert 'Sharing.start(' not in src
print('PASS: 16 async UI wiring, stale-screen guard, retry and opt-in sharing checks')
PY
