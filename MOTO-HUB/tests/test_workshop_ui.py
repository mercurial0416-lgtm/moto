#!/usr/bin/env python3
"""UX38 static regression and palette legibility checks (no Android SDK needed)."""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/com/motohub/next"
home = (JAVA / "GarageHomeUi.java").read_text()
design = (JAVA / "MotoDesign.java").read_text()
hub = (JAVA / "HubScreens.java").read_text()
main = (JAVA / "MainActivity.java").read_text()
checks = 0


def check(condition, message):
    global checks
    checks += 1
    assert condition, message


def color(name):
    match = re.search(r"static final int " + name + r"=(0x[0-9A-Fa-f]{8});", design)
    assert match, name
    return int(match.group(1), 16)


def luminance(rgb):
    channels = [(rgb >> shift) & 255 for shift in (16, 8, 0)]
    converted = [n / 255 / 12.92 if n / 255 <= .04045
                 else ((n / 255 + .055) / 1.055) ** 2.4 for n in channels]
    return sum(x * y for x, y in zip(converted, (.2126, .7152, .0722)))


def contrast(a, b):
    x, y = sorted((luminance(a), luminance(b)), reverse=True)
    return (x + .05) / (y + .05)


for name, background, foreground in [
    ("dark text", "DARK_BACKGROUND", "DARK_TEXT"),
    ("dark secondary text", "DARK_BACKGROUND", "DARK_MUTED"),
    ("dark accent", "DARK_BACKGROUND", "DARK_ACCENT"),
    ("light text", "LIGHT_BACKGROUND", "LIGHT_TEXT"),
    ("light secondary text", "LIGHT_BACKGROUND", "LIGHT_MUTED"),
    ("light accent", "LIGHT_BACKGROUND", "LIGHT_ACCENT"),
]:
    check(contrast(color(background), color(foreground)) >= 4.5,
          name + " does not have 4.5:1 contrast")

render = home[home.index(" void render(){"):home.index(" void interruptedRideShortcut(){")]
for marker in ["addHero(b)", "stats(b)", "quickActions()", "rideAction()", "recent(b)", "insights(b)"]:
    check(marker in render, "missing home section " + marker)
order = [render.index(marker) for marker in
         ["addHero(b)", "stats(b)", "rideAction()", "quickNavigation()", "quickActions()", "recent(b)", "insights(b)"]]
check(order == sorted(order), "home order regressed")
check("int photoHeight=displayWidth<a.dp(390)?190:211;" in home, "hero doesn't adapt")
check(home.count("void quickActions(){") == 1, "duplicated shortcut area")
check('new QuickEntryUi(a).fuel()' in home, "quick fuel entry missing")
check('new QuickEntryUi(a).maintenance()' in home, "maintenance shortcut missing")
check('new RideUi(a).requestStart()' in home, "GPS consent request missing")
rideui=(root/'app/src/main/java/com/motohub/next/RideUi.java').read_text()
check('new RideUi(a).finishWithSummary()' in home and 'setPositiveButton("종료"' in home
      and 'RideService.STOP' in rideui and '".done"' in rideui,
      "GPS stop confirmation or verified finish path missing")
check("changeHeroPhoto(bike,own)" in home and "switchBike(direction)" in home,
      "hero photo management or bike switching lost")
check('String[] labels={"홈","라이딩","정비","설정"}' in design,
      "navigation labels inconsistent")
check('setContentDescription(labels[i]+(selected?", 선택됨":""))' in design,
      "navigation accessibility label missing")
check('getWindow().setStatusBarColor(dark?bg:panel)' in main,
      "system chrome doesn't track theme")
check("new GarageHomeUi(a).render()" in hub, "home delegated UI missing")
check('card.setBackground(a.shape(a.panel,a.line,8))' in hub,
      "listing cards have lost separators")
print("PASS UX38 visual invariants:", checks, "checks")
