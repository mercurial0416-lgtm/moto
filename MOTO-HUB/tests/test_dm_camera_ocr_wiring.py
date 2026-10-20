from pathlib import Path
import xml.etree.ElementTree as ET
p=Path('app/src/main/java/com/motohub/next');m=(p/'MainActivity.java').read_text();o=(p/'ReceiptOcr.java').read_text();g=Path('app/build.gradle').read_text();a=Path('app/src/main/AndroidManifest.xml').read_text();x=Path('app/src/main/res/xml/receipt_camera_paths.xml').read_text()
checks={
 'on-device model retained':'text-recognition-korean:16.0.1' in g,
 'file provider scoped': 'receipt-camera/' in x and '<files-path' not in x,
 'separate provider': 'MotoCameraFileProvider' in a and 'android:exported="false"' in a and 'android:grantUriPermissions="true"' in a,
 'no CAMERA permission': 'android.permission.CAMERA' not in a,
 'persist capture across rotation':'out.putString("cameraCaptureName"' in m and 'state.getString("cameraCaptureName"' in m,
 'camera result handled before null data guard':'if(request==55||request==56){finishReceiptCapture(request,result);return;}if(result!=RESULT_OK||data==null' in m,
 'explicit high-resolution output':'MediaStore.EXTRA_OUTPUT' in m,
 'temporary content uri grant':'FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION' in m and 'setClipData(' in m,
 'camera gallery both':'카메라로 촬영' in m and '갤러리에서 선택' in m,
 'OCR camera direct':'카메라로 영수증 바로 촬영' in m,
 'capture cleaned after OCR': 'scanReceiptOcr(local,()->ReceiptCameraFiles.delete(getCacheDir(),name))' in m,
 'capture cleaned after attachment': 'finally{ReceiptCameraFiles.delete(getCacheDir(),name);}' in m,
 'OCR metadata rotation':'ReceiptImageOrientation.readDegrees(activity,uri)' in o and 'InputImage.fromBitmap(pixels,rotation)' in o and 'new ExifInterface(in)' in (p/'ReceiptImageOrientation.java').read_text(),
 'candidate selection':'OCR 결제금액 선택' in m and 'amountCandidates' in m,
 'manual apply':'영수증 인식 후보 확인' in m and '입력칸에 적용' in m,
 'do not share OCR text':'Sharing.' not in o,
 'post-save receipt opt-in': 'attachAfterSave.isChecked()' in m and 'offerReceiptForSavedRecord(key,item)' in m,
 'no forced photo prompt':'attachAfterSave.setText(' in m,
 'service batch live cost':'batchLive.setText(' in m and 'refreshBatchTotal.run()' in m,
 'service quick pair choice':'엔진오일 + 오일필터 빠른 선택' in m,
 'service per-line confirmation':'detail.append(' in m and '정비 내용 확인' in m,
 'camera jpeg EXIF corrected':'ReceiptImageOrientation.rotate(image' in m,

}
ET.fromstring(a);ET.fromstring(x)
for n,v in checks.items():print(('PASS' if v else 'FAIL')+' UX4 '+n)
assert all(checks.values()),[n for n,v in checks.items() if not v]
