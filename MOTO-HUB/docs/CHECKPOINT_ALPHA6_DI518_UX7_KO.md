# MOTO HUB DI518 UX7 — Photo & GPS resilience

Base: `build/di518-ux4-prebuild-20261020` after UX6. Source-only patch, versionCode 518 unchanged. Android APK and live update manifest intentionally remain unchanged.

## Implemented
- Garage image decoding, JPEG conversion and disk writes now run in a single background executor, in document-picker arrival order. Bind each image to the selected garage motorcycle ID at picker return, rather than a mutable current selection. A canceled document selection discards its pending target ID.
- Gallery JPEGs honor EXIF orientation. `AtomicFile` staging and rollback, and existing personal-photos-first display policy, remain in place. Only post-save toast and render run on the UI thread.
- On-device receipt OCR cancellation now invokes the failure callback when its Activity is finishing/destroyed, allowing its owner to delete a private temporary camera capture. OCR never saves its suggestions without approval.
- Background model-photo searches recycle downloaded bitmaps when a view is dismissed, the Activity is destroyed or a newer photo request supersedes the result.
- GPS listing, parsing and abandoned empty-session cleanup refuse symbolic links; the `rides` storage directory may not be redirected outside app-private storage. Completed and interrupted real track files remain untouched unless the user explicitly acts.
- New regression script `tests/run-ux7-safety-tests.sh` tests ordinary GPS tracks, intentionally linked track/directory inputs, photo-selection wiring, background saves, EXIF, OCR cleanup and canceled photo image disposal.
- CI: all source regression scripts, Android Java compilation, Android lint, JVM unit tests and explicit no-APK output check.

## Still requires physical device and private signing credentials
- Samsung / Android 16: gallery chooser lifecycle and orientation, unsupported photo URIs, app rotation while the camera/OCR is active, low-storage writes, background GPS with screen off/KakaoNavi, Bluetooth OBD pairing and permission revocation.
- Live update: SHA-256 pinned **517** release is the existing installed checkpoint. Do not publish the 518 source branch as a new update until it is built using the same fixed signing certificate (SHA256 `212c5b39d0082ab4bed78f8da2b719819cd80a20bc116c84f8bdfabca507021b`) and in-place upgrade + data-preservation tests succeed. Debug-signed builds are **not** safe in-place replacements.
- For app data migration: first make a complete full ZIP export and verify it. Do not uninstall the currently installed app to test a signing mismatch.

## Next highest-value candidate changes
- Reliable queued / bounded GPS writes on a dedicated disk worker without sacrificing last-line durability on stop or crash; requires careful ordering and device stress testing.
- Detect image-provider failures and surface retry with an explicit source preview and year-confidence message.
- Restore testing with corrupt ZIP/limited disk space on real Android 16 hardware.
- End-to-end signed upgrade path from version 517 to 518, with rollback plan.
