#!/usr/bin/env python3
"""One anchored hourly attempt per slot, invoked by an external minute timer.
No daemon is installed by this file. State must be on persistent local storage.
"""
import argparse
from datetime import datetime
import fcntl
import json
from pathlib import Path
import subprocess
import time
from zoneinfo import ZoneInfo

ANCHOR = int(datetime.fromisoformat('2026-11-05T00:27:37+09:00').timestamp())
INTERVAL = 60 * 60

def slot_at(now):
    return (now - ANCHOR) // INTERVAL if now >= ANCHOR else -1

def dispatch(state_dir, runner, now=None, dry_run=False):
    now = int(time.time()) if now is None else now
    slot = slot_at(now)
    if dry_run:
        due = ANCHOR + max(0, slot) * INTERVAL
        print(json.dumps({'slot': slot, 'due': datetime.fromtimestamp(due, ZoneInfo('Asia/Seoul')).isoformat(), 'next': datetime.fromtimestamp(due + INTERVAL, ZoneInfo('Asia/Seoul')).isoformat()}))
        return 0
    state_dir = Path(state_dir)
    state_dir.mkdir(parents=True, exist_ok=True)
    with (state_dir / 'lock').open('a') as lock:
        try:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            return 0  # A prior execution still owns the workspace.
        state = state_dir / 'state.json'
        previous = json.loads(state.read_text()) if state.exists() else {}
        # Compare attempt timestamps, so switching the old 305-minute schedule
        # to hourly cannot reuse an incompatible persisted slot number.
        last_attempt = previous.get('attempted_at')
        previous_slot = slot_at(last_attempt) if isinstance(last_attempt, int) else previous.get('slot', -1)
        if slot < 0 or slot <= previous_slot:
            return 0
        # Record the attempt before launching: crashes/quota failures cannot loop.
        record = {'slot': slot, 'attempted_at': now, 'status': 'attempted', 'interval_seconds': INTERVAL}
        temp = state_dir / 'state.tmp'
        temp.write_text(json.dumps(record))
        temp.replace(state)
        try:
            result = subprocess.run(runner, check=False).returncode
        except OSError:
            result = 127
        record.update(status='finished', exit_code=result)
        temp.write_text(json.dumps(record))
        temp.replace(state)
        return result

if __name__ == '__main__':
    p = argparse.ArgumentParser()
    p.add_argument('--state-dir', default='/workspace/onboarding/scheduler-state')
    p.add_argument('--dry-run', action='store_true')
    p.add_argument('runner', nargs=argparse.REMAINDER)
    args = p.parse_args()
    if not args.dry_run and not args.runner:
        p.error('an authenticated runner executable is required')
    raise SystemExit(dispatch(args.state_dir, args.runner, dry_run=args.dry_run))
