#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
p=Path("app/src/main/java/com/motohub/next")
load=lambda file:(p/file).read_text()
ride=load("RideUi.java")
timeline=load("MotoTimelineUi.java")
gpx=load("GpxImporter.java")
track=load("TrackFile.java")
insights=load("RideInsights.java")
cache=load("RideDistanceCache.java")
report=ride.split("void rideReport(String bikeScope,String scopeLabel){",1)[1].split("void handleReadiness(",1)[0]
checks={
 "one serial 60-day worker and newest-only result": "REPORT_EXECUTOR.execute(" in report and "REPORT_GENERATION.incrementAndGet()" in report and "request!=REPORT_GENERATION.get()" in report and '"라이딩".equals(a.tab)' in report,
 "progressive partial activity results": "events.addAll(initial)" in timeline and "GPS 기록 계속 불러오는 중" in timeline and "inspected==64||inspected%1024==0" in timeline and "loading[0]=false" in timeline,
 "complete timeline never silently capped": "if(inspected++>=200)break;" not in timeline and "RideSegmentGroup.collapse(merged)" in timeline,
 "larger GPX safe import": "MAX_INPUT_BYTES=TrackFile.MAX_BYTES" in gpx and "MAX_IMPORT_POINTS=200_000" in gpx and "written[0]" in gpx,
 "GPX external entities remain disabled": 'disallow-doctype-decl' in gpx and 'external-general-entities' in gpx,
 "digest generated from parsed bytes": "new DigestInputStream" in track and "RideInsights.analyze(track,parsedDigest)" in cache and "},digest);" in insights,
 "cache hit remains strongly hash-verified": "cached.sha256.equals(currentHash)" in cache and "RideMileageIntegrity.fingerprint(track)" in cache,
 "cache miss uses two source passes not three": "String parsed=toHex(parsedDigest.digest())" in cache and "!parsed.equals(after)" in cache and "String before=" not in cache,
 "cache never feeds odometer": "NEVER evidence for odometer" in cache,
 "explicit release version": Path("release-code.txt").read_text().strip()=="575"
}
for label,ok in checks.items(): print(("PASS " if ok else "FAIL ")+label)
assert all(checks.values())
PY
