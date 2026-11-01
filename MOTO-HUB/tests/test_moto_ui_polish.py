#!/usr/bin/env python3
"""Static quality gates for the native MOTO HUB UI redesign.
This is a structural/contrast check, not an emulator screenshot test.
"""
from pathlib import Path
import re

root=Path(__file__).resolve().parents[1]/"app/src/main/java/com/motohub/next"
design=(root/"MotoDesign.java").read_text(encoding="utf-8")
main=(root/"MainActivity.java").read_text(encoding="utf-8")
home=(root/"GarageHomeUi.java").read_text(encoding="utf-8")
garage=(root/"HubScreens.java").read_text(encoding="utf-8")
ride=(root/"RideUi.java").read_text(encoding="utf-8")
quick=(root/"QuickEntryUi.java").read_text(encoding="utf-8")
service=(root/"ServiceStatusUi.java").read_text(encoding="utf-8")

def color(name):
    match=re.search(r"\b"+name+r"=0xFF([A-Fa-f0-9]{6})",design)
    assert match, "missing design token "+name
    return match.group(1)

def luminance(h):
    channels=[int(h[i:i+2],16)/255 for i in (0,2,4)]
    linear=[c/12.92 if c<=.04045 else ((c+.055)/1.055)**2.4 for c in channels]
    return sum(weight*c for weight,c in zip((.2126,.7152,.0722),linear))

def contrast(a,b):
    bright,dark=sorted((luminance(a),luminance(b)),reverse=True)
    return (bright+.05)/(dark+.05)

for theme in ("LIGHT","DARK"):
    background=color(theme+"_BACKGROUND")
    surface=color(theme+"_SURFACE")
    for token in ("TEXT","MUTED","ACCENT"):
        fg=color(theme+"_"+token)
        assert contrast(fg,background)>=4.5,(theme,token,"background",contrast(fg,background))
        assert contrast(fg,surface)>=4.5,(theme,token,"surface",contrast(fg,surface))

assert contrast(color("LIGHT_ACCENT"),color("LIGHT_TINT"))>=4.5
assert "theme\",\"light\"" in main, "new installs should begin with the clean light UI"
assert 'theme.equals("dark")' in main and 'theme.equals("system")' in main
assert "static void addNavigation(" in design and "static void addHeader(" in design
assert "static void spinnerField(" in design
assert "MotoDesign.spinnerField(this,p,s,12)" in main
assert "MotoDesign.spinnerField(a,body,spinner,18)" in quick
assert 'new MotoQuickIcon(a,index,orange)' in home and 'for(int r=0;r<2;r++)' in home
assert "showNotices()" in home and "RideService.STOP" in home, "home ride controls must remain"
assert "void line(LinearLayout target" in garage and "MotoDesign.ripple(a,block,a.panel,a.line,17)" in garage
assert "void tabs(String[] values" in garage and "select.pick(value)" in garage
assert "RideService.saving()" in ride and "requestStart()" in ride
assert "TrackFile" in ride and "GPX 가져오기" in ride
assert "void updateOdo(" in service and "void complete(" in service
assert "void saveItem(" in main and "new AtomicFile(" in main, "original data store remains"
assert "https://" not in design and "http://" not in design, "no remote artwork / styles"

print("MOTO HUB native UI contrast and action-wiring checks: PASS")
