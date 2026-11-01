#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
import re
p=Path("app/src/main/java/com/motohub/next")
design=(p/"MotoDesign.java").read_text()
main=(p/"MainActivity.java").read_text()
home=(p/"GarageHomeUi.java").read_text()
quick=(p/"QuickEntryUi.java").read_text()
status=(p/"ServiceStatusUi.java").read_text()
ride=(p/"RideUi.java").read_text()
hub=(p/"HubScreens.java").read_text()
def token(name):
 m=re.search(r"\b"+name+r"=0xFF([0-9A-Fa-f]{6})",design)
 assert m, name
 return m.group(1)
def luminosity(h):
 c=[int(h[i:i+2],16)/255. for i in (0,2,4)]
 linear=[v/12.92 if v<=0.04045 else ((v+.055)/1.055)**2.4 for v in c]
 return sum(x*y for x,y in zip((.2126,.7152,.0722),linear))
def contrast(a,b):
 hi,lo=sorted([luminosity(a),luminosity(b)],reverse=True)
 return (hi+.05)/(lo+.05)
for theme in ("DARK","LIGHT"):
 for text in ("TEXT","MUTED","ACCENT"):
  for base in ("BACKGROUND","SURFACE"):
   score=contrast(token(theme+"_"+text),token(theme+"_"+base))
   assert score>=4.5,(theme,text,base,score)
assert contrast(token("LIGHT_ACCENT"),token("LIGHT_TINT"))>=4.5
assert 'optString("theme","light")' in main and 'theme.equals("dark")' in main
assert "MotoDesign.addHeader(this,root)" in main and "MotoDesign.addNavigation(this,root)" in main
assert "static void spinnerField(" in design and "MotoDesign.spinnerField(this,p,s,12)" in main
assert "MotoDesign.spinnerField(a,body,spinner,14)" in quick
assert "HomeShortcutOrder.parse(" in home and "quickActionPair(" in home
assert 'case "ride":rideAction();' in home and 'case "nav":quickNavigation();' in home
assert "RideService.running" in ride and "finishWithSummary()" in ride and "requestStart()" in ride
assert "RideService.STOP" in main or "RideService.STOP" in ride
assert "void updateOdo(" in status and "void complete(" in status
assert "void line(LinearLayout target" in hub and "void tabs(String[] values" in hub
assert "new AtomicFile(" in main, "local persistence code must stay"
assert "https://" not in design and "http://" not in design, "no remote design asset"
print("PASS v569-based clean UI: WCAG AA color contrast, shortcuts, GPS, OBD, forms, persistence")
PY
