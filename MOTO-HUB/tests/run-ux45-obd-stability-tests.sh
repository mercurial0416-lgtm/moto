#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
temp="$(mktemp -d)"
trap 'rm -rf "$temp"' EXIT
javac -encoding UTF-8 -d "$temp" \
 app/src/main/java/com/motohub/next/ObdPidCapabilities.java \
 app/src/main/java/com/motohub/next/ObdRideAutoGate.java \
 app/src/main/java/com/motohub/next/ObdSpeedSanityPolicy.java \
 app/src/main/java/com/motohub/next/ObdGaugeWarnings.java \
 app/src/main/java/com/motohub/next/ObdDiagnosticEventPolicy.java \
 tests/ObdSpeedSanityPolicyTest.java \
 tests/ObdGaugeWarningsTest.java \
 tests/ObdDiagnosticEventPolicyTest.java \
 tests/ObdRideAutoStartFreshnessTest.java \
 tests/ObdRideAutoGateTest.java
for klass in ObdSpeedSanityPolicyTest ObdGaugeWarningsTest ObdDiagnosticEventPolicyTest ObdRideAutoStartFreshnessTest ObdRideAutoGateTest;do
 java -cp "$temp" "com.motohub.next.$klass"
done
python3 - <<'PY'
from pathlib import Path
src=Path("app/src/main/java/com/motohub/next")
read=lambda name:(src/name).read_text(encoding="utf-8")
service=read("RideService.java"); dev=read("DeveloperObdScreen.java")
home=read("RideUi.java"); dash=read("ObdDashboardActivity.java")
journal=read("ObdLocalDiagnosticLog.java"); policy=read("ObdDiagnosticEventPolicy.java")
checks={
 "one-sample speed filtering":'speedSanity.accept(kmh,elapsed)' in service,
 "sanity rejection resets auto start":'autoGate.connectionLost();obdLastError=' in service,
 "fresh samples guarded":'recording?15_000:5_500' in read("ObdRideAutoGate.java"),
 "disconnect resets speed filter":'speedSanity.reset();ObdLocalDiagnosticLog.record(this,"AUTO_OFF")' in service,
 "sensor state persisted locally":'ObdLocalDiagnosticLog.capability(RideService.this,capability)' in service,
 "manual probe saves verified capabilities":'ObdLocalDiagnosticLog.capability(activity,capabilities)' in dev,
 "user controlled local history":'지난 ECU 지원 항목 · 연결 테스트 기록' in dev and 'ObdLocalDiagnosticLog.clear(activity)' in dev,
 "no MAC/GPS persistent metadata":'getAddress()' not in journal and 'getLatitude()' not in journal,
 "local allowlisted events":'if(!ObdDiagnosticEventPolicy.valid(code))return;' in journal,
 "bounded event history":'MAX_ENTRIES=12' in journal,
 "manual battery guidance":'장기 주차 시 동글 분리' in dev,
 "OBD status stays in advanced dashboard":'RideService.obdLink' in dash and 'OBD2 데이터를 읽는 중' not in home,
 "missing speed is clearly labeled":'ObdTelemetryFreshness.speedFresh(' in dash or 'ObdSignalAge.label(' in dash,
 "digital dash scroll":'ScrollView scroll=new ScrollView(this)' in dash,
 "digital dash RPM meter":'rpmMeter.setMax(15000)' in dash,
 "stale data never drives warning":'rpmFresh?RideService.obdRpm:Double.NaN' in dash,
 "dashboard advisory reads":'ObdGaugeWarnings.advisory(' in dash,
 "privacy policy excludes identifiers":'GPS 37.' not in policy,
 "do not auto-edit ODO":'target.put("odo"' not in service,
 "GPS ride still user opted":'getSharedPreferences("developer_experiments",0).getBoolean("enabled",false)' in dev,
}
for label,valid in checks.items():assert valid,label
print(f"PASS UX45 OBD safety/source guards: {len(checks)}")
PY
