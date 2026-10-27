from pathlib import Path
s=Path('app/src/main/java/com/motohub/next')
main=(s/'MainActivity.java').read_text()
up=(s/'AppUpdater.java').read_text()
hub=(s/'HubScreens.java').read_text()
sharing=(s/'Sharing.java').read_text()
assert 'UpdateReadiness.fixedIdentity' in up and up.index('UpdateReadiness.fixedIdentity',up.index('void check()')) < up.index('connect(BuildConfig.UPDATE_MANIFEST_URL)',up.index('void check()'))
assert 'UpdateStoragePolicy.fits' in up and up.index('UpdateStoragePolicy.fits',up.index('void download(')) < up.index('connect(m.apkUrl,requestOffset)',up.index('void download('))
assert 'RideDiagnostics.summarize' in hub and 'ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS' in hub and 'ridePreflight' not in hub
assert 'AutoPhotoCache.trim' in main
assert 'SharingPrivacyPolicy.validUuid(groupId)' in sharing
assert 'if(!RideService.shareAllowed()||generation!=g)' in sharing
assert '공유 시작 실패 · 위치 미전송' in sharing
assert 'MOTO_SIGNING' in Path('app/build.gradle').read_text()
assert 'versionCode 518' in Path('app/build.gradle').read_text()
print('PASS DI quality source integration assertions: 9')
