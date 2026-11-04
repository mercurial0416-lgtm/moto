import fcntl
import json
from pathlib import Path
import sys
import tempfile
import unittest
from dispatch import ANCHOR, INTERVAL, dispatch, slot_at

class DispatcherTest(unittest.TestCase):
    def test_exact_anchored_intervals_across_midnight(self):
        self.assertEqual(slot_at(ANCHOR-1), -1)
        for n in range(100):
            self.assertEqual(slot_at(ANCHOR+n*INTERVAL), n)
            self.assertEqual(slot_at(ANCHOR+(n+1)*INTERVAL-1), n)

    def test_once_per_slot_even_on_failure_and_no_catchup_burst(self):
        with tempfile.TemporaryDirectory() as root:
            command=[sys.executable, '-c', 'raise SystemExit(7)']
            self.assertEqual(dispatch(root, command, now=ANCHOR), 7)
            self.assertEqual(dispatch(root, command, now=ANCHOR+1), 0)
            self.assertEqual(dispatch(root, command, now=ANCHOR+10*INTERVAL), 7)
            self.assertEqual(json.loads((Path(root)/'state.json').read_text())['slot'], 10)

    def test_interval_is_one_hour(self):
        self.assertEqual(INTERVAL, 3600)
        self.assertEqual(slot_at(ANCHOR+3599), 0)
        self.assertEqual(slot_at(ANCHOR+3600), 1)

    def test_legacy_305_minute_state_uses_attempt_time_without_duplicate(self):
        with tempfile.TemporaryDirectory() as root:
            state=Path(root)/'state.json'
            state.write_text(json.dumps({'slot': 1, 'attempted_at': ANCHOR+305*60, 'status': 'finished'}))
            command=[sys.executable, '-c', 'raise SystemExit(7)']
            self.assertEqual(dispatch(root, command, now=ANCHOR+310*60), 0)
            self.assertEqual(dispatch(root, command, now=ANCHOR+360*60), 7)
            self.assertEqual(json.loads(state.read_text())['interval_seconds'], 3600)

    def test_overlap_is_skipped_without_advancing_state(self):
        with tempfile.TemporaryDirectory() as root:
            with (Path(root)/'lock').open('a') as lock:
                fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
                self.assertEqual(dispatch(root, ['missing-command'], now=ANCHOR), 0)
                self.assertFalse((Path(root)/'state.json').exists())

if __name__ == '__main__':
    unittest.main()
