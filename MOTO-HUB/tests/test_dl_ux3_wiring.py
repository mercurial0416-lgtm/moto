from pathlib import Path
j=Path('app/src/main/java/com/motohub/next')
a=(j/'MainActivity.java').read_text();fmt=(j/'BackupFormat.java').read_text();hub=(j/'HubScreens.java').read_text()
checks={
 'draft private per bike':'"service_batch_drafts"' in a and '"batch_"+bikeId' in a,
 'draft restored by action':'이전에 작성하던 묶음 정비 불러오기' in a,
 'draft stored only separately':'batchPrefs.edit().putString(batchKey' in a and 'batchPrefs.edit().remove(batchKey)' in a,
 'receipt slot on recreate':'out.putInt("receiptPhotoSlot"' in a and 'state.getInt("receiptPhotoSlot",1)' in a,
 'old filename still usable':'return receiptPhotoFile(id,1)' in a,
 'multi image slot selector':'영수증 · 작업 사진 (최대 ' in a,
 'every receipt gets cleaned':'clearReceiptFiles(id)' in a and 'clearReceiptFiles(childId)' in a,
 'no file path manipulation':'ReceiptSlots.filename(id,slot)' in a,
 'backups include all slots':'ReceiptSlots.PATTERN' in fmt and 'name.matches(BackupFormat.RECEIPT)' in (j/'BackupBundle.java').read_text(),
 'restore validates all slots':'name.substring(9).matches(RECEIPT)' in (j/'RestoreArchive.java').read_text(),
 'vehicle only analytics':'FuelStats.forBike(a.items("expenses"),bikeId)' in hub,
 'full tank estimates labeled':'만땅 간격 추정 연비' in hub,
 'service latest batch gated':'ServiceBatchPreset.latest(prior,bikeId)' in a,
 'user must confirm service save':'정비 내용 확인' in a,
 'on-device OCR dependency':'com.google.mlkit:text-recognition-korean:16.0.1' in Path('app/build.gradle').read_text(),
 'OCR manual confirmation':'영수증 인식 후보 확인' in a and '입력칸에 적용' in a,
 'no network side effect':'Sharing.start(' not in (j/'RideService.java').read_text()
}
for k,v in checks.items():print(('PASS' if v else 'FAIL')+' UX3 '+k)
assert all(checks.values()),[k for k,v in checks.items() if not v]
