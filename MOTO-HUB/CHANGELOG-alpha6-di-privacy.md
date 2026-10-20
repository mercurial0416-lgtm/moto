# MOTO HUB Alpha6-DI (518) — privacy & offline prebuild patch

No APK build, signing, Android compile or Gradle assemble was performed. VersionCode stays 518.

- Stored GPS routes start with external map tile data disabled. Online map tiles require explicit consent; original GPS points remain local. Offline tile backgrounds appear only if cached; otherwise route line renders against blank backdrop.
- Hardened network status to require Android NET_CAPABILITY_VALIDATED; no local writes depend on the status.
- Added manually copied allowlisted local diagnostics: version, signer status, permission and service flags, current ride point count; no coordinates, tokens, account IDs, stack traces, raw error messages or paths.
- After a verified update download and SharedPreferences state commit, prune only old 64-character SHA-256 APK cache filenames, avoiding symlinks, current APK, backups, GPS and unknown files.
- Preserved fixed signing SHA256: 212c5b39d0082ab4bed78f8da2b719819cd80a20bc116c84f8bdfabca507021b; no key generated or copied to public code.

Risks not solved in this prebuild patch: Android SDK compilation and Lint; APK installation and signing integration; offline base map caching and offline tile packaging; actual screen-off GPS, Bluetooth OBD2 and Supabase authenticated session end-to-end validation; automated crash trace reporting (excluded to protect user location and credentials).
