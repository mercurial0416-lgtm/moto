#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
import re

root=Path("app/src/main/java/com/motohub/next")
design=(root/"MotoDesign.java").read_text()
home=(root/"GarageHomeUi.java").read_text()
ride=(root/"RideUi.java").read_text()
screens=(root/"HubScreens.java").read_text()
main=(root/"MainActivity.java").read_text()

def needs(data,*phrases):
    for phrase in phrases:
        assert phrase in data, "Expected UI/behavior contract missing: "+phrase

def rgb_int(name):
    m=re.search(r"\b"+re.escape(name)+r"=0xFF([A-Fa-f0-9]{6})\s*;",design)
    assert m, name+" missing"
    h=m.group(1)
    return tuple(int(h[i:i+2],16)/255 for i in (0,2,4))
def lightness(rgb):
    x=[v/12.92 if v<=0.04045 else ((v+0.055)/1.055)**2.4 for v in rgb]
    return sum(a*b for a,b in zip(x,(.2126,.7152,.0722)))
def contrast(name1,name2):
    hi,lo=sorted((lightness(rgb_int(name1)),lightness(rgb_int(name2))),reverse=True)
    return (hi+.05)/(lo+.05)
for text,bg in (
    ("DARK_TEXT","DARK_SURFACE"),("DARK_MUTED","DARK_SURFACE"),
    ("DARK_ACCENT","DARK_BACKGROUND"),("LIGHT_TEXT","LIGHT_SURFACE"),
    ("LIGHT_MUTED","LIGHT_SURFACE"),("LIGHT_ACCENT","LIGHT_SURFACE"),
    ("LIGHT_ACCENT","LIGHT_BACKGROUND"),
):
    assert contrast(text,bg)>=4.5, (text,bg,contrast(text,bg))

# Primary navigation still reaches the ORIGINAL tab identifiers.
needs(main,'final String[] tabs={"홈","라이딩","차고","더보기"}')
needs(design,'String[] labels={"홈","라이딩","정비·주유","설정"}','a.navigate(destination)')
needs(home,'compactVehicleHeader(bike)','homeShortcutTile(key)','라이딩 시작','requestStart(true)','editHomeShortcuts()')
needs(ride,'라이딩 기록','rideTools.setOnClickListener','RideStatsUi.open(a)',
      'chooseRideReport()','a.pickGpx()','if(focus&&RideService.running)return')
needs(screens,'void settings(){a.section("데이터 보호")','if(Sharing.enabled){a.section("안전 · 위치 공유")',
      'line("백업 · 복원"','AppUpdater(a).show()','this::advancedSettings')
needs(main,'LinearLayout page(String title)','toolbar.setMinimumHeight(dp(56))',
      'formBody=col()','formFooter=col()','this::close',
      'if(readOnly)','void confirmImport(')
# Source cannot claim real-ECU validation; the UI layer must preserve OBD & GPS.
needs(ride,'RideService.running','RideService.autoArmed')
needs(screens,'this::basicBackup','UpdateReadiness.fixedIdentity')
print("PASS UX110: contrast, navigation, core actions, backups, dialogs, and ride controls")
PY
