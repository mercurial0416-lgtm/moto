# MOTO HUB Alpha6 DI518 UX6 — UI, offline records, release safety

Date: 2026-10-20. Base: DI518 UX5 source branch, versionCode 518.

## Applied code changes
1. Android update unit-test fixture now includes the required pinned certificate SHA-256; verifies wrong-signer rejection. Old fixture caused six test failures even though release verifier was correct.
2. MY GARAGE, notification dialog, and maintenance cost dashboard use the same newest-service-per-type selection, so completed maintenance is not counted again as overdue from old entries.
3. Motorcycle model-photo lookup deduplicates identical Wikimedia Commons URLs so repeated results cannot waste all three fallback download attempts.
4. Backup ZIP preflight rejects symlinked GPS directories, photo and receipt files, GPS track files and suspicious sidecars, and saturates byte totals; a successful preflight is still **not** proof that the exported ZIP has been restored.
5. Unknown, negative, or out-of-protocol ECU speeds use a conservative OBD2 polling interval; supported moving readings stay responsive. ECU access remains read-only, explicitly opted in via developer settings.
6. Listing search computes the normalized search string once, rather than for every record; previous on-screen pagination and user-selected sort are preserved.
7. Regression tests cover the updated signing fixture, maintenance UI wiring, symlinked backup input, and conservative OBD behavior.
8. A CI job runs all pure-Java regression scripts, Android Java compilation, lint, and JVM unit tests **without creating an APK**.

## Boundaries (not verified just by writing source)
- Galaxy S25 / Android 16 hardware: camera capture/rotation/permissions, photo gallery loading and offline/online fallback, GPS background restrictions, ride continuity while screen off, Bluetooth ELM327 reconnect and OBD PID support, actual navigation handoff, accessibility and performance.
- Existing installations: a 518 update can only be published using the same fixed Android signing certificate, SHA-256 `212c5b39d0082ab4bed78f8da2b719819cd80a20bc116c84f8bdfabca507021b`. A random debug-key APK is **not** an upgrade and should never be offered as one.
- Existing signed live update manifest remains 517 until a real 518 release APK, hash, signature and preservation of old data are all verified.
- No account/login, location sharing, GPX, read-only OBD and other existing features were intentionally removed.

## Practical device acceptance checklist
1. Export a complete ZIP and confirm it opens; record the installed version/certificate and last backup path. Do **not** uninstall to test an unsigned upgrade.
2. Add two motorcycles, select photos for each, switch repeatedly, restart, and inspect whether each model/year and photo remains correctly associated.
3. Add an older overdue oil change then a newer completed oil change; home, alerts, and cost dashboard should show the new schedule consistently.
4. Test a 15–30 minute **normal** ride with the screen locked, KakaoNavi foreground, temporary GPS loss and app UI reopened. The route must not invent missing distance or share any location by default.
5. Connect one supported paired ELM327 adapter while stationary, start/stop explicit auto mode, and verify unsupported/missing PIDs are marked absent. Do not test controls while riding.
6. Export and independently verify a new ZIP with GPS and receipt photos. Confirm restoration first on a *separate* disposable test installation with equivalent signing, not on the only copy of the data.
7. Build a fixed-signed release in protected CI, validate `apksigner` certificate and update metadata hash, then upgrade in place and verify records/photos/GPS remain.

## Verification
Branch-specific CI lives at `.github/workflows/di518-ux4-compile-only.yml` and invokes all `tests/run-*.sh`, `:app:compileDebugJavaWithJavac`, `:app:lintDebug` and `:app:testDebugUnitTest`; no assemble task. See the latest GitHub Actions conclusion for the actual passing/failing status; this document does not claim hardware testing.
