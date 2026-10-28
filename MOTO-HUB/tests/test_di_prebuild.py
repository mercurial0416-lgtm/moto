from pathlib import Path
import json,hashlib,re
root=Path(__file__).resolve().parents[1]
src=root/'app/src/main/java/com/motohub/next'
manifest=json.loads((root.parent/'updates/manifest.json').read_text())
pin=re.search(r'PINNED_CERT_SHA256="([a-f0-9]{64})"',(src/'UpdateManifest.java').read_text()).group(1)
assert manifest['certificateSha256']==pin
assert manifest['versionCode']==517  # prior VERIFIED fixed-signed release, not a fake 518 build
assert manifest['sizeBytes']==942154
assert manifest['sha256']=='93fe78dfe68faa62b8c8460b8ae2e22d2e8806961b247e4d5099e8586358a6dd'
assert 'raw.githubusercontent.com' in manifest['apkUrl']
assert 'versionCode 518' in (root/'app/build.gradle').read_text()
assert 'PINNED_CERT_SHA256.equals(certificateSha256)' in (src/'UpdateManifest.java').read_text()
assert 'SigningIdentity.current(a)' in (src/'AppUpdater.java').read_text()
assert 'PrivateCacheCleanup.atProcessStart' in (src/'MainActivity.java').read_text()
assert 'void listings(){' not in (src/'HubScreens.java').read_text()
assert 'a.recordPageOffset' in (src/'HubScreens.java').read_text()
assert 'a.recordPageOffset' in (src/'HubScreens.java').read_text()
assert 'decodeLocalSampled(manual,1200)' in (src/'MotorcyclePhoto.java').read_text()
assert 'updateProgress("APK 다운로드 중",nextProgress)' in (src/'AppUpdater.java').read_text()
assert 'UpdateProgressPolicy.label(action,percent)' in (src/'AppUpdater.java').read_text()
assert 'UpdateDownloadPolicy.safeRedirect' in (src/'AppUpdater.java').read_text()
assert 'ObdConnectionPolicy.MAX_RETRIES' in (src/'ObdAutoSpeedMonitor.java').read_text()
assert 'ObdReadOnlyProtocol.query' in (src/'ObdAutoSpeedMonitor.java').read_text()
print('PASS DI prebuild source, update manifest and privacy wiring checks')
