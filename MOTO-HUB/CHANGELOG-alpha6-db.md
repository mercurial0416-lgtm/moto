# MOTO HUB Alpha6-DB (511) — 2026-10-19

Input: verified DA(510) checkpoint ZIP (SHA256 c2d36de7a9b1c0aff29bd0b4aa9fa41436b5fca31701b5ebebe49f0f2bd4e6aa).

- ZIP restore safety: remove obsolete recognized ride/photo assets **before** the atomic JSON commit; fail on deletion errors and roll back originals instead of reporting a mixed-state success. New regression assertions check deletion timing and recovery on writer failure.
- GPS fix sanity: reject timestamps more than 120 seconds ahead of the device clock before they can poison monotonic track order. Preserve actual GPS timestamps; do not synthesize coordinates.
- Live ride elapsed timer: use `SystemClock.elapsedRealtime()` so network/manual wall-clock changes do not distort the duration shown in the ride screen.
- Version 511 and DB checkpoint references across nested and root GitHub Actions; DB safety tests added to both debug and signed workflows.
- Preserved offline riding, GPX, maintenance, listings, catalog, Supabase, explicit sharing consent, and developer-only read-only OBD2 behavior.

## Limitations
Android SDK/Gradle distribution and original signing key are not available in this runtime. Source-level tests do not establish that the APK installs or works on a physical motorcycle.
