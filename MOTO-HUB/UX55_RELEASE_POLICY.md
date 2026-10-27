# MOTO HUB app-only updater — UX55/525 and later
Distribution is APK sideload; Google Play is not used. **Never distribute an unsigned/debug APK to an installed user**.
The public `main/updates/manifest.json` is the only update signal used by the app.

## Secure release
1. Merge verified app code into `release/moto-hub-apk`.
2. `moto-verified-unsigned-prebuild.yml` runs regressions, lint, and unit tests, and uploads a source-commit-specific unsigned APK.
3. Privately sign that exact APK with the original fixed key. Never commit private keys or passwords.
4. Commit only an offline-signed APK v2 public-signature block and SHA-256 manifest under `signed-artifacts/v<version>-signing-block.json`, with the source workflow run and commit IDs. These are public signatures, not signing credentials.
5. `moto-offline-versioned-publish.yml` verifies the byte-for-byte unsigned input and signed output, exact app ID/version, pinned certificate, APK signature, and then publishes the GitHub Release asset.
6. Only after publishing the asset should the workflow update `main/updates/manifest.json` (HTTPS URL, size and SHA-256).
7. The installed app prompts users to update and invokes Android's system install confirmation. It never auto-uninstalls or replaces private data.

This workflow intentionally requires access to the existing private signing key **off GitHub** at each release. A private signature cannot be generated from public code alone, and no fully unattended GitHub signing is promised without secure Actions secrets.

Test signing and upgrade compatibility on a physical device before relying on distribution. Never silently enable OBD/ECU write actions.
