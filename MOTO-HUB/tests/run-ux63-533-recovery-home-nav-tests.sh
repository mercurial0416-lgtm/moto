#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
p=Path("app/src/main/java/com/motohub/next")
a=(p/"MainActivity.java").read_text()
g=(p/"GarageHomeUi.java").read_text()
n=(p/"NavigationApps.java").read_text()
assert 'RideRecoveryPromptPolicy.shouldOffer(' in a
assert 'putString("snoozed",filename)' in a
assert 'putString("reviewed",filename)' in a
assert 'putString("seen",filename)' not in a
assert 'showInterruptedRidePrompt()' in a
assert 'if(recording||armed){' in g
assert 'if(!recording&&!armed){' in g and '원터치 출발 · GPS + 기본 내비' in g
assert 'new RideUi(a).requestStart(true)' in g
assert '다른 내비 실행' in n
assert 'chooseInstalledAndLaunch(MainActivity activity)' in n
assert 'NavigationChoice.values()' in n and 'MotoWidgetProvider.refresh(activity)' in n
assert 'installedLaunchIntent(activity,option)==null' in n
print("PASS UX63 533: interrupted GPS snooze, simple home actions, installed navigator fallback")
PY
