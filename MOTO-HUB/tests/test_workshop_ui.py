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

# Modern layout regression: keep the same actions, not the old 211dp hero/card stack.
render = home[home.index(" void render(){"):home.index(" /** Scan local GPS metadata")]
for marker in [
    "compactVehicleHeader(bike)", "stats(bike)", "HomeShortcutOrder.parse(",
    "rideAction()", "homeShortcutTile(key)", "recent(bike)", "insights(bike)",
    "interruptedRideShortcut()", "showQuickTools(bike)"
]:
    check(marker in render, "missing modern home action " + marker)
check(render.index("compactVehicleHeader(bike)") < render.index("stats(bike)") <
      render.index("HomeShortcutOrder.parse(") < render.index("recent(bike)"),
      "vehicle, status, shortcuts and latest activity should have clear hierarchy")
check('row.addView(thumb,new LinearLayout.LayoutParams(a.dp(78),a.dp(70)))' in home,
      "vehicle thumbnail should not monopolize the viewport")
check('void homeShortcutTile' not in home and 'LinearLayout homeShortcutTile(String key)' in home,
      "compact icon shortcut builder missing")
check('new QuickEntryUi(a).fuel()' in home and
      'new QuickEntryUi(a).maintenance()' in home,
      "existing fuel and maintenance input actions must remain wired")
rideui=(JAVA/'RideUi.java').read_text()
check('new RideUi(a).requestStart(true)' in home,
      "explicit GPS consent/navigation action missing")
check('setPositiveButton("저장하고 종료"' in rideui and
      'void finishWithSummary()' in rideui and 'RideService.STOP' in rideui,
      "confirmed GPS stop and verified save must remain available")
check('changeHeroPhoto(bike,owned)' in home and 'switchBike(direction)' in home,
      "photo editing or multi-bike switch implementation lost")
check('String[] labels={"홈","라이딩","정비","설정"}' in design,
      "navigation labels inconsistent")
check('setContentDescription(labels[i]+(selected?", 현재 탭":" 탭 열기"))' in design,
      "navigation accessibility label missing")
check('getWindow().setStatusBarColor(dark?bg:panel)' in main,
      "system chrome doesn't track theme")
check("new GarageHomeUi(a).render()" in hub, "home delegated UI missing")
check('void listings(){' not in hub and 'void compare(){' not in hub,
      "retired listing rendering should not reappear")
print("PASS UX38 visual invariants:", checks, "checks")
