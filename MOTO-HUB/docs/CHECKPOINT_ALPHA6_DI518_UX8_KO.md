# MOTO HUB Alpha6 DI518 UX8 — GPS, photos, restore safety

Source base: `build/di518-ux4-prebuild-20261020` including UX7. Same application ID, versionCode 518. This is **source only**. No new signed update APK or manifest is published.

## Code changes
- GPS LocationManager invokes location callbacks on a dedicated `HandlerThread` rather than Android's main/UI looper. Coordinate validation, append/flush/fsync, distance updates and point ordering are serialized; starting/finishing/cleaning a track uses synchronized access so a stop cannot close an actively written file. The service stops its GPS handler thread on destruction and GPS signal-loss watchdog resets are protected against races.
- Only the OBD auto-start/stop decision stays on the UI looper. Read-only OBD telemetry and explicit opt-in remain intact. GPS provider-off callbacks stop location sharing and do not invent distance.
- Automatic Wikimedia Commons fallback rejects an image filename with an explicitly different model year; undated exact-model photos remain eligible but clearly marked as **year unverified**. People can still choose a gallery image explicitly.
- Restore refuses recognized photos, receipts, GPS tracks and `rides` directories that are symlinks or not regular local files **before** it copies, deletes, or invokes the JSON writer. Required disk-space calculations also refuse linked files and handle large integer values without overflowing.
- Regression tests: `PhotoTitleMatcherTest`, `RestoreCapacityTest`, `Ux8RestoreSafetyTest`, updated home/GPS wiring checks, static GPS threading invariants. All pre-existing `tests/run-*.sh` are run by the source-only CI, followed by Android Java compile, lint and JVM unit tests.

## Not possible to verify without access to a real phone
- 20+ minute ride, Android 16 background-foreground transitions, KakaoNavi simultaneously active, sudden GPS loss and return, real ELM327 Bluetooth behavior, display photo provider/camera lifecycle, and low-battery/low-disk stress tests. A JVM simulation is **not** proof these work on hardware.
- Full restore on a disposable Android 16 installation, including process death during restore.
- In-place update from the existing fixed-signed 517 installation to 518: requires private original signing credentials, an actual release APK, apksigner validation of SHA-256 certificate `212c5b39d0082ab4bed78f8da2b719819cd80a20bc116c84f8bdfabca507021b`, matching metadata/hash, on-device data preservation and user review before publication.
- Do **not** uninstall the current app or replace its update manifest with a debug-signed package.

## Next prioritized checks
1. Test GPS throughput and lifecycle on a physical Android 16 phone while KakaoNavi is foregrounded; inspect preserved track records after a forced process death.
2. Capture camera images with several EXIF orientations and provider/storage failures; verify retries never overwrite user-selected model photos.
3. Conduct exact 517→518 signed upgrade rehearsal with full ZIP export and restoration test.
4. Profile 10,000-ride history on actual low-memory hardware; consider database indexing only if device measurements prove the need.
5. Test backup restore with storage exhaustion and corrupted archive, then verify rollback protects every original asset.

CI run URL is tracked in GitHub Actions; **do not call this verified unless the latest run has concluded success**.
