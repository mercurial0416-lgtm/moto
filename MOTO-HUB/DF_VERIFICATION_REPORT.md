# DF(515) verification report

Base: verified DE(514) source from `MOTO-HUB-4.0.0-alpha6-de-514-source.zip`.

## Completed local tests

- `tests/run-df-quality-tests.sh`: 14 archive safety/10K checks, 8 backup preview checks, 7 static UI/version/privacy checks PASS.
- `tests/run-de-quality-tests.sh`: backup integrity, interlock, GPS observed coverage, 8 UI/privacy/version checks PASS.
- `tests/run-backup-tests.sh`: PASS (real archive with GPX, photos and raw ride).
- `tests/run-restore-tests.sh`: 22 rollback/restore assertions PASS.
- Python integration: all 18 scripts PASS.
- A prior attempt to execute every shell script locally exceeded the environment timeout and did not yield an exhaustive result; this is NOT a test pass. The GitHub workflow must run the full regressions.

## GitHub Actions / APK validation

Not yet executed at the time this report was initially authored. Update this section *only* with observed CI results.

## Data and policy preservation

- Existing maintenance/fuel/listings/catalog/GPX/ride/group/backend sources retained.
- OBD2 remains developer-mode only/read-only and does not issue ECU writes.
- Location sharing remains explicit opt-in; backup checks operate on local files only.
- Stable signing and real physical Android/OBD2/Supabase testing remain outstanding.
