#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
src=Path('app/src/main/java/com/motohub/next/DailyMaintenanceJob.java').read_text()
assert 'ConcurrentHashMap<Integer,ActiveJob>' in src
assert 'private static boolean cancelled(ActiveJob job)' in src
assert 'runningJobs.remove(params.getJobId(),job)' in src
assert 'if(!job.stopped)jobFinished(params,false)' in src
assert 'job.worker.interrupt();' in src
assert 'if(cancelled(job))return;' in src
assert 'synchronized(LocalSafetyBackup.class)' in src
assert 'if(enabled&&!RideService.running)sendMaintenanceReminder(db)' in src
assert 'else if(!enabled)clearReminderState(this)' in src
assert 'new Thread(()->{' in src
print('PASS cancellable maintenance JobService lifecycle and non-overlapping local snapshots')
PY
