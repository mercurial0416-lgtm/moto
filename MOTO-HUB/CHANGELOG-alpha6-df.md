# MOTO HUB Alpha6-DF (515) — recoverable 10K ride backups

Built on the verified Alpha6-DE (514) full-source checkpoint.

## Implemented

- **10,000-ride backup/restore consistency:** expanded former 2,500-track export and 6,000-ZIP-entry restore/readback limits to 10,000 tracks and 100,000 archive entries. Maintained the 250 MB cumulative uncompressed-byte ceiling, 25 MB per ride/photo, 50 MB per generated GPX and introduced a 1,000-photo ceiling. These checks now share `BackupFormat` across export, readback and restore.
- **Stricter backup validation:** readback accepts only recognized ZIP names, rejects unknown entries, duplicate paths, invalid state-marker payloads, orphaned ride state files, invalid bike/category labels, damaged merged-ride references and unmerged markers without a completed ride. Every entry is streamed through CRC validation before reporting success. The reader remains bounded in memory by per-entry metadata size.
- **Interrupted session preservation:** an uncommitted single partial GPS row is preserved byte-for-byte when importing a full ZIP, provided no `.done`, `.recovered`, `.imported` or `.mergedinto` finalization is claimed. A committed track with no valid points remains rejected. Fully empty uncommitted GPS files are excluded during export; an empty *completed* track fails clearly instead of producing an archive that restore cannot read.
- **Deterministic file order:** backup exports photos and rides in filename order. During a failed restore, cleanup now uses a hash set instead of repeatedly scanning the old-file list, avoiding quadratic scans on 10K records.
- **Settings backup preflight UI:** non-destructive local preflight reports ride count, blank/unfinished rides, image count, oversized individual files, and approximate local source bytes. UI explicitly states that this is not a completed backup and excludes generated GPX overhead.
- **Tests:** real >2,500-ride export, synthetic 10,000-ride ZIP readback and restore staging, malformed sidecar/category/ZIP name rejection, interrupted partial-row round-trip, and preflight UI data checks. Previous recovery, OBD2 read-only, GPS consent and sharing policies retained.

## Important limits

- 10K synthetic archive tests are not real-Android stress tests. Actual large restores may take significant time and storage; 250 MB uncompressed cap still applies.
- Stable upgrade signing identity cannot be derived from a prior debug APK. Old versions cannot be uninstalled safely without off-device ZIP backup; signing private key or migration strategy is needed for install-over updates.
- No physical-device GPS endurance, KTM ECU read-only OBD2, Supabase account/group integration, or real handset reinstall/data persistence checks have been performed as part of this version.
