# Alpha6-DD (513) — recovery safety, analysis and verification

- Interrupted GPS repair now retains a byte-exact local temporary original until the trimmed track and recovered marker both succeed. Failure during marker creation/sync rolls back the original. If restoring the original fails, it preserves the hidden backup and reports its location; it never silently destroys it.
- Added fault-injection regression for marker creation failure and data-preserving rollback.
- Ride analytics now distinguishes total elapsed average speed from GPS-observed moving average. Non-moving/unobserved time is explicitly **not** mislabeled as true stopped time.
- Added generated 10,000-ride filesystem/index benchmark with byte-for-byte preservation checks.
- Existing OBD2 developer-only/read-only behavior and explicit group sharing consent unchanged.
- Signing: no private key is provided. A guaranteed install-over-update APK requires the device's original signing key, which cannot be reconstructed from a signed APK. Existing CI release flow fails closed if signing secrets are absent.

- Added pure Java 24-hour GPS simulation (17,281 append-only samples) and monotonic OBD sample-expiration assertions. This does NOT simulate real Android battery management, GPS receivers or motorcycles.
- Added on-device read-only signing certificate SHA-256 copy action in Settings so users can compare installer compatibility without exposing private keys.
- Regression: 11 selected shell scripts passed locally; GitHub CI results are recorded separately.
