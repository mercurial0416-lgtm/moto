package com.motohub.next;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import java.io.*;
import java.nio.file.*;

/** Optional, local JPEG per finished ride. GPS originals and the JSON schema are untouched. */
final class RidePhotoJournal {
 static final long MAX_INPUT_BYTES=12_000_000L;
 static final long MAX_PHOTO_BYTES=8_000_000L;
 private RidePhotoJournal(){}

 static String photoName(String trackName) {
  if(trackName==null||!trackName.matches(BackupFormat.TRACK))
   throw new IllegalArgumentException("라이딩 파일 이름이 올바르지 않아요.");
  return "ride-photo-"+trackName.substring(0,trackName.length()-6)+".jpg";
 }
 static File file(File filesDir,File track)throws IOException{
  if(filesDir==null||track==null||!RideMileageIntegrity.validCompleted(track))
   throw new IOException("완료된 GPS 원본에서만 사진을 저장할 수 있어요.");
  File photo=new File(filesDir,photoName(track.getName()));
  if(Files.isSymbolicLink(photo.toPath()))throw new IOException("안전하지 않은 사진 경로");
  return photo;
 }
 static Bitmap readPreview(File file,int maxDimension)throws IOException{
  if(file==null||!file.isFile()||Files.isSymbolicLink(file.toPath())||file.length()>MAX_PHOTO_BYTES)
   throw new IOException("사진을 읽을 수 없어요.");
  BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
  BitmapFactory.decodeFile(file.getPath(),bounds);
  if(bounds.outWidth<1||bounds.outHeight<1||bounds.outWidth>25000||bounds.outHeight>25000)
   throw new IOException("지원되지 않는 사진 크기입니다.");
  int sample=1;
  while(bounds.outWidth/sample>maxDimension||bounds.outHeight/sample>maxDimension)sample*=2;
  BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=sample;
  Bitmap bitmap=BitmapFactory.decodeFile(file.getPath(),options);
  if(bitmap==null)throw new IOException("사진 디코딩 실패");
  return bitmap;
 }
 static void attach(Context context,File filesDir,File track,Uri uri)throws Exception{
  File target=file(filesDir,track);
  if(uri==null)throw new IOException("선택한 사진이 없어요.");
  byte[] bytes;
  try(InputStream in=context.getContentResolver().openInputStream(uri)){
   if(in==null)throw new IOException("사진 파일을 읽지 못했어요.");
   ByteArrayOutputStream out=new ByteArrayOutputStream();
   byte[] buffer=new byte[32768];int n;
   while((n=in.read(buffer))!=-1){
    if(out.size()+n>MAX_INPUT_BYTES)throw new IOException("사진은 12MB 이하로 선택해 주세요.");
    out.write(buffer,0,n);
   }
   bytes=out.toByteArray();
  }
  BitmapFactory.Options b=new BitmapFactory.Options();b.inJustDecodeBounds=true;
  BitmapFactory.decodeByteArray(bytes,0,bytes.length,b);
  if(b.outWidth<1||b.outHeight<1||b.outWidth>25000||b.outHeight>25000)
   throw new IOException("올바른 JPG/PNG 사진이 아닙니다.");
  int sample=1;
  while(b.outWidth/sample>1800||b.outHeight/sample>1800)sample*=2;
  BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=sample;
  Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
  if(bitmap==null)throw new IOException("사진을 열 수 없어요.");
  File temporary=new File(filesDir,target.getName()+".writing");
  try{
   if(Files.isSymbolicLink(temporary.toPath()))throw new IOException("안전하지 않은 사진 임시 파일");
   try(FileOutputStream out=new FileOutputStream(temporary)){
    if(!bitmap.compress(Bitmap.CompressFormat.JPEG,84,out))throw new IOException("JPG 변환 실패");
    out.getFD().sync();
   }
   if(temporary.length()<128||temporary.length()>MAX_PHOTO_BYTES)
    throw new IOException("사진 저장 크기 오류");
   try{Files.move(temporary.toPath(),target.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
   catch(AtomicMoveNotSupportedException fallback){
    Files.move(temporary.toPath(),target.toPath(),StandardCopyOption.REPLACE_EXISTING);
   }
  }finally{bitmap.recycle();temporary.delete();}
 }
 static void remove(File filesDir,File track)throws IOException{
  File photo=file(filesDir,track);
  if(photo.exists()&&!photo.delete())throw new IOException("사진을 삭제하지 못했어요.");
 }
}
