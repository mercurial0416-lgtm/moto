from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'app/src/main/java/com/motohub/next'
main=(SRC/'MainActivity.java').read_text()
hub=(SRC/'HubScreens.java').read_text()
backup=(SRC/'BackupFormat.java').read_text()
ocr=(SRC/'ReceiptOcr.java').read_text()
gradle=(ROOT/'app/build.gradle').read_text()
checks={
 'batch autosave key scoped to bike': 'batchPreferences.edit().putString(bikeId,BatchDraft.encode' in main,
 'batch cleared only after atomic save': 'write(next);batchPreferences.edit().remove(bikeId).apply()' in main,
 'batch resumes previous draft': 'BatchDraft.decode(batchPreferences.getString(bikeId,""),bikeId)' in main,
 'batch resume shortcut on records': '작성 중인 정비 묶음 이어서 작성' in hub,
 'receipt photo slot restored after rotation': 'receiptPhotoSlot=state.getInt("receiptPhotoSlot",1)' in main,
 'receipt limit 5': 'ReceiptFiles.MAX_PER_RECORD' in main,
 'attachments count in list': 'a.receiptPhotos(x.optString("id"))' in hub,
 'photo record purge all slots': 'removeReceiptsFor(id);' in main,
 'encrypted ZIP receipt route accepts slots': '(?:-[2-5])?' in backup,
 'OCR explicitly requires review': 'OCR은 오인식할 수 있어요' in main and '자동 저장하지 않습니다' in main,
 'OCR preview lacks auto store': 'receiptOcrSuggestion(ReceiptTextParser.Guess guess)' in main,
 'Korean bundled ML Kit dependency': 'com.google.mlkit:text-recognition-korean:16.0.1' in gradle,
 'Korean OCR device local pipeline': 'KoreanTextRecognizerOptions.Builder()' in ocr,
 'OCR bounded input image': 'opts.inSampleSize*=2' in ocr,
 'fuel result computed from saved expenses': 'FuelEfficiency.forBike(allExpenses,id)' in hub,
 'fuel display explicitly full-tank': '가득→가득' in hub,
 'manual interval review dialog': '공식 정비 간격이 아닙니다' in main,
 'user manual interval persists scoped to bike': 'updatedSettings.put("serviceIntervals",storedIntervals)' in main,
 'immutable current package': "applicationId 'com.motohub.next'" in gradle,
 'version stays prebuild 518': "versionCode 518" in gradle,
 'no key asset in worktree': not any(ROOT.rglob('*.p12')),
}
for name, passed in checks.items():
    if not passed:raise SystemExit('FAIL: '+name)
print('PASS UX3 source wiring:', len(checks), 'checks')
