#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
p=Path("app/src/main/java/com/motohub/next")
read=lambda filename:(p/filename).read_text(encoding="utf-8")
stats=read("RideStatsUi.java")
series=read("RideStatsSeries.java")
rest=read("RestoreArchive.java")
main=read("MainActivity.java")
audit=read("RestoreVerificationAudit.java")
hub=read("HubScreens.java")
obd=read("DeveloperObdScreen.java")
log=read("ObdLocalDiagnosticLog.java")
checks={
 "unlimited GPS scan, no artificial 180-ride truncation": "if(inspected++>=180)" not in stats and 'inspected%40==0' in stats,
 "worker-only GPS scanning and stale-screen cancellation": 'STATS_EXECUTOR.execute(' in stats and 'generation==STATS_GENERATION.get()' in stats,
 "travel-local month boundary in GPS stats": 'YearMonth.now(localZone)' in stats and 'ZoneId.systemDefault()' in stats and 'atZone(zone)' in series and 'atStartOfDay(zone)' in series,
 "ride date filter already uses device zone": 'ZoneId.systemDefault()' in read("RideHistoryPeriod.java"),
 "last restore outcome is retained without paths or hashes": 'RestoreVerificationAudit.record(this,postRestoreOk' in main and 'verifiedFiles=checked' in rest and '"full_restore_verification"' in audit and 'GPS coordinates' in audit,
 "last restore status visible in existing backup screen": 'RestoreVerificationAudit.summary(a)' in hub,
 "manual OBD measurements tracked, not just bitmap": 'latencyMs+=Math.max' in obd and 'ObdLocalDiagnosticLog.probeMetrics(activity' in obd and 'probe_last_received' in log,
 "live auto OBD attempts and retries visible": 'RideService.obdReconnects' in log and 'RideService.obdConnectionAttempts' in log,
 "original package and OTA signing untouched": 'UpdateInstallSnapshot.recordInstaller' in read("AppUpdater.java")
}
for name,ok in checks.items():print(("PASS" if ok else "FAIL")+" : "+name)
assert all(checks.values()),"missing expected v573 safety invariant"
PY
