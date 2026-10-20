# MOTO HUB Alpha6 DI518 UX9 — Last-mile reliability checkpoint

**Base:** `build/di518-ux4-prebuild-20261020` after UX8. UX9 is a source-only change, Android versionCode remains 518. **No APK has been built or published by this change.** Existing fixed-signed 517 update manifest is unchanged.

## Implemented and targeted by regression
1. Manual GPS STOP now revokes any active group location sharing immediately, stops new GPS callbacks and **queues the full-track `TrackCompletion.mark` verification on the ordered GPS I/O worker** instead of doing an up-to-25MB scan on the main UI thread. The service stays running until verification has settled. The original GPS file remains readable for recovery after failures.
2. OBD2 automatic ride STOP uses the same ordered worker; the main-thread autoGate cannot rearm during verification. A user STOP while auto-finalization is pending also stops the OBD session without double-closing its GPS track. The user-stop path remains distinct from the AUTO_DISARM button.
3. Destroy/error cleanup invalidates any queued completion status callbacks so an old completion cannot change state after shutdown. The `.done` marker is written only after committed GPS sample counts and final newline are checked, and invalid/truncated files keep their original bytes for recovery.
4. The ride screen shows `SAVING RIDE`, disables duplicate stop buttons during verification, and refreshes itself once the worker finishes without requiring the user to navigate away.
5. Model-photo galleries release full-size downloaded bitmap memory immediately after saving, including failures and cancelation; unused preview bitmaps are freed when their result has no visible Activity.
6. Legacy cached automatic motorcycle photos that explicitly name the wrong model or model year are not shown as the registered bike. A deliberately selected photo always wins; offline old photos without label metadata are preserved but not year-certified.

## Validation
- `MOTO-HUB/tests/run-ux9-last-mile-tests.sh`: photo-cache policy cases, completion-marker failure cases, async UI/save/privacy wiring. Legacy regression scripts are run as part of all-scripts CI.
- `.github/workflows/di518-ux4-compile-only.yml`: all `tests/run-*.sh`, `:app:compileDebugJavaWithJavac`, `:app:lintDebug`, `:app:testDebugUnitTest`, no APK creation. **The latest GitHub Actions run must have a success conclusion before claiming complete verification.**
- Earlier UX8 final base CI passed: `39424038843`. UX9 is a new code change, not verified by that earlier run.

## Still not validated or publishable without a real phone/private signing identity
- Android 16: long rides with screen off; KakaoNavi foreground; OBD2 movement and Bluetooth disconnections; rapid double STOP and process kill during an asynchronous finalize; GPS/provider and permissions revoked mid-ride; low battery / storage full; corrupt partial GPS files; model gallery under network loss; UI frame timing.
- Full backup/export/restore and in-place 517→518 install rehearsal using the same fixed certificate SHA-256: `212c5b39d0082ab4bed78f8da2b719819cd80a20bc116c84f8bdfabca507021b`.
- Release only from the guarded original-certificate workflow after package ID, cert, version, APK hash and phone-side data retention all verify. Never replace the public 517 manifest with a debug-signed build, and never uninstall the currently installed app as a shortcut to test an upgrade.

## Remaining work rather than invented completion
- On-device performance profiling at 10k ride histories and low memory.
- Hardware regression against the different camera content providers and all EXIF rotations.
- Foreground GPS lifecycle across airplane mode/background battery restrictions.
- End-to-end persistent data roundtrip with version 517 and one final production-signed 518 APK.
