package com.motohub.next;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;
import java.io.InputStream;
import java.util.function.Consumer;

/** Bundled, on-device Korean OCR. No receipt bytes are uploaded by this class. */
final class ReceiptOcr {
 private ReceiptOcr(){}
 static void read(Context context,Uri uri,Consumer<ReceiptTextParser.Guess> success,Consumer<String> failure){
  new Thread(()->{
   Bitmap image=null;
   try{
    BitmapFactory.Options opts=new BitmapFactory.Options();opts.inJustDecodeBounds=true;
    try(InputStream scan=context.getContentResolver().openInputStream(uri)){
     if(scan==null)throw new IllegalArgumentException("사진을 열 수 없어요.");BitmapFactory.decodeStream(scan,null,opts);
    }
    if(opts.outWidth<1||opts.outHeight<1||opts.outWidth>25000||opts.outHeight>25000)throw new IllegalArgumentException("지원하지 않는 사진 크기예요.");
    opts.inJustDecodeBounds=false;opts.inSampleSize=1;
    while(Math.max(opts.outWidth,opts.outHeight)/opts.inSampleSize>1800)opts.inSampleSize*=2;
    try(InputStream source=context.getContentResolver().openInputStream(uri)){
     if(source==null)throw new IllegalArgumentException("사진을 읽을 수 없어요.");image=BitmapFactory.decodeStream(source,null,opts);
    }
    if(image==null)throw new IllegalArgumentException("사진을 해석할 수 없어요.");
    Bitmap loaded=image;
    TextRecognizer reader=TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());
    reader.process(InputImage.fromBitmap(loaded,0))
      .addOnSuccessListener(result->{try{success.accept(ReceiptTextParser.parse(result.getText()));}finally{reader.close();loaded.recycle();}})
      .addOnFailureListener(e->{try{failure.accept("인식에 실패했어요. 사진을 다시 선택하거나 직접 입력해 주세요.");}finally{reader.close();loaded.recycle();}});
   }catch(Exception e){if(image!=null)image.recycle();failure.accept(e.getMessage()==null?"OCR 실행 오류":e.getMessage());}
  },"moto-on-device-receipt-ocr").start();
 }
}
