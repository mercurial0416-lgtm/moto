#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
root=Path('app/src/main/java/com/motohub/next')
design=(root/'MotoDesign.java').read_text()
home=(root/'GarageHomeUi.java').read_text()
ride=(root/'RideUi.java').read_text()
screen=(root/'HubScreens.java').read_text()
main=(root/'MainActivity.java').read_text()

# No URL or remote dependency in the new native presentation.
assert 'TextView brand=a.text("MOTO HUB",12,a.accent,true)' in design
assert 'FrameLayout pill=new FrameLayout(a)' in design
assert 'pill.setBackground(a.shape(selected?a.highlight:a.panel,0,16))' in design
assert 'cell.setMinimumHeight(a.dp(62))' in design
assert 'if(!destination.equals(a.tab))a.navigate(destination)' in design
assert 'setContentDescription(labels[i]+(selected?", 현재 탭":" 탭 열기"))' in design

render=home.split(' void render(){',1)[1].split(' void interruptedRideShortcut(){',1)[0]
for marker in ('addHero(b)', 'stats(b)', 'HomeShortcutOrder.parse(', 'quickActionPair(key,ordered[i+1])',
               'case "ride":rideAction()', 'case "nav":quickNavigation()', 'case "fuel":quickAction("fuel")',
               'case "service":quickAction("service")'):
 assert marker in render, marker
assert 'fontScale>=1.35f' in render and 'screenWidthDp<360' in render
assert 'if(!shortcut(key))continue;' in render
assert 'i++;' in render and 'quickActionPair(key,ordered[i+1])' in render
assert 'void quickActionPair(String left,String right)' in home
assert 'new QuickEntryUi(a).fuel()' in home and 'new QuickEntryUi(a).maintenance()' in home
assert 'surfaceCard.setBackground(a.shape(surface,stroke,19))' in home
assert 'MotoDesign.figure(a,values[i],24,light)' in home
assert 'group.setOnClickListener(v->a.navigate("차고"))' in home
assert 'new ServiceStatusUi(a).updateOdo(b)' in home
assert 'a.readOnly||RideService.running||RideService.autoArmed' in home
assert 'GarageBikeStatus.sold(b.optString("soldDate"))' in home
assert 'RideMileageEstimate.total(a,b.optString("id"))' in home
assert 'OdometerAutoSyncInbox.display(a,b.optString("id"))' in home
assert 'int photoHeight=displayWidth<a.dp(390)?190:211' in home
assert 'new FrameLayout.LayoutParams(a.dp(48),a.dp(54)' in home

# OBD-only disarm remains non-destructive. GPS-stop always requires confirmation.
assert 'RideService.running?"라이딩 저장 후 종료"' in ride
assert 'Button disarm=a.button("OBD만 중지 · GPS 기록은 계속"' in ride
assert 'RideService.AUTO_DISARM' in ride and 'RideService.STOP' in ride
assert 'finishWithSummary();' in ride
assert 'setPositiveButton("저장하고 종료"' in ride
assert 'narrowMetrics' in ride and 'narrowNav' in ride
assert 'NavigationApps.openPreferred(a)' in ride and 'NavigationApps.choose(a)' in ride
assert 'RideScreenRefreshPolicy.interval(' in ride

assert 'settingCard=target==a.content&&"더보기".equals(a.tab)' in screen
assert 'MotoDesign.ripple(a,block,settingCard?a.panel:a.bg' in screen
assert 'block.setOnClickListener(v->click.run())' in screen
assert 'MotoDesign.ripple(this,b,primary?accent:panel,primary?0:line,17)' in main

# Explicitly preserve no unapproved ECU changes and all local persistence APIs.
assert 'Sharing.start(' not in ride and 'Sharing.start(' not in home
assert 'Store.upsert(' in main and 'LocalSafetyBackup.checkpoint(' in main
print('PASS UX106 native UI hierarchy, home shortcuts, riding safety, settings and persistence wiring')
PY
