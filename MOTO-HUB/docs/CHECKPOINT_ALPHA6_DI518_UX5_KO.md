# MOTO HUB Alpha6-DI518 UX5 — record reliability patch (source only)

Base: build/di518-ux4-prebuild-20261020; versionCode remains 518. **No APK compiled, signed or published by this patch.**

## Changes
- MY GARAGE maintenance reminders choose the newest record by date then odometer per vehicle and service type; an old due date no longer overrides a more recent completed job.
- Duplicate detection treats two blank record IDs as potentially duplicate while excluding edits to the same nonblank ID; null/incomplete/nonfinite input cannot generate false warnings.
- Offline receipt OCR recognizes Korean **승인금액**, **결제합계**, **주유수량** and **2026년 10월 20일** date format, prioritizing final paid amount over subtotal; OCR suggestions remain explicitly confirmed by the user.
- Regression tests cover unsorted service histories, vehicle isolation, zero-cost DIY jobs, blank IDs, malformed numbers and Korean receipt text.

## Verify
```bash
cd MOTO-HUB
bash tests/run-ux5-record-tests.sh
./gradlew :app:compileDebugJavaWithJavac --no-daemon
```
The branch's modified `.github/workflows/di518-ux4-compile-only.yml` runs these checks without invoking assembleDebug/assembleRelease.

## Remaining constraints
- Android 16 device installation, photo camera capture, GPS/OBD2 running in the background, production group sharing, UX gestures and 10k-record UI performance still require device/end-to-end verification.
- To make a distributable update, build later with the original fixed signing certificate, validate it against the existing installation, and publish a new manifest only with a verified APK.
- Never replace the 517 manifest with a 518 source-only checkpoint; do not claim this branch is a downloadable update.
