package com.motohub.next;

import android.content.Context;
import android.graphics.*;
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
 static String key(String brand,String model,int year){return MotorcycleCatalog.norm(brand)+"_"+MotorcycleCatalog.norm(model)+"_"+year;}
 static String tinyName(String k){try{byte[] b=MessageDigest.getInstance("SHA-256").digest(k.getBytes("UTF-8"));StringBuilder s=new StringBuilder();for(byte x:b)s.append(String.format(Locale.ROOT,"%02x",x));return s.substring(0,24);}catch(Exception e){return Integer.toHexString(k.hashCode());}}
 static File photoFile(Context c,String brand,String model,int year){File folder=new File(c.getCacheDir(),"moto-year-photos");return new File(folder,tinyName(key(brand,model,year))+".jpg");}
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
 static PhotoLoad candidate(String brand,String model,int year){
  PhotoLoad loaded=null;
  if(year>=1900&&year<=2200)
   loaded=firstDownload(year+" "+brand+" "+model,brand,model,year,true);
  if(loaded!=null)return loaded;
  // If an exact-year candidate fails to download, still try the same model without year.
  return firstDownload(brand+" "+model+" motorcycle",brand,model,year,false);
 }
 static PhotoLoad firstDownload(String query,String brand,String model,int year,boolean exactYear){
  try {
   return PhotoRetry.firstSuccess(searchCommons(query,brand,model,year,exactYear),3,meta->{
    Bitmap bitmap=downloadImage(meta.optString("imageUrl"));
    return bitmap==null?null:new PhotoLoad(meta,bitmap);
   });
  }catch(Exception ignored){return null;}
 }
 static List<JSONObject> searchCommons(String search,String brand,String model,int year,boolean exactYear)throws Exception{
  List<JSONObject> candidates=new ArrayList<>();
  URL url=new URL("https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrsearch="+URLEncoder.encode(search,"UTF-8")+"&gsrnamespace=6&gsrlimit=12&prop=imageinfo&iiprop=url%7Cmime%7Cextmetadata&iiurlwidth=1100&format=json");
  HttpURLConnection c=(HttpURLConnection)url.openConnection();try{
   c.setConnectTimeout(6000);c.setReadTimeout(8000);c.setRequestProperty("User-Agent","MOTO-HUB/4.0 motorcycle photo (https://commons.wikimedia.org/)");
   if(c.getResponseCode()!=200)return candidates;
   try(InputStream in=c.getInputStream()){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){if(out.size()+n>250000)throw new IOException("image result too big");out.write(buf,0,n);}
    JSONObject pages=new JSONObject(out.toString("UTF-8")).optJSONObject("query");if(pages==null)return candidates;pages=pages.optJSONObject("pages");if(pages==null)return candidates;
    for(Iterator<String> keys=pages.keys();keys.hasNext();){JSONObject page=pages.optJSONObject(keys.next());if(page==null)continue;
      String title=page.optString("title");if(exactYear?!matches(title,brand,model,year):!matchesModel(title,brand,model))continue;
      JSONArray images=page.optJSONArray("imageinfo");JSONObject im=images==null?null:images.optJSONObject(0);if(im==null||!PhotoMimeFilter.supported(im.optString("mime")))continue;
      String imageUrl=im.optString("thumburl",im.optString("url"));String pageUrl=im.optString("descriptionurl");
      if(!imageUrl.startsWith("https://upload.wikimedia.org/")||!pageUrl.startsWith("https://commons.wikimedia.org/"))continue;
      JSONObject ext=im.optJSONObject("extmetadata");String license=ext==null?"":ext.optJSONObject("LicenseShortName")==null?"":ext.optJSONObject("LicenseShortName").optString("value");
      if(license.isEmpty())continue;
      JSONObject candidate=new JSONObject();candidate.put("imageUrl",imageUrl);candidate.put("pageUrl",pageUrl);candidate.put("label",title);candidate.put("license",license);candidate.put("exactYearCandidate",exactYear);candidates.add(candidate);
    }
    return candidates;
   }
  }finally{c.disconnect();}
 }
 static Bitmap downloadImage(String s)throws Exception{
  URL url=new URL(s);if(!"https".equals(url.getProtocol())||!"upload.wikimedia.org".equals(url.getHost()))return null;
  HttpURLConnection c=(HttpURLConnection)url.openConnection();try{c.setConnectTimeout(7000);c.setReadTimeout(10000);c.setInstanceFollowRedirects(false);
   if(c.getResponseCode()!=200)return null;int length=c.getContentLength();if(length>4500000)return null;
   try(InputStream in=c.getInputStream()){
    ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] chunk=new byte[4096];int n;while((n=in.read(chunk))!=-1){if(out.size()+n>4500000)return null;out.write(chunk,0,n);}
    byte[] b=out.toByteArray();BitmapFactory.Options opts=new BitmapFactory.Options();opts.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(b,0,b.length,opts);if(opts.outWidth<=0||opts.outWidth>9000||opts.outHeight<=0||opts.outHeight>9000)return null;
    opts.inJustDecodeBounds=false;opts.inSampleSize=1;while(opts.outWidth/opts.inSampleSize>1300||opts.outHeight/opts.inSampleSize>1300)opts.inSampleSize*=2;return BitmapFactory.decodeByteArray(b,0,b.length,opts);
   }
  }finally{c.disconnect();}
 }
 /** Main thread only. Each lookup owns a generation token, so stale responses cannot overwrite newer selections. */
 static void show(MainActivity a,ImageView target,TextView credit,String brand,String model,int year){
  // Invalidate all older requests even when the new selection has no photo.
  long requestToken=PhotoRequestGuard.begin(target);
  target.setImageDrawable(null);
  credit.setOnClickListener(null); // Never leave a previous model's source link active.
  if(brand.isEmpty()||model.isEmpty()) {credit.setText("모델 정보 확인 필요 · 사진 직접 등록 가능");return;}
  File file=photoFile(a,brand,model,year);File attribution=new File(file.getAbsolutePath()+".json");
  if(file.isFile()&&attribution.isFile())try{Bitmap b=BitmapFactory.decodeFile(file.getAbsolutePath());if(b!=null){JSONObject meta=new JSONObject(readSmall(attribution));target.setImageBitmap(b);credit.setText((meta.optBoolean("exactYearCandidate")?"연식 검색 후보":"동일 모델 참고 · 연식 미확인")+" · "+meta.optString("license")+" · 출처 보기");credit.setOnClickListener(v->a.open(meta.optString("pageUrl")));return;}}catch(Exception ignored){}
  credit.setText("바이크 사진 검색 중… · 일치 후보만 표시");
  new Thread(()->{
   PhotoLoad loaded=candidate(brand,model,year);
   JSONObject found=loaded==null?null:loaded.source;
   Bitmap b=loaded==null?null:loaded.bitmap;
   if(found!=null&&b!=null)try{
    file.getParentFile().mkdirs();try(OutputStream out=new FileOutputStream(file)){b.compress(Bitmap.CompressFormat.JPEG,87,out);}
    try(OutputStream out=new FileOutputStream(attribution)){out.write(found.toString().getBytes("UTF-8"));}
   }catch(Exception ignored){} // A cache write failure must not hide a downloaded photo.
   final JSONObject data=found;final Bitmap image=b;
   a.runOnUiThread(()->{if(a.isFinishing()||target.getParent()==null||!PhotoRequestGuard.isCurrent(target,requestToken))return;
    if(data!=null&&image!=null){target.setImageBitmap(image);credit.setText((data.optBoolean("exactYearCandidate")?"연식 검색 후보":"동일 모델 참고 · 연식 미확인")+" · "+data.optString("license")+" · 출처 보기");credit.setOnClickListener(v->a.open(data.optString("pageUrl")));}
    else credit.setText("모델 사진을 찾지 못했어요 · 사진 변경으로 직접 등록");
   });
  },"moto-commons-image").start();
 }
 static String readSmall(File f)throws IOException{try(InputStream in=new FileInputStream(f)){byte[] b=new byte[(int)Math.min(f.length(),8192)];int len=in.read(b);return len<=0?"":new String(b,0,len,"UTF-8");}}
}
