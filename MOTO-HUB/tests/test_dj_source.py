from pathlib import Path
p=Path('app/src/main/java/com/motohub/next')
map=(p/'RideMapActivity.java').read_text()
assert 'map.setUseDataConnection(false)' in map
assert 'setOnlineTiles(true)' in map and 'new AlertDialog.Builder(this)' in map
assert 'OpenStreetMap 타일 서버' in map
assert map.index('map.setUseDataConnection(false)') < map.index('new Thread(()->')
assert 'setOnlineTiles(allowed)' not in map or 'map.setUseDataConnection(allowed)' in map
h=(p/'HubScreens.java').read_text()
assert 'this::localDiagnostics' in h and 'LocalDiagnosticReport.create' in h
assert 'ClipData.newPlainText("MOTO HUB 로컬 진단",report)' in h
assert 'SocialClient.INSTANCE.access' not in h[h.index('void localDiagnostics()'):h.index('void serverPreflight()')]
net=(p/'OfflineStatus.java').read_text()
assert 'NET_CAPABILITY_VALIDATED' in net
assert 'catch(Exception ignored){return false;}' in net
assert 'UpdateCacheRetention.prune(dir,m.sha256)' in (p/'AppUpdater.java').read_text()
print('PASS offline map privacy + diagnostics + validated network checks')
