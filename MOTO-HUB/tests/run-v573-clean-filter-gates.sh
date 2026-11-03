#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
p=Path("app/src/main/java/com/motohub/next")
ride=(p/"RideUi.java").read_text()
stats=(p/"RideStatsUi.java").read_text()
filters=(p/"RideCategoryFilter.java").read_text()
assert 'putString("category_filter",RideCategoryFilter.OPTIONS[index])' in ride
assert 'getString("category_filter",RideCategoryFilter.ALL)' in ride
assert 'rideHistoryQuery=index==0' not in ride, "Category chooser must not overwrite text search"
assert 'RideCategoryFilter.matches(categoryRange,RideCategory.read(file))' in ride
assert ride.index('int unfinished=0;') < ride.index('RideCategoryFilter.matches(categoryRange'), "Interrupted GPS recovery prompt must survive category filtering"
assert 'putBoolean("advanced_expanded",expand)' in ride
assert 'getBoolean("advanced_expanded",false)' in ride
assert 'filterToggle.setContentDescription(' in ride
assert 'data.inDisplayedPeriod(t,now)' in stats
assert 'new File(ride.getPath()+".done").isFile()' in stats
assert 'category equals' not in filters
print("PASS v573 category, recovery, filter, and calendar/cost source safeguards")
PY
