#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" \
 app/src/main/java/com/motohub/next/ObdPidCapabilities.java \
 app/src/main/java/com/motohub/next/ObdFirstConnectionGuide.java \
 tests/ObdFirstConnectionGuideTest.java
java -cp "$out" com.motohub.next.ObdFirstConnectionGuideTest
python3 - <<'PY'
from pathlib import Path
p=Path("app/src/main/java/com/motohub/next")
screen=(p/"DeveloperObdScreen.java").read_text()
ride=(p/"RideUi.java").read_text()
main=(p/"MainActivity.java").read_text()
reader=(p/"ObdAutoSpeedMonitor.java").read_text()
tests={
 "SPP RFCOMM only":'device.createRfcommSocketToServiceRecord(SPP)' in screen and 'device.createRfcommSocketToServiceRecord(SPP)' in reader,
 "physical first connect steps":'첫 연결 · ① 진단 케이블' in screen,
 "Bluetooth settings shortcut":'Settings.ACTION_BLUETOOTH_SETTINGS' in screen,
 "first test easy action":'페어링된 장치로 센서 연결 테스트' in screen,
 "no paired device guidance":'OBD2 동글 먼저 페어링' in screen,
 "OBD candidate rank":'ObdFirstConnectionGuide.adapterPriority(device.getName())' in screen,
 "sensor results not inferred":'ObdFirstConnectionGuide.result(' in screen,
 "multiple transient sensor timeouts tolerated":'catch(Exception temporary)' in screen and 'noDataCycles>=3' in screen,
 "read only":'ObdReadOnlyProtocol.query(pid)' in screen and 'ObdReadOnlyProtocol.query("0100")' in screen,
 "normal ride page entry":'OBD2 동글 연결 · ECU 센서 테스트' in ride,
 "experimental opt in":'getSharedPreferences("developer_experiments",0).getBoolean("enabled",false)' in ride,
 "ODO permission callback":'DeveloperObdScreen.ODO_CONNECT_PERMISSION_REQUEST' in main and 'developerObd.loadPairedOdo()' in main,
 "no boot reconnection":'App.onCreate()' not in screen and 'onBoot' not in screen,
 "ride auto start 7 km/h":'START_KMH=7' in (p/"ObdRideAutoGate.java").read_text(),
 "no ECU writes":'모든 차량' not in screen and 'setPid(' not in screen,
}
for description,passed in tests.items():
 assert passed,description
print(f"PASS UX44 OBD source guards: {len(tests)}")
PY
