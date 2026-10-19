# Alpha6-DE (514) — reliable backups and GPS-data confidence

Source: latest verified Alpha6-DD (513) full ZIP. Version code 514.

## Implemented

- Full ZIP archive export and ride/OBD auto-record now share an interlock. New ride starts and new restores are rejected while ZIP generation and file-provider readback are in progress. ZIP exports are denied if GPS recording, OBD automatic arm or restore is active. App update installation is also denied while recording, automatic OBD detection, backup or restore is active.
- Full ZIP export now preflights each optional GPX file and counts its exact serialized bytes against the total quota; a mid-export GPX I/O error aborts instead of silently producing a truncated but apparently successful file. The original `.track` remains included if optional GPX conversion is unavailable.
- After writing the SAF document, the app reopens it and verifies ZIP entries, duplicate/path rejection, size caps and entry CRC before telling the user that file export is complete. ZIP reread cannot prove that cloud synchronization has finished; keep a second offline copy.
- Ride analysis now displays GPS-observed time and timeline coverage, calculated from valid sequential location samples. Observed time explicitly does not imply continuous movement, and unobserved time is never mislabeled as stopped time. GPS accuracy over 40 m and long gaps break coverage.
- `TrackFile.tracks` now lists only names matching the app's track file format instead of any arbitrary `.track` file.
- Added pure-Java tests: byte-tampered ZIP rejection, GPX entry completeness, competing export reservations, ride/restore/update interlock and stationary GPS coverage.
- Maintained developer-mode-only, read-only OBD2, opt-in-only location sharing, offline GPS/GPX and existing garage, expenses, maintenance, listings, catalog, backup and Supabase group modules.

## Limits and outstanding

- A stable app-update signing key cannot be recovered from old APKs. New debug-build signatures may differ, so data-preserving install-over-update is NOT guaranteed. Back up your data before uninstalling.
- Real mobile 24-hour GPS, foreground-service battery restrictions, Bluetooth/OBD2 connection to a KTM motorcycle, Supabase server interactions, update-installer signature matching on a handset and 10K-ride UI profiling have not been verified on hardware.
- Optional GPX export cannot promise perfect historical routes for interrupted/damaged tracks; original local track bytes are kept in the ZIP.
