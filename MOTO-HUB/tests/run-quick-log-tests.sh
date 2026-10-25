#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" tests/support/org/json/JSONObject.java \
 app/src/main/java/com/motohub/next/{QuickLogMath,QuickLogDuplicate,QuickServiceHistory,RecentRecordReuse,CostDashboard,RecordMonthlySummary}.java \
 tests/{QuickLogEntryTest,RecordMonthlySummaryTest}.java
java -cp "$out" com.motohub.next.QuickLogEntryTest
java -cp "$out" com.motohub.next.RecordMonthlySummaryTest
python3 tests/test_quick_log_ui.py
