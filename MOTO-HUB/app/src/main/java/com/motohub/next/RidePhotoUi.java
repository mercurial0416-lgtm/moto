package com.motohub.next;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.view.Gravity;
import android.widget.*;
import androidx.core.content.FileProvider;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/** Explicit photo journal and share card; never publishes GPS coordinates or EXIF. */
final class RidePhotoUi {
 private final MainActivity a;
 RidePhotoUi(MainActivity activity){a=activity;}
 void open(File track,RideInsights.Result result){
  if(!RideMileageIntegrity.validCompleted(track)){a.toast("완료된 라이딩만 사진을 연결할 수 있어요.");return;}
  LinearLayout body=a.page("라이딩 사진 일지");
  a.label(body,"GPS 원본과 별도 보관 · 전체 ZIP 사진 백업에 포함",13,a.muted,false);
  File photo;
  try{photo=RidePhotoJournal.file(a.getFilesDir(),track);}catch(Exception e){a.error(e.getMessage());return;}
  if(photo.isFile()){
   try{
    Bitmap bitmap=RidePhotoJournal.readPreview(photo,900);
    ImageView image=new ImageView(a);image.setImageBitmap(bitmap);
    image.setScaleType(ImageView.ScaleType.CENTER_CROP);image.setAdjustViewBounds(true);
    image.setMaxHeight(a.dp(340));
    body.addView(image,new LinearLayout.LayoutParams(-1,a.dp(260)));
   }catch(Exception invalid){a.label(body,"사진 미리보기를 불러올 수 없어요. 원본은 그대로 보관합니다.",13,a.muted,false);}
  }else a.label(body,"등록된 사진이 없어도 거리·시간 공유 카드를 만들 수 있어요.",14,a.muted,false);
  a.gap(body,12);
  a.label(body,String.format(Locale.KOREA,"%.1f km · 평균 %.1f km/h · 최고 %.0f km/h",
    result.distanceKm,result.averageKmh,result.peakKmh),16,a.fg,true);
  a.action(body,photo.isFile()?"사진 교체 · 갤러리에서 선택":"사진 추가 · 갤러리에서 선택",true,()->{
   a.pendingRidePhotoTrack=track.getName();
   Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*");
   pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
   try{a.startActivityForResult(pick,93);}
   catch(Exception error){a.pendingRidePhotoTrack="";a.toast("사진 선택 앱을 열 수 없어요.");}
  });
  a.action(body,"라이딩 공유 카드 만들기 · 사진·거리·시간",false,()->share(track,result));
  if(photo.isFile())a.action(body,"이 라이딩의 사진 삭제",false,()->new AlertDialog.Builder(a)
   .setTitle("연결된 라이딩 사진을 지울까요?")
   .setMessage("GPS 경로·제목·메모와 정비 기록은 변경하지 않습니다.")
   .setNegativeButton("취소",null)
   .setPositiveButton("사진만 삭제",(d,w)->a.attempt(()->{
    if(a.readOnly)throw new IOException("데이터 보호 상태입니다.");
    RidePhotoJournal.remove(a.getFilesDir(),track);
    a.close();a.openRideInsights(track);
   })).show());
  a.label(body,"공유 카드는 사진을 새 JPEG로 변환해 메타데이터(EXIF)를 제외합니다. GPS 좌표는 포함하지 않아요. 다른 앱으로 보내려면 직접 공유 버튼을 눌러야 합니다.",12,a.muted,false);
  a.show();
 }
 static void receive(MainActivity a,Uri uri){
  String name=a.pendingRidePhotoTrack;a.pendingRidePhotoTrack="";
  if(name==null||!name.matches(BackupFormat.TRACK)){a.toast("라이딩 사진 선택이 만료됐어요.");return;}
  if(uri==null){a.toast("사진을 선택하지 않았어요.");return;}
  if(a.readOnly||RideService.running){a.toast("기록·복구 중에는 사진 변경을 제한합니다.");return;}
  File track=new File(a.getFilesDir(),"rides/"+name);
  a.toast("사진을 로컬에 저장 중이에요.");
  new Thread(()->{
   String err=null;
   try{RidePhotoJournal.attach(a,a.getFilesDir(),track,uri);}
   catch(Exception e){err=e.getMessage();}
   final String message=err;
   a.runOnUiThread(()->{
    if(a.isFinishing()||a.isDestroyed())return;
    if(message==null){a.close();a.toast("라이딩 사진 저장 완료 · 다음 ZIP 백업에 포함됩니다.");a.openRideInsights(track);}
    else a.error("사진 저장 실패: "+message);
   });
  },"moto-ride-photo").start();
 }
 private void share(File track,RideInsights.Result result){
  a.toast("개인정보를 제외한 공유 카드를 만들고 있어요.");
  final String title=RideNotes.title(a.db,track);
  final String bike=RideBikeBinding.read(track).isEmpty()?"MY RIDE":a.bikeName(RideBikeBinding.read(track));
  new Thread(()->{
   Uri share=null;String error=null;
   try{
    File cache=new File(a.getCacheDir(),"ride-share");
    if(!cache.isDirectory()&&!cache.mkdirs())throw new IOException("공유 임시 폴더 생성 실패");
    File[] older=cache.listFiles();
    if(older!=null)for(File old:older)if(old.getName().matches("ride-card-[a-f0-9-]{36}\\.jpg")
      &&System.currentTimeMillis()-old.lastModified()>86400000L)old.delete();
    File output=new File(cache,"ride-card-"+UUID.randomUUID()+".jpg");
    writeCard(output,track,result,bike,title);
    share=FileProvider.getUriForFile(a,a.getPackageName()+".receiptcamera",output);
   }catch(Exception ex){error=ex.getMessage();}
   final Uri uri=share;final String failure=error;
   a.runOnUiThread(()->{
    if(a.isFinishing()||a.isDestroyed())return;
    if(failure!=null){a.error("공유 카드 생성 실패: "+failure);return;}
    Intent send=new Intent(Intent.ACTION_SEND);send.setType("image/jpeg");
    send.putExtra(Intent.EXTRA_STREAM,uri);
    send.setClipData(ClipData.newRawUri("MOTO HUB 공유 카드",uri));
    send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    try{a.startActivity(Intent.createChooser(send,"라이딩 카드 공유"));}
    catch(Exception ex){a.error("공유 앱을 열 수 없어요.");}
   });
  },"moto-ride-share").start();
 }
 private static void writeCard(File output,File track,RideInsights.Result r,String bike,String title)throws Exception{
  Bitmap image=Bitmap.createBitmap(1080,1350,Bitmap.Config.ARGB_8888);
  Bitmap cover=null;
  try{
   Canvas canvas=new Canvas(image);
   Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
   canvas.drawColor(Color.rgb(16,30,53));
   File journal=new File(track.getParentFile().getParentFile(),RidePhotoJournal.photoName(track.getName()));
   if(journal.isFile())cover=RidePhotoJournal.readPreview(journal,1600);
   if(cover!=null){
    p.setFilterBitmap(true);
    float scale=Math.max(980f/cover.getWidth(),870f/cover.getHeight());
    float w=cover.getWidth()*scale,h=cover.getHeight()*scale;
    canvas.save();canvas.clipRect(50,50,1030,920);
    canvas.drawBitmap(cover,null,new RectF(540-w/2,485-h/2,540+w/2,485+h/2),p);
    canvas.restore();
   }else{
    p.setColor(Color.rgb(31,66,115));canvas.drawRoundRect(50,50,1030,920,32,32,p);
    p.setColor(Color.WHITE);p.setTextSize(74);p.setTypeface(Typeface.DEFAULT_BOLD);
    canvas.drawText("MY RIDE",100,460,p);
   }
   p.setColor(Color.rgb(125,179,255));p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(32);
   canvas.drawText("MOTO HUB  /  MY RIDE",65,1000,p);
   p.setColor(Color.WHITE);p.setTextSize(48);
   String name=(title==null||title.trim().isEmpty()?bike:title).replaceAll("[\\r\\n]"," ").trim();
   if(name.length()>24)name=name.substring(0,23)+"…";
   canvas.drawText(name,65,1063,p);
   p.setTextSize(70);p.setTypeface(Typeface.DEFAULT_BOLD);
   canvas.drawText(String.format(Locale.KOREA,"%.1f km",r.distanceKm),65,1165,p);
   p.setTextSize(30);p.setTypeface(Typeface.DEFAULT);
   String detail=String.format(Locale.KOREA,"%d분  |  평균 %.0f km/h  |  최고 %.0f km/h",
    r.durationMillis/60000,r.averageKmh,r.peakKmh);
   canvas.drawText(detail,65,1222,p);
   p.setColor(Color.rgb(177,190,210));p.setTextSize(24);
   canvas.drawText("GPS 추정값  ·  위치 좌표 및 EXIF 제외  ·  직접 선택한 공유",65,1297,p);
   try(FileOutputStream stream=new FileOutputStream(output)){
    if(!image.compress(Bitmap.CompressFormat.JPEG,88,stream))throw new IOException("카드 JPEG 저장 실패");
    stream.getFD().sync();
   }
  }finally{
   if(cover!=null)cover.recycle();
   image.recycle();
  }
 }
}
