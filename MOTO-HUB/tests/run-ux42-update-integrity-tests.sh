#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" \
 app/src/main/java/com/motohub/next/UpdateResumePolicy.java \
 tests/UpdateResumePolicyTest.java
java -cp "$out" com.motohub.next.UpdateResumePolicyTest
python3 - <<'PY'
from pathlib import Path
java=Path("app/src/main/java/com/motohub/next")
up=(java/"AppUpdater.java").read_text()
snap=(java/"UpdateInstallSnapshot.java").read_text()
main=(java/"MainActivity.java").read_text()
home=(java/"GarageHomeUi.java").read_text()
screens=(java/"HubScreens.java").read_text()
cache=(java/"UpdateCacheRetention.java").read_text()
checks={
 "stable keyed partial":'m.sha256+".part.apk"' in up,
 "request range":'setRequestProperty("Range","bytes="+offset+"-")' in up,
 "range validation":'UpdateResumePolicy.validPartial(code,connection.getHeaderField("Content-Range")' in up,
 "full restart on 200":'requestOffset=0;bytes=0;digest=MessageDigest.getInstance("SHA-256")' in up,
 "bounded byte count":'if(n>m.sizeBytes-bytes)' in up,
 "full hash after resume":'!hex(digest.digest()).equals(m.sha256)' in up,
 "verify package and signer":'validateArchive(part,m)' in up and 'validateArchive(f,m)' in up,
 "resume file persists after transient":'finally{if(connection!=null)connection.disconnect();}' in up,
 "clean abandoned fragments":'UpdateCacheRetention.pruneStalePartials(dir,m.sha256' in up,
 "cleanup strict names":'matches("[a-f0-9]{64}\\\\.part\\\\.apk")' in cache,
 "snapshot atomic":'atomic.startWrite()' in snap and 'atomic.finishWrite(stream)' in snap,
 "snapshot parses":'Store.parse(new String(data,StandardCharsets.UTF_8))' in snap,
 "snapshot byte verification":'Arrays.equals(data,LocalBackupFilePolicy.readBounded(target))' in snap,
 "snapshot fails closed":'throw new IOException("복구 지점 저장 후 검증에 실패했습니다.")' in snap,
 "user can export full backup":'a.exportEncryptedBundle()' in up,
 "snapshot before installer":'File recovery=UpdateInstallSnapshot.create(a,a.disk)' in up,
 "installation markers":'UpdateInstallSnapshot.recordInstaller(a,(int)m.versionCode,recovery)' in up,
 "post update audit":'UpdateInstallSnapshot.afterStartup(this,BuildConfig.VERSION_CODE,!readOnly)' in main or 'UpdateInstallSnapshot.afterStartup(this,BuildConfig.VERSION_CODE,loaded)' in main,
 "off UI thread audit":'moto-post-update-data-audit' in main,
 "large font hero stacked":'LinearLayout meta=a.col()' in home,
 "large font metric stacked":'fontScale>=1.35f' in home and 'row.setOrientation(LinearLayout.VERTICAL)' in home,
 "large font record stacked":'if(largeText)head.setOrientation(LinearLayout.VERTICAL)' in screens,
 "retired listings renderer":'void listings(){' not in screens,
 "same pinned signer":'UpdateManifest.PINNED_CERT_SHA256' in up,
 "ride gate unchanged":'UpdateSafety.canInstall(' in up,
}
for name,passed in checks.items():assert passed,name
print(f"PASS UX42 source integrity: {len(checks)} checks")
PY
