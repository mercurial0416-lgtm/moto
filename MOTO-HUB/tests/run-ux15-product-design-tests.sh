#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
src=Path("app/src/main/java/com/motohub/next")
home=(src/"GarageHomeUi.java").read_text()
design=(src/"MotoDesign.java").read_text()
icons=(src/"MotoQuickIcon.java").read_text()
activity=(src/"MainActivity.java").read_text()
assert 'ImageView.ScaleType.FIT_CENTER' in home, "keep bike visible without cropping"
assert '0x58000000' in home and '0xC9081016' not in home, "avoid thick faux-cinematic overlay"
assert 'TextView tag=a.text(HomeBikeSwitch.label' not in home, "no promotional badge covering photos"
assert 'imageArea.setOnTouchListener' in home and 'switchBike(direction)' in home and 'new HubScreens(a).chooseBike()' in home, "switch bikes via swipe/arrows without cropping photo"
assert home.index('addHero(b);') < home.index('stats(b);') < home.index('quickActions();') < home.index('rideAction();'), "clear hero, metrics, quick recording, GPS controls"
assert 'String[] names={"계기판 거리","다음 정비"};' in home, "honest odometer and maintenance KPIs"
assert 'String.format(Locale.ROOT,"%02d",i+1)' not in home, "no arbitrary numbered menu"
assert 'Button fuel=a.button("주유 기록",true' in home and 'Button maintenance=a.button("정비주기",false' in home, "native buttons replace unreadable mini icons"
assert 'new QuickEntryUi(a).fuel()' in home and 'new QuickEntryUi(a).maintenance()' in home, 'first-class quick fuel and reminders'
assert 'a.navigate("라이딩")' in home and 'new HubScreens(a).manageBike()' in home
assert 'if(!a.items("listings").isEmpty())' in home, "no empty marketplace section on home"
assert 'friends();' not in home.split(' void render(){')[1].split(' void interruptedRideShortcut(')[0], "no unnecessary account card"
assert 'addHeader(MainActivity a' in design and 'addNavigation(MainActivity a' in design
assert 'View mark=new View(a)' not in design and 'View indicator=new View(a)' not in design
assert 'ripple(a,cell,a.panel,0,10);' in design
assert 'if(!t.equals(MotoDesign.heading(tab)))' in activity, "avoid repeated page header"
assert 'content.animate().alpha(1f)' not in activity, "no full-screen fade for every GPS refresh"
assert 'android.graphics.Canvas' in icons and 'android.graphics.Paint' in icons
assert 'drawCircle' in icons and 'drawLine' in icons
assert 'Sharing.start(' not in home and 'Sharing.start(' not in icons
print("PASS UX15 real product layout, navigation, photography and functional shortcuts")
PY
