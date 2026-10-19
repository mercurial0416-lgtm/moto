# MOTO HUB Alpha6-DG (516) — restore storage safety

Based on verified DF(515) checkpoint; all existing ride, repair, listing, GPX, social and read-only OBD features are preserved.

## Changes

- Before applying a selected whole-ZIP restore, reserve room for **both** a complete copy of existing local photos/GPS sidecars and a copy of incoming staged files; add previous JSON recovery bytes and a fixed 8MiB safety margin. Reject low space **before** changing the prior `before-import.json` recovery point or any ride data.
- Restore UI previews estimated required space and currently available bytes. The final install checks actual space again; it does not trust the earlier preview.
- Checked summation rejects unavailable files, invalid lengths and arithmetic overflow.
- Add executable `RestoreCapacityTest` (11 assertion checks) plus restore preflight UI/permission wiring checks in `run-dg-quality-tests.sh`; add new class to all standalone tests which compile RestoreArchive.
- Synchronize version 516 build metadata and GitHub CI artifact names. No signing private key has been generated or recovered.

## Limitations

- Estimating free space is conservative but cannot guarantee writes when the filesystem loses free space concurrently. Existing copy+rollback error handling remains in place.
- Current debug signing identity may differ from DF 515: don't uninstall the old app without verifying off-device full ZIP backup.
- Real-device GPS/OBD/Supabase tests and encrypted ZIP export remain unverified/unimplemented.
