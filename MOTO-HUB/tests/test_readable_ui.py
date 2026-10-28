#!/usr/bin/env python3
"""UX39 visual-regression guards. Pure Python: does not require Android SDK."""
from pathlib import Path
import re

source = Path(__file__).resolve().parents[1] / "app/src/main/java/com/motohub/next"
get = lambda name: (source / name).read_text(encoding="utf-8")
main, design, garage, records, nav = map(get, (
    "MainActivity.java", "MotoDesign.java", "GarageHomeUi.java",
    "HubScreens.java", "NavIcon.java"))
checks = 0


def test(truth, message):
    global checks
    checks += 1
    assert truth, message


test('t.setLineSpacing(dp(size>=14?2:1),1f);' in main,
     "multiline labels should have consistent leading")
test('setFontFeatureSettings("tnum")' in design,
     "numeric values must use tabular digit widths")
test(design.count("static TextView figure(") == 1,
     "one reusable figure type style")
test("MotoDesign.figure(a,values[i],22,light)" in garage,
     "odometer must use numeric figure style")
test("MotoDesign.figure(a,a.won(" in garage,
     "recent maintenance expenses must use numeric figure style")
test("MotoDesign.figure(a,values[i],i==0?19:18,light)" in garage,
     "monthly expense and efficiency must use numeric style")
test("MotoDesign.figure(a,value,16,a.fg)" in records,
     "right aligned currency values should be differentiated")
test('hint.setGravity(Gravity.END|Gravity.CENTER_VERTICAL)' in records,
     "currency values should align to the right")
test('TextView summary=a.text(detail,13,a.muted,false)' in records,
     "record descriptions must not be tiny")
test('summary.setMaxLines(3)' in records,
     "record descriptions should be bounded but legible")
test('MotoDesign.figure(a,a.won(x.optDouble("price")),23,a.fg)' in records,
     "listing prices should have stable-width digits")
test('year=x.optInt("year")>0' in records,
     "unknown year must not render as 0 model year")
test('value.setMaxLines(2);value.setMinHeight(a.dp(30))' in garage,
     "odometer/maintenance values should have room for large text")
test('new LinearLayout.LayoutParams(-1,-2);vp.topMargin=a.dp(6)' in garage,
     "metrics cannot use a hardcoded one-line height")
test('model.setMaxLines(2);model.setEllipsize(' in records,
     "long listing names should not collide with status")
test('b.setMinHeight(a.dp(44))' in records and 'b.setTextColor(selected?a.accent:a.fg)' in records,
     "filter controls should remain tappable with readable selected state")
test('case 1: // Listings: price tag' in nav,
     "listing tab must use a coherent icon")
test('default: // Settings: eight-tooth gear' in nav,
     "settings tab must use a recognizable icon")
test('c.drawCircle(12,12,3.0f,p)' in nav,
     "gear center must be visible")
test('new QuickEntryUi(a).fuel()' in garage and
     'new QuickEntryUi(a).maintenance()' in garage,
     "functional quick-entry actions must be retained")
rideui=(JAVA/'RideUi.java').read_text()
test('new RideUi(a).requestStart()' in garage and
     'new RideUi(a).finishWithSummary()' in garage and
     'RideService.STOP' in rideui and '".done"' in rideui,
     "ride start consent, stop confirmation and verified finish still wired")
test('switchBike(direction)' in garage and 'changeHeroPhoto(bike,own)' in garage,
     "photo editing and multi-bike switching still wired")
print(f"PASS UX39 typography / graphics / actions: {checks} checks")
