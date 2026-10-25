# MOTO HUB Alpha6-AK
- Added optional release APK signing via environment variables; secrets are not embedded in source.
- Partial signing configuration now fails fast instead of silently creating an unsigned release.
- Added GitHub Actions signed-release workflow template and APK signature verification step.
- Raised versionCode 441 to 442; package ID unchanged.
- Existing installed APK signature cannot be reproduced without its original signing key. Do not install a differently signed APK as an update.
- No Android APK build or device test performed for this checkpoint.
