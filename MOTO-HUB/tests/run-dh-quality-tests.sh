#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
BIN=$(mktemp -d)
trap 'rm -rf "$BIN"' EXIT
javac -encoding UTF-8 -d "$BIN" \
  app/src/main/java/com/motohub/next/{BackupFormat,TrackFile,BackupBundle,BackupIntegrity,RestoreArchive,RestoreCapacity,BackupCrypto}.java \
  tests/{BackupCryptoTest,BackupCryptoArchiveTest}.java
java -cp "$BIN" com.motohub.next.BackupCryptoTest
java -cp "$BIN" com.motohub.next.BackupCryptoArchiveTest
python3 - <<'PY'
from pathlib import Path
root=Path('app/src/main/java/com/motohub/next')
main=(root/'MainActivity.java').read_text()
screen=(root/'HubScreens.java').read_text()
crypto=(root/'BackupCrypto.java').read_text()
assert '암호화 전체 백업 만들기' in screen
assert '암호화 백업 복원' in screen
assert 'BackupCrypto.decryptToPrivateFile' in main
assert 'BackupIntegrity.verify(in)' in main
assert 'Arrays.fill(password' in main
assert 'versionCode 517' in Path('app/build.gradle').read_text()
assert 'AES/GCM/NoPadding' in crypto and 'PBKDF2WithHmacSHA256' in crypto
assert 'Sharing.start(' not in (root/'RideService.java').read_text()
print('PASS DH encrypted backup UI, no password persistence, privacy checks')
PY
