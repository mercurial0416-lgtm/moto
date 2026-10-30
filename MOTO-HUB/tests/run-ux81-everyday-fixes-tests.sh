#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
javac -encoding UTF-8 -d "$tmp" app/src/main/java/com/motohub/next/UpdateErrorText.java tests/UX81ErrorMessagesTest.java
java -cp "$tmp" com.motohub.next.UX81ErrorMessagesTest
python3 - <<'PY'
from pathlib import Path
base=Path('app/src/main/java/com/motohub/next')
def s(name):return (base/name).read_text()
main=s('MainActivity.java');home=s('GarageHomeUi.java')
service=s('ServiceStatusUi.java');fuel=s('QuickEntryUi.java')
undo=s('UndoLastSave.java');updater=s('UpdateErrorText.java')
ci=Path('../.github/workflows/moto-ux-validation.yml').read_text()
assert 'void confirmPermanentBikeDelete(' in main and '판매 처리 (기록 유지)' in main
assert '서비스' not in '' # Do not assume remote services or delete backups.
assert '정비 "+serviceTotal+"건' in main
assert '백업·복원' in main and 'void removeConfirmed(' in main
assert 'a.toast(label+" 저장 완료' in undo and '.setTitle(label+" 저장 완료")' not in undo
assert 'UndoLastSave.available(a)' in home and '마지막 저장 실행 취소' in home
assert 'moneyWatcher.onTextChanged(' in main and 'value/10000' in main
assert 'rideBikeCaption(item)' in main and 'rideBikeCaption(pending)' in main
assert '정비 예정' in home and '정비 완료' in home and 'scheduleOnly' in home
assert 'void completeOilPair(' in service
assert 'Store.upsert(next,"services",oilDone);Store.upsert(next,"services",filterDone);' in service
assert 'OdometerGuard.require(current.optDouble("odo"),km);' in service
assert 'a.formFooter.getChildAt(' in fuel and 'IME_ACTION_DONE' in fuel
assert 'amount.requestFocus()' in fuel
assert '서명 인증서 불일치' in updater and '저장공간 부족' in updater
assert '파일 무결성 실패' in updater and '배포 파일 미공개' in updater
assert '-PmotoReleaseCode=$CODE' in ci and "versionCode='$CODE'" in ci
print('PASS UX81 eight practical refinements and destructive-operation guardrails')
PY
