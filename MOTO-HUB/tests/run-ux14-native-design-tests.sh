#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
root=Path('app/src/main/java/com/motohub/next')
main=(root/'MainActivity.java').read_text()
design=(root/'MotoDesign.java').read_text()
home=(root/'GarageHomeUi.java').read_text()
lists=(root/'HubScreens.java').read_text()
ride=(root/'RideUi.java').read_text()
assert 'int[] palette=MotoDesign.colors(dark)' in main, 'all screens use one palette'
assert 'dark=tab.equals("홈")' not in main, 'light preference must be honored on home'
assert 'MotoDesign.addHeader(this,root);' in main
assert 'MotoDesign.addNavigation(this,root);' in main
assert 'new GarageHomeUi(this).header(top)' not in main, 'no duplicate branded toolbar'
assert 'if(tab.equals("홈")&&bike()!=null)pair(dock,' not in main
assert 'setElevation(0)' in main and 'MotoDesign.ripple(this,b,' in main, 'native quiet controls'
assert 'private MotoDesign(){}' in design
assert 'DARK_ACCENT=0xFF80ADFF' in design and 'LIGHT_ACCENT=0xFF1D66DC' in design
assert 'root.addView(bar);' in design and 'new NavIcon(a,icons[i]' in design
assert 'cell.setContentDescription(labels[i]+(selected?", 선택됨":""))' in design
assert 'headerAction(a,"알림")' in design and '.showNotices()' in design
assert 'orange=a.accent;light=a.fg;soft=a.muted;surface=a.panel' in home
assert 'void quickActions(){' in home and 'new MotoQuickIcon(a,index,orange)' in home and 'for(int r=0;r<2;r++)' in home
assert '"⛽","⚒","▤","▣"' not in home, 'no emoji shortcut placeholders'
assert 'void stats(JSONObject b)' in home and 'String[] names={"총 주행거리","다음 정비"};' in home
assert 'void line(LinearLayout target' in lists and 'MotoDesign.ripple(a,block,a.panel,a.line,17)' in lists
assert 'a.pair(actions,"간편 주유"' in lists and 'QuickEntryUi(a).maintenance()' in lists and '🔧 정비' not in lists
assert '"라이딩 시작하기"' in ride and 'title.setTypeface(MotoDesign.display())' in ride
assert 'finishRide.setEnabled(!saving)' in ride and 'RideService.saving()' in ride
assert 'Sharing.start(' not in ride and 'Sharing.start(' not in home
print('PASS UX14 native typography, themes, readable IA, tap targets and ride/data wiring')
PY
