#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
p=Path('app/src/main/java/com/motohub/next')
src=lambda name:(p/name).read_text(encoding='utf-8')
quick=src('QuickEntryUi.java')
s=quick[quick.index('StringBuilder warnings=new StringBuilder();'):quick.index('if(warnings.length()>0)')]
ride=src('RideUi.java')
scan=ride[ride.index('void loadHistoryAsync('):ride.index('void populateHistory(')]
home=src('GarageHomeUi.java')
photo=src('MotorcyclePhoto.java')
service=src('RideService.java')
policy=src('GpsStorageSpacePolicy.java')
checks={
 'fuel warnings contain real newline escapes': '간격' not in s and r'\n' in s and r'\\n' not in s,
 'no brittle fuel newline replace': 'warnings.toString().replace(' not in quick,
 'storage gate checks before GPS track open': service.index('GpsStorageSpacePolicy.require(getFilesDir());') < service.index('track=new File(TrackFile.directory(getFilesDir())'),
 'storage gate exists before consent consumption': ride.index('GpsStorageSpacePolicy.require(a.getFilesDir());') < ride.index('if(!a.rideStartConsent.consume('),
 'space policy rejects unknown/low capacity': '48L*1024L*1024L' in policy and 'new StatFs(filesDir.getAbsolutePath()).getAvailableBytes()' in policy,
 'corrupt user image retained with auto fallback': 'MotorcyclePhoto.showFallback(a,iv,credit,brand,model' in home and 'skipInvalidLocal' in photo and 'if(garageMode&&!skipInvalidLocal)' in photo,
 'background ride scan only reads copied notes': 'rideNotesSnapshot=liveNotes==null?' in scan and 'RideNotes.title(stableDb,file)' in scan and 'RideNotes.memo(stableDb,file)' in scan,
 'no mutable database read inside history worker': 'RideNotes.title(a.db,file)' not in scan and 'RideNotes.memo(a.db,file)' not in scan,
 'fixed signing fingerprint unchanged': '212c5b39d0082ab4bed78f8da2b719819cd80a20bc116c84f8bdfabca507021b' in src('UpdateManifest.java'),
}
for title,ok in checks.items():print(('PASS' if ok else 'FAIL')+': UX93 '+title)
assert all(checks.values()),"UX93 integrity regression failed"
PY
