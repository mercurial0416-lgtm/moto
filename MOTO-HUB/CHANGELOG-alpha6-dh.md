# MOTO HUB Alpha6-DH (517) — secure offline encrypted backups

Based on previously verified DG(516). All existing JSON and unencrypted ZIP import/export paths remain supported.

## Changes
- New opt-in encrypted full backup with password confirmation and explicit extension `.mhub`. ZIP contents are encrypted, including GPS tracks, GPX files and garage photos.
- AES-256-GCM, password-derived 256-bit keys with PBKDF2-HMAC-SHA256 (210,000 iterations), 16-byte salt, 8-byte random nonce prefix and per-record counters, with independent authenticated 64 KiB chunks and mandatory authenticated final marker. The format is `MOTOENC2`; only compatible MOTO HUB builds can open it.
- Reject incorrect passwords, tampered/reordered/truncated ciphertext, unknown headers and appended garbage before invoking ZIP parsing or exposing any restore confirmation. Never automatically persist the password.
- Full encrypted export is reopened, fully decrypted to an app-private temporary file, and verified by the existing ZIP integrity reader before success is shown.
- Encrypted restore stages the plaintext exclusively in the private cache while verifying the complete ciphertext before entering the existing atomic ZIP restore workflow; temporary files are removed after use. Existing ride/export interlocks and explicit restore consent remain in place.
- Allow the developer OBD display to rotate on larger phones/tablets rather than permanently locking portrait.
- New plain-Java adversarial tests for roundtrip, wrong password, bit flips, truncation, trailing bytes, invalid header and real ZIP restoration.
- Synchronize CI/version metadata and preserve all original signing protections.

## Important limitations
- This is a new encrypted format; older versions cannot open `.mhub`. A password lost by its owner cannot be recovered. This password encryption is not independently audited.
- Decrypted ZIP bytes temporarily occupy app-private cache during validation. Normal code deletes them after reading, but forced process termination may leave a temporary file until Android clears app cache. Do not put the app cache on shared storage.
- Hardware GPS/OBD/Supabase tests cannot be certified from an Android CI build.
- No previous debug signing private key was recovered. Existing installations with a different signing certificate still cannot be overwritten. Do not uninstall without an off-device backup.
