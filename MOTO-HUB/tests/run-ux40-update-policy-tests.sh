#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" \
  app/src/main/java/com/motohub/next/UpdateVersionPolicy.java \
  tests/UpdateVersionPolicyTest.java
java -cp "$out" com.motohub.next.UpdateVersionPolicyTest
python3 - <<'PY'
from pathlib import Path
base=Path("app/src/main/java/com/motohub/next")
s=(base/"AppUpdater.java").read_text()
assert 'UpdateVersionPolicy.isUpgrade(m.versionCode,BuildConfig.VERSION_CODE)' in s
assert 'UpdateVersionPolicy.currentMessage(m.versionCode,BuildConfig.VERSION_CODE)' in s
assert 'if(!fixed){' in s and 'a.show();return;' in s
assert 'UpdateManifest.PINNED_CERT_SHA256' in s
assert 'cachedUpdateReady()' in s and 'cachedInstallButton=a.button("다운로드한 업데이트 설치"' in s
assert 'validateArchive(f,m);' in s and 'verifyFile(f,m);' in s
assert 'ready.isFile()&&ready.length()==m.sizeBytes' in s
assert 'SigningIdentity.current(a)' in s
print("PASS update UI safety source guards: 8 checks")
PY
