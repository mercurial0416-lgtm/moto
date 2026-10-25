#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" \
  app/src/main/java/com/motohub/next/UpdateProgressPolicy.java \
  tests/UpdateProgressPolicyTest.java
java -cp "$out" com.motohub.next.UpdateProgressPolicyTest
python3 - <<'PY'
from pathlib import Path
root=Path("app/src/main/java/com/motohub/next")
s=(root/"AppUpdater.java").read_text(encoding="utf-8")
checks={
    "progress label":'progressLabel=a.text("업데이트 대기 중"' in s,
    "progress bar":'progressBar=new ProgressBar(a,null,android.R.attr.progressBarStyleHorizontal)' in s,
    "non-blocking UI":'progressLabel.isAttachedToWindow()' in s and 'progressBar.isAttachedToWindow()' in s,
    "bounded progress":'UpdateProgressPolicy.percentage(bytes,m.sizeBytes)' in s,
    "no progress toast spam":'a.toast("업데이트 파일 다운로드 "' not in s,
    "hash verify":'!hex(digest.digest()).equals(m.sha256)' in s,
    "archive signer verify":'validateArchive(part,m)' in s and 'validateArchive(f,m)' in s,
    "install rechecks hash":'verifyFile(f,m)' in s,
    "download busy guard":'BUSY.compareAndSet(false,true)' in s,
    "exception handled":'catch(Exception e)' in s and 'finally{' in s,
    "ride safety":'UpdateSafety.canInstall(' in s,
    "corrupt cache pointer reset":'prefs.edit().remove("ready").commit()' in s,
    "manual retry message":'배포 버전 확인에서 다시 내려받으세요.' in s,
    "android archive suffix":'m.sha256+".part.apk"' in s,
    "dynamic cached installer":'cachedInstallButton.setVisibility(ready?' in s,
    "refresh after download":'updateProgress("검사 완료 · 설치 준비됨",100)' in s and 'refreshCachedControls();' in s,
    "refresh after invalid cache":'ui(this::refreshCachedControls)' in s,
    "page owner captured":'owner=a.dialog;' in s,
    "stale result blocked":'owner==a.dialog&&owner.isShowing()' in s,
}
for k,v in checks.items(): assert v,k
print(f"PASS UX41 source guards: {len(checks)} checks")
PY
