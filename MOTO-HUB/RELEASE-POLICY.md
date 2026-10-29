# MOTO HUB fixed-certificate release policy

- Changes pass the Android build, lint, unit tests, and regression suite before merging to `release/moto-hub-apk`.
- The numeric `MOTO-HUB/release-code.txt` is advanced to the next Android versionCode with the release source PR.
- `.github/workflows/moto-publish-signed-on-version.yml` listens for version-file updates on that integrated branch, or can be dispatched manually. **Push publishing defaults OFF** until the owner configures both the fixed signing credentials and repository variable `MOTO_AUTO_PUBLISH=true`.
- Credentials remain **private GitHub Actions secrets**: `MOTO_SIGNING_STORE_B64`, `MOTO_SIGNING_STORE_PASSWORD`, `MOTO_SIGNING_KEY_ALIAS`, `MOTO_SIGNING_KEY_PASSWORD`. The keystore file and any password must **never** be committed to GitHub, embedded in an APK, added to public artifacts, or sent in a chat.
- Build uses the established package `com.motohub.next`, verifies Android APK Signature Scheme using official `apksigner` and certificate pin `212c5b39d0082ab4bed78f8da2b719819cd80a20bc116c84f8bdfabca507021b`, verifies versionCode, ZIP structure and SHA-256 before publishing.
- GitHub Release assets are immutable by release policy. The online `updates/manifest.json` is written only after verified APK publication.
- After a stabilized release, **freeze elective feature additions**. Next work must be based on a reproducible bug, data-loss risk, broken update, observed usability issue or an explicit new user request. Never invent a new version solely because more features could be imagined.
- If Actions private secrets are absent, leave the current manifest unchanged. A locally fixed-signed APK can still be published through a one-off audited public-signature block workflow after a separate official `apksigner` verification. This is not equivalent to fully unattended private-key signing.
