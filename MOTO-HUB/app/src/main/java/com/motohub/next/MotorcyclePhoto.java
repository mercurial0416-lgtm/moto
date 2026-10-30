package com.motohub.next;

import android.content.Context;
import android.graphics.*;
import android.util.AtomicFile;
import android.widget.ImageView;
import android.widget.TextView;
import org.json.*;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.*;

/** Candidate photographs from Wikimedia Commons. Exact-year in file title is a search signal, not model-year certification. */
final class MotorcyclePhoto {
 private MotorcyclePhoto(){}
 // Model-photo disk decode is bounded and never blocks scrolling on the Android main thread.
 private static final java.util.concurrent.ThreadPoolExecutor PHOTO_DISK=
  new java.util.concurrent.ThreadPoolExecutor(1,1,0L,java.util.concurrent.TimeUnit.MILLISECONDS,
   new java.util.concurrent.ArrayBlockingQueue<Runnable>(12),r->{
    Thread t=new Thread(r,"moto-photo-cache");t.setDaemon(true);return t;
   },new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());
 static String key(String brand,String model,int year){return MotorcycleCatalog.norm(brand)+"_"+MotorcycleCatalog.norm(model)+"_"+year;}
 static String tinyName(String k){try{byte[] b=MessageDigest.getInstance("SHA-256").digest(k.getBytes("UTF-8"));StringBuilder s=new StringBuilder();for(byte x:b)s.append(String.format(Locale.ROOT,"%02x",x));return s.substring(0,24);}catch(Exception e){return Integer.toHexString(k.hashCode());}}
 static File photoFile(Context c,String brand,String model,int year){File folder=new File(c.getFilesDir(),"moto-year-photos");return new File(folder,tinyName(key(brand,model,year))+".jpg");}
 static boolean matches(String title,String brand,String model,int year){
  return PhotoTitleMatcher.matches(title,brand,model,year);
 }
 static boolean matchesModel(String title,String brand,String model){
  return PhotoTitleMatcher.matchesModel(title,brand,model);
 }
 /** A failed or unsupported Commons image must not block the next matching photo. */
 static final class PhotoLoad {
  final JSONObject source;
  final Bitmap bitmap;
  PhotoLoad(JSONObject source,Bitmap bitmap){this.source=source;this.bitmap=bitmap;}
 }
 /** Search diagnostics are returned with the photo, without exposing request URLs to the UI. */
 static final class PhotoSearchResult {
  final PhotoLoad photo;
  final PhotoSearchStatus.Kind failure;
  PhotoSearchResult(PhotoLoad photo,PhotoSearchStatus.Kind failure){this.photo=photo;this.failure=failure;}
 }
 static PhotoLoad candidate(String brand,String model,int year){return candidateWithStatus(brand,model,year).photo;}
 static PhotoSearchResult candidateWithStatus(String brand,String model,int year){
  PhotoSearchStatus.Trace trace=new PhotoSearchStatus.Trace();
  PhotoLoad loaded=null;
  if(year>=1900&&year<=2200)
   loaded=firstDownload(year+" "+brand+" "+model,brand,model,year,true,trace);
  if(loaded==null)loaded=firstDownload(brand+" "+model+" motorcycle",brand,model,year,false,trace);
  if(loaded==null)loaded=firstDownload(brand+" "+model,brand,model,year,false,trace);
  return new PhotoSearchResult(loaded,loaded==null?trace.result():null);
 }
 static PhotoLoad firstDownload(String query,String brand,String model,int year,boolean exactYear){
  return firstDownload(query,brand,model,year,exactYear,new PhotoSearchStatus.Trace());
 }
 static PhotoLoad firstDownload(String query,String brand,String model,int year,boolean exactYear,PhotoSearchStatus.Trace trace){
  try {
   List<JSONObject> candidates=searchCommons(query,brand,model,year,exactYear);
   trace.searchSucceeded(candidates.size());
   PhotoLoad result=PhotoRetry.firstSuccess(candidates,3,meta->{
    try{
     Bitmap bitmap=downloadImage(meta.optString("imageUrl"));
     return bitmap==null?null:new PhotoLoad(meta,bitmap);
    }catch(Exception downloadError){
     trace.downloadFailed(downloadError); // Keep the real network reason after fallback retries.
     throw downloadError;
    }
   });
   if(result==null&&!candidates.isEmpty())trace.imageFailed();
   return result;
  }catch(Exception error){trace.searchFailed(error);return null;}
 }
 static List<JSONObject> searchCommons(String search,String brand,String model,int year,boolean exactYear)throws Exception{
  List<JSONObject> candidates=new ArrayList<>();
  Set<String> seenImageUrls=new HashSet<>(); // An identical broken photo needs only one network attempt.
  URL url=new URL("https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrsearch="+URLEncoder.encode(search,"UTF-8")+"&gsrnamespace=6&gsrlimit=40&prop=imageinfo&iiprop=url%7Cmime%7Cextmetadata&iiurlwidth=1100&format=json");
  HttpURLConnection c=(HttpURLConnection)url.openConnection();try{
   c.setConnectTimeout(6000);c.setReadTimeout(8000);c.setRequestProperty("User-Agent","MotoHubAndroid/4.0 (motorcycle photo lookup; https://github.com/mercurial0416-lgtm/moto)");c.setRequestProperty("Accept","application/json");
   if(c.getResponseCode()!=200)throw new IOException("Commons HTTP "+c.getResponseCode());
   try(InputStream in=c.getInputStream()){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){if(out.size()+n>250000)throw new IOException("image result too big");out.write(buf,0,n);}
    JSONObject pages=new JSONObject(out.toString("UTF-8")).optJSONObject("query");if(pages==null)return candidates;pages=pages.optJSONObject("pages");if(pages==null)return candidates;
    for(Iterator<String> keys=pages.keys();keys.hasNext();){JSONObject page=pages.optJSONObject(keys.next());if(page==null)continue;
      String title=page.optString("title");if(exactYear?!matches(title,brand,model,year):!PhotoTitleMatcher.matchesFallback(title,brand,model,year))continue;
      JSONArray images=page.optJSONArray("imageinfo");JSONObject im=images==null?null:images.optJSONObject(0);if(im==null||!PhotoMimeFilter.supported(im.optString("mime")))continue;
      String imageUrl=im.optString("thumburl",im.optString("url"));String pageUrl=im.optString("descriptionurl");
      if(!imageUrl.startsWith("https://upload.wikimedia.org/")||!seenImageUrls.add(imageUrl))continue;
      if(pageUrl.isEmpty())pageUrl="https://commons.wikimedia.org/wiki/"+title.replace(' ','_');
      if(!pageUrl.startsWith("https://commons.wikimedia.org/"))continue;
      JSONObject ext=im.optJSONObject("extmetadata");String license=ext==null?"":ext.optJSONObject("LicenseShortName")==null?"":ext.optJSONObject("LicenseShortName").optString("value");
      if(license.isEmpty())license="Wikimedia Commons";
      JSONObject candidate=new JSONObject();candidate.put("imageUrl",imageUrl);candidate.put("pageUrl",pageUrl);candidate.put("label",title);candidate.put("license",license);candidate.put("exactYearCandidate",exactYear);candidates.add(candidate);
    }
    return candidates;
   }
  }finally{c.disconnect();}
 }
 static Bitmap downloadImage(String s)throws Exception{return downloadImage(s,1300);}
 static Bitmap downloadPreview(String s)throws Exception{return downloadImage(s,360);}
 private static Bitmap downloadImage(String s,int maxDimension)throws Exception{
  URL url=new URL(s);if(!"https".equals(url.getProtocol())||!"upload.wikimedia.org".equals(url.getHost()))return null;
  HttpURLConnection c=(HttpURLConnection)url.openConnection();try{c.setConnectTimeout(7000);c.setReadTimeout(10000);c.setInstanceFollowRedirects(false);
   // Wikimedia discourages anonymous library-default User-Agent requests, including thumbnails.
   c.setRequestProperty("User-Agent","MotoHubAndroid/4.0 (motorcycle photo lookup; https://github.com/mercurial0416-lgtm/moto)");
   c.setRequestProperty("Accept","image/jpeg,image/png,image/webp");
   if(c.getResponseCode()!=200)return null;int length=c.getContentLength();if(length>4500000)return null;
   try(InputStream in=c.getInputStream()){
    ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] chunk=new byte[4096];int n;while((n=in.read(chunk))!=-1){if(out.size()+n>4500000)return null;out.write(chunk,0,n);}
    byte[] b=out.toByteArray();BitmapFactory.Options opts=new BitmapFactory.Options();opts.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(b,0,b.length,opts);if(opts.outWidth<=0||opts.outWidth>9000||opts.outHeight<=0||opts.outHeight>9000)return null;
    opts.inJustDecodeBounds=false;opts.inSampleSize=PhotoDecodeScale.sampleSize(opts.outWidth,opts.outHeight,maxDimension);return BitmapFactory.decodeByteArray(b,0,b.length,opts);
   }
  }finally{c.disconnect();}
 }
 /** Decode local user or model photos at display size, never full camera resolution. */
 static Bitmap decodeLocalSampled(File photo,int maxDimension){
  if(photo==null||!photo.isFile()||maxDimension<1)return null;
  try{
   BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
   BitmapFactory.decodeFile(photo.getAbsolutePath(),bounds);
   if(bounds.outWidth<1||bounds.outHeight<1||bounds.outWidth>20000||bounds.outHeight>20000)return null;
   BitmapFactory.Options scaled=new BitmapFactory.Options();
   scaled.inSampleSize=PhotoDecodeScale.sampleSize(bounds.outWidth,bounds.outHeight,maxDimension);
   return BitmapFactory.decodeFile(photo.getAbsolutePath(),scaled);
  }catch(Exception invalid){return null;}
 }
 /** Main thread only. Each lookup owns a generation token, so stale responses cannot overwrite newer selections. */
 static void show(MainActivity a,ImageView target,TextView credit,String brand,String model,int year,String garageBikeId){
  showInternal(a,target,credit,brand,model,year,garageBikeId,false,false);
 }
 /** Called after the owner's local photo failed image decoding. Never delete/overwrite that file. */
 static void showFallback(MainActivity a,ImageView target,TextView credit,String brand,String model,int year,String garageBikeId){
  showInternal(a,target,credit,brand,model,year,garageBikeId,true,false);
 }
 private static void showInternal(MainActivity a,ImageView target,TextView credit,String brand,String model,int year,String garageBikeId,boolean skipInvalidLocal,boolean skipModelCache){
  // Only a garage card is allowed to load or change its owner's manually chosen photo.
  // Catalog previews must not silently display or overwrite the currently selected bike.
  final boolean garageMode=PhotoViewPolicy.hasGarageOwner(garageBikeId);
  // Invalidate all older requests even when the new selection has no photo.
  long requestToken=PhotoRequestGuard.begin(target);
  credit.setOnClickListener(null); // Never leave a previous model's source link active.
  // A reused preview must not keep displaying the previous motorcycle during lookup/failure.
  target.setImageResource(BikeVisual.superCub(model)?R.drawable.garage_super_cub:R.drawable.garage_model_placeholder);
  if(brand.isEmpty()||model.isEmpty()) {credit.setText(garageMode?"모델 정보 확인 필요 · 탭해서 사진 직접 등록":"모델 정보를 선택하면 사진을 검색해요");if(garageMode)credit.setOnClickListener(v->a.chooseGaragePhoto());return;}
  if(garageMode&&!skipInvalidLocal){File manual=a.garagePhotoFile(garageBikeId);
   if(manual!=null&&manual.isFile())try{Bitmap user=decodeLocalSampled(manual,1200);
    if(user!=null){target.setImageBitmap(user);credit.setText("내가 선택한 사진 · 탭해서 변경");credit.setOnClickListener(v->a.chooseGaragePhoto());return;}}
   catch(Exception ignored){}
  }
  File file=photoFile(a,brand,model,year);File attribution=new File(file.getAbsolutePath()+".json");
  // An earlier cached automatic image may explicitly describe another model/year.
  // Keep the file recoverable but do not display it as the selected motorcycle.
  JSONObject meta=new JSONObject();
  if(attribution.isFile())try{meta=new JSONObject(readSmall(attribution));}catch(Exception ignored){}
  boolean cacheAllowed=PhotoCacheYearPolicy.canShow(meta.optString("label"),brand,model,year,meta.optBoolean("userSelected"));
  if(file.isFile()&&cacheAllowed&&!skipModelCache){
   final JSONObject cachedMeta=meta;
   final long cachedSize=file.length(),cachedModified=file.lastModified();
   credit.setText("저장된 모델 사진 불러오는 중…");
   try{
    PHOTO_DISK.execute(()->{
     android.graphics.Bitmap cached=decodeLocalSampled(file,1200);
     final android.graphics.Bitmap loaded=cached;
     a.runOnUiThread(()->{
      if(a.isFinishing()||a.isDestroyed()||target.getParent()==null||
         !PhotoRequestGuard.isCurrent(target,requestToken)){
       if(loaded!=null&&!loaded.isRecycled())loaded.recycle();
       return;
      }
      if(loaded!=null&&file.length()==cachedSize&&file.lastModified()==cachedModified){
       String source=cachedMeta.optString("pageUrl");
       credit.setText(source.isEmpty()?(garageMode?"저장된 바이크 사진 · 사진 변경 버튼 사용":"저장된 모델 사진"):
        (cachedMeta.optBoolean("userSelected")?"내가 고른 모델 사진 · ":"저장된 모델 사진 · ")+
        cachedMeta.optString("license","Wikimedia Commons")+" · 출처 보기");
       target.setImageBitmap(loaded);
       if(!source.isEmpty())credit.setOnClickListener(v->a.open(source));
       else if(garageMode)credit.setOnClickListener(v->a.chooseGaragePhoto());
       return;
      }
      if(loaded!=null&&!loaded.isRecycled())loaded.recycle();
      // Corrupt or replaced cache cannot loop back through disk decoding forever.
      showInternal(a,target,credit,brand,model,year,garageBikeId,skipInvalidLocal,true);
     });
    });
    return;
   }catch(java.util.concurrent.RejectedExecutionException busy){
    // A saturated image queue falls back to a bounded online lookup; never block the UI.
   }
  }
  credit.setText(garageMode?"사진 검색 중… · 탭해서 직접 등록 가능":"모델 사진 검색 중…");if(garageMode)credit.setOnClickListener(v->a.chooseGaragePhoto());
  boolean accepted=PhotoLookupQueue.submit(()->{
   // When a card has been replaced while waiting in the queue, skip all HTTP work.
   if(!PhotoRequestGuard.isCurrent(target,requestToken))return;
   PhotoSearchResult lookup=candidateWithStatus(brand,model,year);
   PhotoLoad loaded=lookup.photo;
   JSONObject found=loaded==null?null:loaded.source;
   Bitmap b=loaded==null?null:loaded.bitmap;
   Bitmap manualOverride=null;
   JSONObject manualAttribution=null;
   if(found!=null&&b!=null&&PhotoRequestGuard.isCurrent(target,requestToken))try{
    synchronized(MotorcyclePhoto.class){
     // Never overwrite a model photo deliberately chosen in the gallery with a late automatic lookup.
     boolean selected=false;
     if(attribution.isFile())try{selected=new JSONObject(readSmall(attribution)).optBoolean("userSelected");}catch(Exception ignored){}
     if(selected){
      // A gallery selection made while the network was loading must win.
      Bitmap current=decodeLocalSampled(file,1200);
      if(current!=null){
       manualOverride=current;
       try{manualAttribution=new JSONObject(readSmall(attribution));}catch(Exception ignored){}
      }
     }else{
      file.getParentFile().mkdirs();
      AtomicFile imageDisk=new AtomicFile(file);FileOutputStream imageOut=null;
      try{imageOut=imageDisk.startWrite();if(!b.compress(Bitmap.CompressFormat.JPEG,87,imageOut))throw new IOException("image encode failed");imageDisk.finishWrite(imageOut);imageOut=null;}
      catch(Exception failure){if(imageOut!=null)imageDisk.failWrite(imageOut);throw failure;}
      AtomicFile sourceDisk=new AtomicFile(attribution);FileOutputStream sourceOut=null;
      try{sourceOut=sourceDisk.startWrite();sourceOut.write(found.toString().getBytes("UTF-8"));sourceDisk.finishWrite(sourceOut);sourceOut=null;}
      catch(Exception failure){if(sourceOut!=null)sourceDisk.failWrite(sourceOut);throw failure;}
     }
    }
   }catch(Exception ignored){} // A cache write failure must not hide a downloaded photo.
   final JSONObject data=found;final Bitmap image=b;
   final Bitmap selectedImage=manualOverride;final JSONObject selectedMeta=manualAttribution;
   a.runOnUiThread(()->{if(a.isFinishing()||a.isDestroyed()||target.getParent()==null||!PhotoRequestGuard.isCurrent(target,requestToken)){
    if(image!=null&&!image.isRecycled())image.recycle();
    if(selectedImage!=null&&!selectedImage.isRecycled())selectedImage.recycle();
    return;
   }
    if(selectedImage!=null){
     target.setImageBitmap(selectedImage);
     if(image!=null&&!image.isRecycled())image.recycle();
     String url=selectedMeta==null?"":selectedMeta.optString("pageUrl");
     credit.setText("내가 고른 모델 사진"+(url.startsWith("https://commons.wikimedia.org/")?" · 출처 보기":" · 사진 변경 버튼 사용"));
     if(url.startsWith("https://commons.wikimedia.org/"))credit.setOnClickListener(v->a.open(url));
     else if(garageMode)credit.setOnClickListener(v->a.chooseGaragePhoto());
     return;
    }
    if(data!=null&&image!=null){target.setImageBitmap(image);credit.setText((data.optBoolean("exactYearCandidate")?"연식 검색 후보":"동일 모델 참고 · 연식 미확인")+" · "+data.optString("license")+" · 출처 보기");credit.setOnClickListener(v->a.open(data.optString("pageUrl")));}
    else {
     String reason=PhotoSearchStatus.message(lookup.failure);
     if(garageMode){
      credit.setText(reason+" · 탭해서 다시 검색/직접 등록");
      credit.setOnClickListener(v->new android.app.AlertDialog.Builder(a)
       .setTitle("바이크 사진")
       .setItems(new String[]{"사진 다시 검색","내 사진 등록"},(dialog,choice)->{
        if(choice==0)show(a,target,credit,brand,model,year,garageBikeId);
        else a.chooseGaragePhoto();
       }).show());
     }else{
      credit.setText(reason+" · 탭해서 다시 검색");
      credit.setOnClickListener(v->show(a,target,credit,brand,model,year,null));
     }
    }
   });
  });
  if(!accepted&&PhotoRequestGuard.isCurrent(target,requestToken)){
   credit.setText("사진 요청이 많아요 · 탭해서 다시 검색"+(garageMode?"/직접 등록":""));
   if(garageMode)credit.setOnClickListener(v->new android.app.AlertDialog.Builder(a)
    .setTitle("바이크 사진").setItems(new String[]{"사진 다시 검색","내 사진 등록"},(dialog,choice)->{
     if(choice==0)show(a,target,credit,brand,model,year,garageBikeId);
     else a.chooseGaragePhoto();
    }).show());
   else credit.setOnClickListener(v->show(a,target,credit,brand,model,year,null));
  }
 }
 static String readSmall(File f)throws IOException{try(InputStream in=new FileInputStream(f)){byte[] b=new byte[(int)Math.min(f.length(),8192)];int len=in.read(b);return len<=0?"":new String(b,0,len,"UTF-8");}}
}
