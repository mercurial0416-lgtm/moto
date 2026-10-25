# UX47 · release readiness and integration

## Why
Prior release workflow references a stale source branch instead of the latest integrated main commit. This integration branch replaces it with the triggering `github.sha`, compares that SHA to the checkout, and validates UX38–UX46 before invoking secrets/signing.

## CI and source status
- Review PR from `integration/ux47-fixed-signed-readiness-20261025` into `main` includes the stacked UX38–UX46 source changes.
- The debug CI validates all local policy tests, Java unit tests, Android compile and lint.
- Signed APK generation still requires the **private original signing keystore** via GitHub Actions secrets and same certificate digest.
- Exact signed source revision, version code, certificate SHA-256 and APK SHA-256 are archived.
- Public update manifest is changed only for an explicit `workflow_dispatch` with `publish_release=true` on `main`.
- Never distribute or install a different-signed debug APK over an existing installation. Do not uninstall the user app to bypass signer mismatch.
- No emulator/physical phone installation or KTM 2024 ECU data has been verified. These remain hard release-readiness limits.

## Release checklist
1. Code review UX38–UX47, check PR CI and merge integration to main only after approval.
2. Confirm current installed app certificate matches expected SHA-256 in release workflow.
3. Run release workflow with publish disabled, verify signed artifact + upgrade version and update in-place on test phone with backed-up data.
4. Conduct Vgate iCar Pro BT3.0 + Euro5 6-pin Duke 390 real-bike tests (paired device, ECU sensors, stable PID speed, GPS ride auto-start/stop, parked battery current).
5. Explicitly publish signed release and public update manifest after tests.
