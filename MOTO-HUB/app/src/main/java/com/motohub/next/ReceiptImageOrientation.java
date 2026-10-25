package com.motohub.next;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import java.io.InputStream;

/** Keep camera/gallery receipts upright after JPEG re-encoding strips EXIF metadata. */
final class ReceiptImageOrientation {
 private ReceiptImageOrientation(){}
 static int degrees(int orientation){
  return orientation==ExifInterface.ORIENTATION_ROTATE_90?90:
   orientation==ExifInterface.ORIENTATION_ROTATE_180?180:
   orientation==ExifInterface.ORIENTATION_ROTATE_270?270:0;
 }
 static int readDegrees(Context context,Uri uri){
  try(InputStream in=context.getContentResolver().openInputStream(uri)){
   return in==null?0:degrees(new ExifInterface(in).getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL));
  }catch(Exception e){return 0;}
 }
 static Bitmap rotate(Bitmap original,int degrees){
  if(original==null||degrees%360==0)return original;
  Matrix matrix=new Matrix();matrix.postRotate(degrees);
  Bitmap rotated=Bitmap.createBitmap(original,0,0,original.getWidth(),original.getHeight(),matrix,true);
  if(rotated!=original)original.recycle();
  return rotated;
 }
}
