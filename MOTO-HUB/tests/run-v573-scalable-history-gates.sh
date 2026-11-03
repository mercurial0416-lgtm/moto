#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
src=Path("app/src/main/java/com/motohub/next")
read=lambda f:(src/f).read_text(encoding="utf-8")
timeline=read("MotoTimelineUi.java")
ride=read("RideUi.java")
stats=read("RideStatsUi.java")
cache=read("RideDistanceCache.java")
hub=read("HubScreens.java")
main=read("MainActivity.java")
cost=hub.split("void costDashboard(){",1)[1].split("void settings(){",1)[0]
distance=ride.split('String distanceInfo="";',1)[1].split('List<HistoryEntry> entries=',1)[0]
checks={
 "timeline no 200-track cutoff": "if(inspected++>=200)break;" not in timeline and "for(File track:RideHistorySort.order" in timeline,
 "timeline incremental pages": "MotoTimeline.filtered(events,selected[0],visibleLimit[0])" in timeline and "활동 더 보기" in timeline and "visibleLimit[0]=40" in timeline,
 "timeline old damaged GPS warning retained": "invalid.setText(exclusions>0" in timeline,
 "full-distance search uses cache not 150/80MB gate": "RideDistanceCache.read(file)" in distance and "withinBudget" not in distance and "visible=matched" in distance,
 "distance search generation cancellation": "if(generation!=HISTORY_GENERATION.get())return;" in distance,
 "shared in-memory capped LRU": "MAX_ENTRIES=4096" in cache and "removeEldestEntry" in cache,
 "cache read-only no odometer": "NEVER evidence for odometer" in cache and "RideMileageIntegrity.validCompleted(track)" in cache,
 "cache detects changed file": "track.lastModified()!=modified" in cache and "track.length()!=size" in cache,
 "six-month chart uses same cached summary": "RideDistanceCache.read(ride)" in stats and "ride.length()>5_000_000" in stats,
 "full restore invalidates derived distance": "RideDistanceCache.clear();" in main,
 "cost dashboard uses local snapshot day": "ZoneId.systemDefault()" in cost and "YearMonth.from(today)" in cost and "RecordMonthlySummary.forBikeAsOf" in cost and "localToday=today.toString()" in cost,
 "original data scheme unchanged": "atomic(disk,next.toString(2)" in main and "Sharing.start(" not in timeline
}
for name,ok in checks.items():print(("PASS " if ok else "FAIL ")+name)
assert all(checks.values()),"scalable history/cost integrity regression"
PY
