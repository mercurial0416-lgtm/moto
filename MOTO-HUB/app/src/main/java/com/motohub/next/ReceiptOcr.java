package com.motohub.next;

import android.app.Activity;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.InputStream;
import java.io.IOException;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;

/** Bundled on-device OCR. Decodes bounded pixels, never stores or uploads recognition text. */
final class ReceiptOcr {
 private ReceiptOcr(){}
 interface Callback {void success(String text);void failure(String description);}
 static void scan(Activity activity,Uri uri,Callback callback){
  new Thread(()->{
   Bitmap bitmap=null;
   try{
    BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;
    try(InputStream stream=activity.getContentResolver().openInputStream(uri)){
     if(stream==null)throw new IOException("사진을 열 수 없습니다.");
     BitmapFactory.decodeStream(stream,null,options);
    }
    if(options.outWidth<1||options.outHeight<1||options.outWidth>25000||options.outHeight>25000)
     throw new IOException("지원하지 않는 이미지 크기입니다.");
    int sample=1;while(options.outWidth/sample>1800||options.outHeight/sample>1800)sample*=2;
    options.inJustDecodeBounds=false;options.inSampleSize=sample;
    try(InputStream stream=activity.getContentResolver().openInputStream(uri)){
     if(stream==null)throw new IOException("사진을 읽을 수 없습니다.");
     bitmap=BitmapFactory.decodeStream(stream,null,options);
    }
    if(bitmap==null)throw new IOException("사진을 읽을 수 없습니다.");
    final Bitmap pixels=bitmap;
    int rotation=ReceiptImageOrientation.readDegrees(activity,uri);
    InputImage input=InputImage.fromBitmap(pixels,rotation);
    activity.runOnUiThread(()->{
     if(activity.isFinishing()||activity.isDestroyed()){
      pixels.recycle();callback.failure("화면이 닫혀 영수증 인식이 취소됐습니다.");return;
     }
     TextRecognizer recognizer=TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());
     try{
      recognizer.process(input)
       .addOnSuccessListener(result->{try{callback.success(result.getText());}finally{recognizer.close();pixels.recycle();}})
       .addOnFailureListener(error->{try{callback.failure("문자를 인식하지 못했어요. 직접 입력해 주세요.");}finally{recognizer.close();pixels.recycle();}});
     }catch(Exception e){recognizer.close();pixels.recycle();callback.failure("OCR 처리 실패 · 직접 입력해 주세요.");}
    });
   }catch(Exception e){if(bitmap!=null)bitmap.recycle();activity.runOnUiThread(()->callback.failure("사진을 열지 못했어요. 직접 입력해 주세요."));}
  },"moto-receipt-offline-ocr").start();
 }
}
