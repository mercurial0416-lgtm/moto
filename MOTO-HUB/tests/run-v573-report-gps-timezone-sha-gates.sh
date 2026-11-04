#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
p=Path("app/src/main/java/com/motohub/next")
read=lambda n:(p/n).read_text(encoding="utf-8")
ride=read("RideUi.java")
stats=read("RideStatsUi.java")
timeline=read("MotoTimeline.java")
timeline_ui=read("MotoTimelineUi.java")
cache=read("RideDistanceCache.java")
types=read("RideTypeComparison.java")
summary=read("RideStatsSeries.java")
report=ride.split('void rideReport(String bikeScope,String scopeLabel){',1)[1].split('void handleReadiness(',1)[0]
checks={
 "60-day report no 150-ride cutoff": "if(inspected>=150)" not in report and "RideMonthlyReport.recent(track,now,60)" in report,
 "60-day report uses GPS read-only SHA cache": "RideDistanceCache.read(track)" in report and "categoryComparison.add(track,now,data)" in report,
 "original 60-day comparison retains moving time": "add(File file,long now,RideDistanceCache.Summary ride)" in types and "movingMillis" in types,
 "monthly GPS chart no 5MB exclusion": "ride.length()>5_000_000" not in stats and "ride.length()>TrackFile.MAX_BYTES" in stats,
 "monthly chart distinguishes exclusion reasons": all(x in stats for x in ("data.invalidCompleted","data.oversize","data.unreadable","data.invalidMetrics")),
 "monthly chart preserves cost division exclusion gate": "RideCostPerKm.caption(selected,sixMonthExpense,data.totalKm,data.excluded)" in stats and "data.scanned++" in stats,
 "time zone is not fixed to Korea for timeline": 'ZoneId.of("Asia/Seoul")' not in timeline and 'ZoneId.systemDefault()' in timeline,
 "timeline snapshots zone once per screen": "localZone=java.time.ZoneId.systemDefault()" in timeline_ui and "MotoTimeline.ledger(snapshot,bikeId,localZone)" in timeline_ui and "MotoTimeline.ride(track,snapshot,bikeId,localZone)" in timeline_ui,
 "cache SHA-256 prevents matching-size matching-mtime rewrite": "RideMileageIntegrity.fingerprint(track)" in cache and "cached.sha256.equals(currentHash)" in cache and "!parsed.equals(after)" in cache,
 "cached summary contains no identifying coordinates": "peakLatitude" not in cache and "peakLongitude" not in cache,
 "cache still bounded and read-only": "MAX_ENTRIES=4096" in cache and "removeEldestEntry" in cache and "NEVER evidence for odometer" in cache,
 "stats and other safe 25MB limit unchanged": "TrackFile.MAX_BYTES" in cache and "invalidCompleted=0,oversize=0,unreadable=0,invalidMetrics=0" in summary,
}
for label,ok in checks.items(): print(("PASS" if ok else "FAIL")+" : "+label)
assert all(checks.values()),"v573 report/GPS/timezone/cache regression failure"
PY
