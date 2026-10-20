package com.motohub.next;
import android.app.Activity;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;
import java.io.InputStream;
/** On-device recognition only: text never goes to the MOTO HUB backend. */
final class ReceiptOcr {
 interface Callback {void success(String text);void failure(String message);}
 private ReceiptOcr(){}
 static void scan(Activity a,Uri uri,Callback callback){
  new Thread(()->{
   Bitmap bitmap=null;
   try{
    BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
    try(InputStream in=a.getContentResolver().openInputStream(uri)){if(in==null)throw new IllegalArgumentException("사진을 읽을 수 없습니다.");BitmapFactory.decodeStream(in,null,bounds);}
    if(bounds.outWidth<=0||bounds.outHeight<=0||bounds.outWidth>25000||bounds.outHeight>25000)throw new IllegalArgumentException("지원하지 않는 이미지입니다.");
    BitmapFactory.Options opts=new BitmapFactory.Options();opts.inSampleSize=1;
    while(Math.max(bounds.outWidth,bounds.outHeight)/opts.inSampleSize>1800)opts.inSampleSize*=2;
    try(InputStream in=a.getContentResolver().openInputStream(uri)){if(in==null)throw new IllegalArgumentException("사진을 열 수 없습니다.");bitmap=BitmapFactory.decodeStream(in,null,opts);}
    if(bitmap==null)throw new IllegalArgumentException("사진을 인식할 수 없습니다.");
    final Bitmap toRecycle=bitmap;
    TextRecognizer recognizer=TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());
    recognizer.process(InputImage.fromBitmap(bitmap,0))
     .addOnSuccessListener(result->a.runOnUiThread(()->callback.success(result.getText())))
     .addOnFailureListener(e->a.runOnUiThread(()->callback.failure("문자 인식에 실패했어요. 직접 입력해 주세요.")))
     .addOnCompleteListener(t->{recognizer.close();toRecycle.recycle();});
   }catch(Exception e){if(bitmap!=null)bitmap.recycle();a.runOnUiThread(()->callback.failure(e.getMessage()==null?"사진 읽기 오류":e.getMessage()));}
  },"moto-private-receipt-ocr").start();
 }
}
