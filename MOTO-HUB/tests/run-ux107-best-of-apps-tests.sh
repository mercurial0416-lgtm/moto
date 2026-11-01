#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
p=Path("app/src/main/java/com/motohub/next")
main=(p/"MainActivity.java").read_text()
home=(p/"GarageHomeUi.java").read_text()
ride=(p/"RideUi.java").read_text()
hub=(p/"HubScreens.java").read_text()
timeline=(p/"MotoTimeline.java").read_text()
ui=(p/"MotoTimelineUi.java").read_text()
report=(p/"MotoOwnershipReport.java").read_text()

# Cross-bike data isolation. GPS sidecars must be strictly validated; GPS never
# replaces a manually verified odometer reading.
assert 'bikeId.equals(item.optString("vehicleId"))' in timeline
assert 'expectedBike.equals(RideBikeBinding.readStrict(track))' in timeline
assert 'RideMileageIntegrity.validCompleted(track)' in timeline
assert 'RideHistoryLabel.startTime(track)' in timeline
assert 'MotoTimeline.sort(merged)' in ui
assert 'RideHistorySort.order(TrackFile.tracks(' in ui
assert 'if(inspected++>=200)break;' in ui
assert 'if(count>40' not in ui # count bound handled in filtered display
assert 'MotoTimeline.filtered(events,selected[0],40)' in ui
assert 'new Thread(()->{' in ui and 'a.runOnUiThread(()->{' in ui
assert 'a.dialog!=opened||!opened.isShowing()' in ui
assert 'a.openRideInsights(track)' in ui and 'a.edit(e.collection,item)' in ui

# PDF report exports only explicitly selected vehicle and explicitly completed
# garage records. It never silently certifies a scheduled maintenance task.
assert 'scheduleOnly' in report and 'if(schedule)continue' in report
assert 'bikeId.equals(record.optString("vehicleId"))' in report
assert '사용자 입력 기록이며 공식 점검' in report
assert 'GPS 좌표, 라이더 연락처' in report
assert 'PdfDocument.PageInfo.Builder(' in report and 'pdf.writeTo(output)' in report
assert 'ACTION_CREATE_DOCUMENT' in main and 'application/pdf' in main
assert 'pendingOwnershipReportBike' in main
assert 'Store.parse(snapshot)' in main
assert 'VerifiedDocumentExport.save(getContentResolver(),uri,MotoOwnershipReport.MAX_PDF_BYTES' in main
assert 'RestoreRideGate.beginExport(' in main and 'RestoreRideGate.endExport()' in main

# Core actions are accessible: navigation/GPX, manual route entry, ride stats.
assert 'a::savedRoutes' in home and 'new MotoTimelineUi(a).open()' in home
assert 'new MotoTimelineUi(a).open()' in hub and 'a.exportOwnershipPdf(b.optString("id"))' in hub
assert 'new QuickEntryUi(a).fuel()' in home and 'new QuickEntryUi(a).maintenance()' in home
assert 'NavigationApps.openPreferred(a)' in ride and 'NavigationApps.choose(a)' in ride
assert 'Button savedRoutes=a.button("저장한 코스 "+saved+"개"' in ride
assert 'Button importRoute=a.button("GPX 가져오기"' in ride
assert '최근 30일 주행 통계 · 바이크별' in ride
assert 'RideService.running?"라이딩 저장 후 종료"' in ride

# Live status banner is a read-only shortcut, not a second GPS controller.
banner=main.split('void addRideStatusBanner(){',1)[1].split(' void render(){',1)[0]
assert 'status.setOnClickListener(v->navigate("라이딩"))' in banner
assert 'RideService.startedElapsed' in banner and 'RideService.liveDistanceKm' in banner
assert 'status.postDelayed(this,5000L)' in banner
assert 'RideService.STOP' not in banner and 'startService' not in banner
assert 'new String[]{"주유","보험","부품","튜닝","기타"}' in main

# Unchanged persistence and permission architecture; no fake route engine.
assert 'void atomic(AtomicFile file,byte[] bytes)' in main
assert 'LocalSafetyBackup.checkpoint(' in main
assert 'Sharing.start(' not in ui
assert 'Sharing.start(' not in timeline
print("PASS UX107 integrated local timeline, ownership PDF, route hub and cross-tab ride status wiring")
PY
