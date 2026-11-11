# MOTO HUB v601 pre-release safety
This branch produces an isolated preview APK (package `com.motohub.next.obdpreview601`) and an unsigned fixed-package release build. Neither can overwrite the installed production app.

The official `com.motohub.next` update can **only** be released through the repository's fixed-signer workflow after the release source has passed CI and the APK certificate matches the pinned SHA-256 identity. The public `updates/manifest.json` MUST NOT be changed to point to debug, preview or unsigned output. Existing GPS/ODO data must not be deleted to test installation.

Behavioral caveats: Cardo GPS-only requires explicit per-bike fallback consent; Android may block background launches; earlier GPS fixes are replayed only when genuinely observed and still fresh; recovering a killed recording requires the user to tap resume within 15 minutes and leaves the interrupted original untouched; device battery percentage is a whole-phone observation, not per-app power measurement. Duke 390 ECU ODO capability remains hardware dependent.
