#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
r=Path("app/src/main/java/com/motohub/next")
read=lambda n:(r/n).read_text(encoding="utf-8")
nav=read("NavigationApps.java")
ui=read("RideUi.java")
settings=read("HubScreens.java")
mapui=read("RideMapActivity.java")
cache=read("MapCachePolicy.java")
backup=read("MainActivity.java")
obd=read("ObdDashboardActivity.java")
report=read("RideTypeComparison.java")
assert 'setItems(options,' in nav and '.putString(KEY,selectedApp.id).commit()' in nav
assert 'NavigationApps.choose(a)' in ui and 'NavigationApps.openPreferred(a)' in ui
assert ui.index('NavigationApps.choose(a)')<ui.index('if(focus&&RideService.running)return;')
assert 'line("기본 내비"' in settings and 'a.action(page,"선택한 내비 실행"' in settings
assert 'for(ArrayList<GeoPoint> section:sections)' in mapui
assert 'MapPathContinuity.validFix' in mapui and 'MapPathContinuity.validLink' in mapui
assert 'sectionIndices.get(i-1).equals(sectionIndices.get(i))' in mapui
assert 'if(delete)toDelete.add(file)' in cache and 'for(File file:toDelete)' in cache
assert cache.index('for(File file:toDelete)')>cache.index('while(!queue.isEmpty())')
assert 'RideScreenRefreshPolicy.interval(' in ui
assert 'ObdQualityAdvice.explain(' in obd
assert 'RideTypeComparison categoryComparison' in ui and 'categoryComparison.add' in ui
assert 'RestoreArchive.prepare(in,getFilesDir())' in backup and 'try(prepared)' in backup
assert '기존 ZIP 복구 가능 여부 검사' in settings
print("PASS UX58 navigation selector availability, gap-safe map, cache preflight, ride comparisons, and dry-run backup")
PY
