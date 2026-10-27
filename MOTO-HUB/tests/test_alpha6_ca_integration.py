from pathlib import Path
from version_consistency import version_declaration_ok, debug_alignment_ok, signed_alignment_ok
src=Path('app/src/main/java/com/motohub/next')
def java(name):return (src/(name+'.java')).read_text()
checks={
'home sold label': '판매 완료 · 기록 보관 중' in java('GarageHomeUi'),
'quick entry service chips': '"오일필터","체인","브레이크 패드"' in java('MainActivity'),
'GPS signal stale resets estimated speed': 'now-lastFixElapsed>35_000' in java('RideService') and 'SystemClock.elapsedRealtime()' in java('RideService') and 'liveSpeedKmh=0;lastGood=null' in java('RideService'),
'backup user review without overwriting': '지금 안전 백업 확인' in java('HubScreens') and 'if(destination.exists()||java.nio.file.Files.isSymbolicLink(destination.toPath()))return;' in java('LocalSafetyBackup'),
'photo decode bounded': 'options.outWidth/options.inSampleSize>1024' in java('GarageHomeUi'),
'widget does zero garage disk IO': 'getFilesDir()' not in java('MotoWidgetProvider') and 'R.layout.moto_widget' in java('MotoWidgetProvider'),
'maintenance presets explicit': '정비 간격 선택 · 확인 후 적용' in java('MainActivity'),
'ride moving duration': 'movingMillis' in java('RideInsights') and 'GPS 추정 이동 시간' in java('MainActivity'),
'sold bikes retained and selectable': '판매 처리 (기록 보존)' in java('HubScreens') and 'GarageBikeStatus.label' in java('HubScreens'),
'group location sharing fail-closed': 'failedSends>=3' in java('Sharing') and 'Sharing.start(id)' in java('GroupUi'),
'settings grouped': all(x in java('HubScreens') for x in ['데이터 및 복구','표시 및 알림','개인정보 및 라이딩','내 바이크와 도구']),
'version updated': version_declaration_ok(),
'CI updated': debug_alignment_ok() and 'run-ca-improvements-tests.sh' in Path('.github/workflows/android-debug.yml').read_text(),
'signed release only with secrets': 'MOTO_SIGNING_STORE_B64' in Path('.github/workflows/android-signed-release.yml').read_text(),
}
for title,yes in checks.items():print(('PASS' if yes else 'FAIL')+': '+title)
assert all(checks.values()),'CA wiring regression'
