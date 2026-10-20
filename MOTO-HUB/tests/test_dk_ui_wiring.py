from pathlib import Path
root=Path('app/src/main/java/com/motohub/next')
activity=(root/'MainActivity.java').read_text();hub=(root/'HubScreens.java').read_text()
fmt=(root/'BackupFormat.java').read_text();backup=(root/'BackupBundle.java').read_text()
restore=(root/'RestoreArchive.java').read_text();integrity=(root/'BackupIntegrity.java').read_text()
checks={
 'batch button': 'a::batchServiceEntry' in hub,
 'batch atomic transaction': 'for(JSONObject entry:result.records)Store.upsert(next,"services",entry);' in activity and 'write(next);DailyMaintenanceJob.checkSoon(this);' in activity,
 'batch duplicate confirmation': 'ServiceBatch.duplicates(items("services"),result.records)' in activity,
 'explicit interval opt-in': 'intervals.isChecked()' in activity,
 'receipt picker': 'startActivityForResult(intent,53)' in activity,
 'receipt app-private': 'ReceiptSlots.file(getFilesDir(),id,slot)' in activity and 'static File file(File dir,String id,int slot)' in (root/'ReceiptSlots.java').read_text(),
 'receipt atomic write': 'new AtomicFile(output)' in activity,
 'receipt state restore': 'out.putString("receiptPhotoTarget",receiptPhotoTarget)' in activity,
 'receipt linked to saved record': 'Store.find(db,key,id)==null' in activity,
 'receipt cleanup after record commit': 'write(next);if(key.equals("vehicles"))for' in activity and 'childReceipts' in activity and 'removeReceiptPhotos(childId)' in activity and 'removeReceiptPhotos(id)' in activity,
 'receipt backed up': '"receipts/"+file.getName()' in backup,
 'receipt restored': 'receipt=name.startsWith("receipts/")' in restore,
 'receipt path whitelisted': 'isReceipt(name)' in fmt,
 'receipt valid JPEG required': 'if(photo||receipt){if(size<4||!jpeg(destination))' in restore,
 'validation rejects corrupted JPEG': 'throw new IOException("손상된 사진 파일입니다.")' in integrity,
 'fast record cards': '＋ 바로 기록' in hub and '정비 여러 항목 한번에 등록' in hub,
 'record filtering': '"전체","주유","정비","기타"' in hub,
 'recent fuel card': '최근 주유  ·  ' in hub,
 'receipt not automatically shared': 'Sharing.start(' not in (root/'RideService.java').read_text(),
}
for k,v in checks.items():print(('PASS' if v else 'FAIL')+' DK '+k)
assert all(checks.values())
