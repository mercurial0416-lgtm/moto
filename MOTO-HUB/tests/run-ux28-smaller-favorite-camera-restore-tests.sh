#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
src=Path('app/src/main/java/com/motohub/next/MainActivity.java').read_text()
section=src[src.index('boolean entered=false;',src.index('void prepareBundleRestore(Uri uri,char[] password)')):]
section=section[:section.index('},"MOTO-ZIP-restore-apply").start()')]
assert 'ready.install(getFilesDir(),json->{' in section
assert 'atomic(disk,next.toString(2).getBytes(StandardCharsets.UTF_8));' in section
assert 'db=next;readOnly=false;' in section
assert 'write(next)' not in section.split('ready.install(getFilesDir(),json->{',1)[1].split('LocalSafetyBackup.checkpoint(this,db)',1)[0]
assert section.index('LocalSafetyBackup.checkpoint(this,db)') > section.index('ready.install(getFilesDir()')
assert 'before-import.json' in section and 'atomic(rollback,previous)' in section
ui=Path('app/src/main/java/com/motohub/next/RideUi.java').read_text()
hub=Path('app/src/main/java/com/motohub/next/HubScreens.java').read_text()
q=Path('app/src/main/java/com/motohub/next/QuickEntryUi.java').read_text()
assert 'star.setContentDescription(starred?"라이딩 즐겨찾기 해제":"라이딩 즐겨찾기")' in hub
assert 'Button star=a.button(starred?"★":"☆",false,favorite)' in hub
assert 'RideFavorite.toggle(f)' in ui and 'results.addView(star)' not in ui
assert 'startQuickOdometerCamera(odo)' in q
assert 'startActivityForResult(capture,58);' in src
assert 'if(request==55||request==56||request==58){finishReceiptCapture(request,result);return;}' in src
assert 'if(request==58){scanQuickOdometerPhoto(local,()->ReceiptCameraFiles.delete(getCacheDir(),name));return;}' in src
assert 'if(cleanup!=null)cleanup.run();' in src
assert 'target.setText(candidate)' in src and 'setSingleChoiceItems' in src
print('PASS UX28: compact star, private OCR capture cleanup, JSON commit/side-effect separation')
PY
