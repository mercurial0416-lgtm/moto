# MOTO HUB Alpha6-DI(518) UX2 — source-only usability / UI patch

## Maintenance and fuel workflow

- Added a **single batch maintenance transaction** for up to eight categories (e.g. engine oil + oil filter + brakes). One application data write commits every selected line item; none are written until user confirms a total-cost summary. Reject invalid date, bike ID, mileage, duplicate item names and invalid cost. Duplicates of existing records trigger a warning instead of silent overwrite.
- Per-bike previous service cost reuse is explicit. Previous interval reuse is opt-in, and no manufacturer's official service intervals are assumed.
- Improved the records home UI: prominent quick-entry controls, batch entry button, a recent-fuel summary, and separate **all / fuel / service / other** tabs. Long histories retain bounded pagination.

## Private receipt photos

- Existing service or expense record can link one selected receipt photo. Decode downsampled, convert to JPEG, atomically replace prior attachment. Display/replace/delete receipt without changing the record.
- Local receipt photos use `record-receipt-<UUID>.jpg` under app-private storage. Saved photo is **not uploaded** to the server.
- `BackupBundle`, `BackupFormat`, `BackupIntegrity`, `BackupReadiness`, `RestoreArchive` updated to export, verify and restore `receipts/` entries. Each JPEG validates start/end marker; archive path and file count limits enforced. Rollback includes receipt assets.
- Both ordinary and AES-GCM encrypted full-archive round trips validated using synthetic JPEG and local source tests. Backing JSON file and current local GPS data model stay unchanged.
- Removing a receipt or deleting a record removes only that receipt; deleting a motorcycle also clears photos associated with its deleted services/expenses after the data commit. A failure to delete produces a warning.

## Tests and limitations

- New batch service tests: 19 assertions. Receipt archive and encrypted backup/restore: 12 assertions. Source-level wiring tests: 19 assertions. Existing nine selected regression shell scripts passed (see `DK_PREBUILD_VERIFICATION.json`).
- Source syntax parser passed for 132 Android Java source files. **No Android SDK compile, Android Lint, emulator, actual phone verification or APK build.**
- Existing fixed signing credentials are untouched; `versionCode=518` remains unchanged. No signing keys/passwords embedded in this archive.
- OCR extracting receipt data, real OBD2 odometer read, real motorcycle service intervals, photo capture on a real phone and batch unsaved draft restoration are **not delivered**. Use photo selection and manual confirmation instead.
- **Compatibility note:** older app versions may reject the new `receipts/` ZIP paths. Preserve the old-format backup and use this app code to restore new-format backups.
