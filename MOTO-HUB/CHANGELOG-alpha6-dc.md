# Alpha6-DC (512) · 2026-10-19

- Android Lint blockers fixed: GPS registration now checks runtime fine-location permission immediately at the API boundary; developer-only telemetry view uses `Typeface.BOLD` constant.
- After a successful local database commit, save a separately verified atomic post-edit recovery checkpoint, retaining the existing immutable daily pre-edit snapshot.
- Retain the most recent 12 post-edit restore points in addition to 7 daily pre-edit backups; do not prune on failed writes. Checkpoint corruption fails closed through `Store.parse`.
- The recovery selector lists both kinds of local restore points with local Korean timestamps, and still requires confirmation before replacing data.
- Debug GitHub workflow preserves the configured Supabase publishable URL/key instead of blanking them; no background location sharing is introduced.
- Add Robolectric backup checkpoint tests and static integration tests.
- Previous vehicle, fuel, maintenance, listings, GPX, GPS, OBD2 developer-only read-only gate, and explicitly opt-in group sharing remain in place.

## Known remaining limits
- Android real-device GPS/OBD2 verification, persistent signing credential, and safe in-place app updates require additional controlled device/signing setup.
- Local snapshots are in app-private storage; uninstalling may remove them. Export a complete ZIP to external storage.
- No claim of fixed update signing or Supabase live integration without live tests.
