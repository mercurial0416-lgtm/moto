from pathlib import Path
root=Path(__file__).resolve().parents[1]
java=root/'app/src/main/java/com/motohub/next'
main=(java/'MainActivity.java').read_text();ui=(java/'HubScreens.java').read_text();backup=(java/'BackupFormat.java').read_text();gradle=(root/'app/build.gradle').read_text()
assert '영수증 사진에서 금액·날짜 읽기' in main
assert 'ReceiptOcr.scan(this,uri' in main
assert 'new AlertDialog.Builder(MainActivity.this).setTitle("사진 문자 인식 결과")' in main
assert 'text-recognition-korean:16.0.1' in gradle
assert 'getSharedPreferences("batch_service_draft"' in main
assert 'putString("draft."+bikeId,raw)' in main
assert 'ManualServiceIntervals.next' in main
assert 'manualServiceIntervals' in main
assert 'BatchDraft.decode' in main
assert 'write(next);getSharedPreferences("batch_service_draft"' in main
assert 'ReceiptSlots.nextFree' in main
assert 'removeReceiptPhotos(childId)' in main
assert 'ReceiptSlots.count' in ui
assert 'FuelTrend.forBike' in ui
assert '[2-4]' in backup
print('PASS UX3 source integration assertions')
