#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
javac -encoding UTF-8 -d "$work" app/src/main/java/com/motohub/next/PhotoGalleryChoices.java tests/PhotoGalleryChoicesTest.java
java -cp "$work" com.motohub.next.PhotoGalleryChoicesTest
javac -encoding UTF-8 -d "$work" app/src/main/java/com/motohub/next/PhotoGalleryLoadGate.java tests/PhotoGalleryLoadGateTest.java
java -cp "$work" com.motohub.next.PhotoGalleryLoadGateTest
javac -encoding UTF-8 -d "$work" app/src/main/java/com/motohub/next/PhotoDecodeScale.java tests/PhotoDecodeScaleTest.java
java -cp "$work" com.motohub.next.PhotoDecodeScaleTest
python3 - <<'PYTEST'
from pathlib import Path
s=Path('app/src/main/java/com/motohub/next/MotorcyclePhotoGallery.java').read_text()
h=Path('app/src/main/java/com/motohub/next/GarageHomeUi.java').read_text()
p=Path('app/src/main/java/com/motohub/next/MotorcyclePhoto.java').read_text()
checks={
 'hero offers online gallery and local photo picker': 'MotorcyclePhotoGallery.open(a,brand,model,bike.optInt("year"));' in h and 'a.chooseGaragePhoto();return;' in h,
 'user-uploaded photo is not silently replaced': 'if(manualPhoto)options.add("내 사진 제거 (기본 사진으로)");' in h and 'setTitle("바이크 사진 관리")' in h and 'setPositiveButton("사진 제거"' in h,
 'six image choices maximum': 'MAX_CHOICES=6' in s,
 'gallery candidates use conservative Commons search': 'MotorcyclePhoto.searchCommons(' in s,
 'gallery displays image previews': 'image.setImageBitmap(ready)' in s,
 'gallery previews are downsampled': 'MotorcyclePhoto.downloadPreview(' in s and 'PhotoDecodeScale.sampleSize(' in p,
 'gallery selection saves full-size download': 'bitmap=MotorcyclePhoto.downloadImage(' in s and 'save(a,brand,model,year,choice,bitmap)' in s and 'bitmap.recycle()' in s,
 'preview loading failure is visible': '미리보기 실패 · 선택하면 다시 시도' in s,
 'gallery selection is explicit': 'row.setOnClickListener' in s and 'save(a,brand,model,year,choice,bitmap)' in s,
 'gallery saves JPEG atomically': 'AtomicFile photoFile=new AtomicFile(photo)' in s and 'photoFile.failWrite(photoOut)' in s,
 'gallery saves source and license metadata': 'selected.put("userSelected",true)' in s and 'AtomicFile metaFile=new AtomicFile(metadata)' in s,
 'failed JPEG commit rolls metadata back': 'restore.write(previous)' in s and 'if(previous==null)metadata.delete();' in s,
 'delayed automatic photo cannot replace gallery choice': 'synchronized(MotorcyclePhoto.class)' in p and 'if(!selected)' in p and 'synchronized(MotorcyclePhoto.class)' in s,
 'gallery network operations off UI thread': '"moto-photo-gallery-search").start()' in s and 'newFixedThreadPool(2' in s and 'PHOTO_IO.execute(' in s,
 'gallery saves selected photo without waiting for all thumbnails': 'newSingleThreadExecutor(' in s and 'SAVE_IO.execute(' in s,
 'gallery refuses concurrent double selection': 'applying.compareAndSet(false,true)' in s and 'applying.set(false)' in s,
 'gallery releases thumbnail bitmaps on close and retry': 'for(Bitmap preview:previews)' in s and 'previews.clear()' in s,
 'Wikimedia thumbnail requests have an identifiable user agent': 'c.setRequestProperty("User-Agent","MotoHubAndroid/4.0' in p,
 'dismissed gallery ignores async updates': 'canceled.get()' in s and 'dialog.setOnDismissListener' in s,
 'retry and local photo fallback on empty results': 'recovery.setVisibility(View.VISIBLE)' in s and 'a.chooseGaragePhoto();' in s and '다시 검색' in s,
 'older searches cannot overwrite retry results': 'gate.accepts(request)' in s and 'gate.begin()' in s,
 'dialog close invalidates search': 'gate.close()' in s,
 'no new location sharing or cloud writes': 'Sharing.' not in s and 'GroupClient' not in s,
}
for label,ok in checks.items():print(('PASS' if ok else 'FAIL')+': '+label)
if not all(checks.values()):raise SystemExit(1)
print(f'Gallery UI/source regression PASS ({len(checks)} static checks)')
PYTEST
