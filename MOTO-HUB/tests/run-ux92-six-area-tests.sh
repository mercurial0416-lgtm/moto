#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
p=Path("app/src/main/java/com/motohub/next")
read=lambda n:(p/n).read_text(encoding="utf-8")
home=read("GarageHomeUi.java")
gps=read("RideCompletionInbox.java")
obd=read("DeveloperObdScreen.java")
obd_view=read("ObdDashboardActivity.java")
fuel=read("QuickEntryUi.java")
backup=read("BackupGpsProof.java")
settings=read("HubScreens.java")
manifest=Path("app/build.gradle").read_text()
checks={
 "asynchronous sampled home photo decoding": 'HOME_IO.execute(()->{' in home and 'MotorcyclePhoto.decodeLocalSampled(custom,1024)' in home,
 "bounded photo cache": 'LruCache<String,android.graphics.Bitmap>(8*1024*1024)' in home,
 "old bike image prevented": 'bikeId.equals(a.bike().optString("id"))' in home and 'key.equals(iv.getTag())' in home,
 "gps gaps shown without modifying odometer": 'result.gpsGaps,result.gpsObservedMillis/60000' in gps and 'GPS 수신 공백' in gps,
 "stale speed not shown": '마지막 속도는 숨깁니다' in obd_view,
 "reconnect only when stopped": 'if(RideService.autoArmed||RideService.running)' in obd and 'loadPairedAuto();' in obd,
 "recent fuel suggestion tucked away": 'tools.addView(lastFuel);' in fuel,
 "fuel anomaly warning shows actual values": 'String.format(java.util.Locale.KOREA,"%.2f L · %,.0f원"' in fuel and 'record.optString("date")' in fuel,
 "backup matches current JSON not merely timestamp": 'fullCurrentStatus(Context context,String currentJson)' in backup and 'MessageDigest.isEqual' in backup,
 "backup state visible in simple settings": 'line("백업 · 복원","GPS 백업 상태를 백그라운드에서 검사합니다"' in settings and 'backupState.setText(status)' in settings,
 "signature and version state in settings": 'UpdateReadiness.fixedIdentity(SigningIdentity.current(a),UpdateManifest.PINNED_CERT_SHA256)' in settings,
 "unchanged Android application identity": "applicationId 'com.motohub.next'" in manifest,
}
for name,ok in checks.items():
 print(("PASS" if ok else "FAIL")+" UX92 "+name)
assert all(checks.values()),"UX92 implementation regression"
PY
