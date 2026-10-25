# Alpha6-DE (514) verification

Sources: latest `/MOTO-HUB/checkpoints/MOTO-HUB-4.0.0-alpha6-dd-513-source.zip` from 2026-10-19.

## Direct local verification

- `tests/run-de-quality-tests.sh`: PASS, 11 backup-integrity checks, 16 backup/ride/update interlock checks, 5 GPS coverage checks, 8 static source wiring checks.
- `tests/run-dd-quality-tests.sh`: PASS, legacy GPS crash-recovery tests, 24-hour simulated location processing (17,281 fixes), 10,000 synthetic local ride indexes (not physical-device results).
- `tests/run-backup-tests.sh`, `tests/run-restore-tests.sh`, `tests/run-ch-restore-gate-tests.sh`, `tests/run-db-data-safety-tests.sh`, `tests/run-ride-tests.sh`, `tests/run-ride-consent-tests.sh`, `tests/run-obd-readonly-tests.sh`, `tests/run-obd-auto-ride-tests.sh`, `tests/run-cz-history-scale-tests.sh`: PASS.
- `tests/test_alpha6_*.py` and `tests/test_catalog.py`: 18/18 scripts passed.
- No real device was connected for Android system/location/OBD2 tests.

## CI

Android compile, lint, unit tests, APK signing verification: not yet run for DE(514) when this document was generated; consult GitHub Actions run record once available. This report must not be read as claiming them successful.
