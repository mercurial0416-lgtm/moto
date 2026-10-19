package com.motohub.next;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.util.AtomicFile;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/** Explicit, opt-in Commons image picker. No image or location data is uploaded by the app. */
final class MotorcyclePhotoGallery {
 private MotorcyclePhotoGallery(){}
 private static final int MAX_CHOICES=6;
 // Cap concurrent bitmap downloads instead of spawning six unbounded image threads.
 private static final java.util.concurrent.ExecutorService PHOTO_IO=java.util.concurrent.Executors.newFixedThreadPool(2,r->{
  Thread t=new Thread(r,"moto-gallery-image");t.setDaemon(true);return t;
 });

 static void choose(MainActivity a,String brand,String model,int year){
  new AlertDialog.Builder(a).setTitle("바이크 사진 변경")
   .setItems(new String[]{"온라인 모델 사진 고르기", "휴대폰에서 사진 등록"},(dialog,which)->{
    if(which==0)open(a,brand,model,year);else a.chooseGaragePhoto();
   }).show();
 }

 static void open(MainActivity a,String brand,String model,int year){
  if(brand==null||brand.trim().isEmpty()||model==null||model.trim().isEmpty()){
   a.toast("브랜드와 모델을 먼저 선택해 주세요.");return;
  }
  LinearLayout body=a.col();body.setPadding(a.dp(16),a.dp(12),a.dp(16),a.dp(12));
  TextView info=a.text("Wikimedia Commons 사진 검색 중…",14,a.fg,false);body.addView(info);
  TextView notice=a.text("사진 제목에 나온 연식은 실제 연식 인증이 아니에요. 원하는 사진을 직접 선택하세요.",12,a.muted,false);
  LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2);np.topMargin=a.dp(8);body.addView(notice,np);
  LinearLayout rows=a.col();LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.topMargin=a.dp(12);body.addView(rows,rp);
  LinearLayout recovery=a.col();
  LinearLayout.LayoutParams recoveryParams=new LinearLayout.LayoutParams(-1,-2);
  recoveryParams.topMargin=a.dp(12);body.addView(recovery,recoveryParams);
  recovery.setVisibility(View.GONE);
  ScrollView scroll=new ScrollView(a);scroll.addView(body);
  AlertDialog dialog=new AlertDialog.Builder(a).setTitle("온라인 모델 사진 갤러리")
   .setView(scroll).setNegativeButton("닫기",null).create();
  AtomicBoolean canceled=new AtomicBoolean(false);
  PhotoGalleryLoadGate gate=new PhotoGalleryLoadGate();
  dialog.setOnDismissListener(d->{canceled.set(true);gate.close();});
  Button retry=a.button("다시 검색",false,()->loadChoices(a,brand,model,year,info,rows,recovery,dialog,canceled,gate));
  retry.setContentDescription("온라인 모델 사진 다시 검색");
  recovery.addView(retry,new LinearLayout.LayoutParams(-1,-2));
  Button local=a.button("휴대폰에서 사진 등록",true,()->{dialog.dismiss();a.chooseGaragePhoto();});
  LinearLayout.LayoutParams localParams=new LinearLayout.LayoutParams(-1,-2);
  localParams.topMargin=a.dp(8);recovery.addView(local,localParams);
  dialog.show();
  loadChoices(a,brand,model,year,info,rows,recovery,dialog,canceled,gate);
 }

 /** A retry invalidates earlier asynchronous search results instead of letting them overwrite the new state. */
 private static void loadChoices(MainActivity a,String brand,String model,int year,TextView info,
                                 LinearLayout rows,LinearLayout recovery,AlertDialog dialog,
                                 AtomicBoolean canceled,PhotoGalleryLoadGate gate){
  final int request=gate.begin();
  if(request<0)return;
  rows.removeAllViews();recovery.setVisibility(View.GONE);
  info.setText("Wikimedia Commons 사진 검색 중…");
  new Thread(()->{
   final List<JSONObject> choices;
   final String error;
   List<JSONObject> found=null;String problem=null;
   try{found=candidates(brand,model,year);}catch(Exception ex){problem="사진 검색에 실패했어요. 네트워크를 확인해 주세요.";}
   choices=found;error=problem;
   a.runOnUiThread(()->{
    if(canceled.get()||!gate.accepts(request)||a.isFinishing()||a.isDestroyed())return;
    rows.removeAllViews();
    if(error!=null||choices==null||choices.isEmpty()){
     info.setText(error!=null?error:"일치하는 모델 사진이 없어요. 다른 검색을 시도하거나 휴대폰 사진을 등록하세요.");
     recovery.setVisibility(View.VISIBLE);
     return;
    }
    recovery.setVisibility(View.GONE);
    info.setText("검색 결과 "+choices.size()+"장 · 사진을 눌러 적용");
    for(JSONObject choice:choices)addChoice(a,rows,dialog,canceled,choice,brand,model,year);
   });
  },"moto-photo-gallery-search").start();
 }

 /** Keep year-labelled results first, remove duplicate Wikimedia file URLs. */
 static List<JSONObject> candidates(String brand,String model,int year)throws Exception{
  List<JSONObject> out=new ArrayList<>();Set<String> seen=new HashSet<>();
  if(year>=1900&&year<=2200){
   try{append(out,seen,MotorcyclePhoto.searchCommons(year+" "+brand+" "+model,brand,model,year,true));}
   catch(Exception ignored){/* The non-year search may still succeed. */}
  }
  if(out.size()<MAX_CHOICES)append(out,seen,MotorcyclePhoto.searchCommons(brand+" "+model+" motorcycle",brand,model,year,false));
  if(out.isEmpty())append(out,seen,MotorcyclePhoto.searchCommons(brand+" "+model,brand,model,year,false));
  return out;
 }
 static void append(List<JSONObject> out,Set<String> seen,List<JSONObject> incoming){
  PhotoGalleryChoices.append(out,seen,incoming,item->item.optString("imageUrl"),MAX_CHOICES);
 }

 private static void addChoice(MainActivity a,LinearLayout rows,AlertDialog dialog,AtomicBoolean canceled,
                               JSONObject choice,String brand,String model,int year){
  LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.HORIZONTAL);
  row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(a.dp(10),a.dp(10),a.dp(10),a.dp(10));
  row.setBackground(a.shape(a.panel,a.line,12));
  ImageView image=new ImageView(a);image.setScaleType(ImageView.ScaleType.CENTER_CROP);
  image.setImageResource(R.drawable.garage_model_placeholder);
  row.addView(image,new LinearLayout.LayoutParams(a.dp(110),a.dp(84)));
  LinearLayout details=a.col();details.setPadding(a.dp(12),0,0,0);
  TextView name=a.text(choice.optString("label","모델 사진"),13,a.fg,true);name.setMaxLines(2);details.addView(name);
  TextView caption=a.text((choice.optBoolean("exactYearCandidate")?"연식 검색 후보":"연식 미확인")+" · "+choice.optString("license","Wikimedia Commons"),11,a.muted,false);
  LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.topMargin=a.dp(5);details.addView(caption,cp);
  TextView previewStatus=a.text("미리보기 불러오는 중…",11,a.muted,false);details.addView(previewStatus);
  row.addView(details,new LinearLayout.LayoutParams(0,-2,1));
  LinearLayout.LayoutParams rowp=new LinearLayout.LayoutParams(-1,-2);rowp.bottomMargin=a.dp(9);rows.addView(row,rowp);
  row.setContentDescription("모델 사진 선택: "+choice.optString("label"));
  row.setClickable(true);row.setFocusable(true);
  final Bitmap[] preview={null};
  row.setOnClickListener(v->{
   if(canceled.get())return;
   row.setEnabled(false);caption.setText("사진 적용 중…");
   PHOTO_IO.execute(()->{
    String problem=null;
    try{
     // A low-resolution thumbnail must never become the saved full-size model image.
     Bitmap bitmap=MotorcyclePhoto.downloadImage(choice.optString("imageUrl"));
     if(bitmap==null)throw new java.io.IOException("사진을 내려받지 못했어요.");
     if(!canceled.get())save(a,brand,model,year,choice,bitmap);
    }catch(Exception ex){problem="사진 저장 실패: "+(ex.getMessage()==null?"다시 시도해 주세요.":ex.getMessage());}
    final String failure=problem;
    a.runOnUiThread(()->{
     if(canceled.get()||a.isFinishing()||a.isDestroyed())return;
     if(failure==null){dialog.dismiss();a.toast("모델 사진을 변경했어요.");a.render();}
     else{caption.setText(failure);row.setEnabled(true);}
    });
   });
  });
  PHOTO_IO.execute(()->{
   Bitmap bitmap=null;
   try{bitmap=MotorcyclePhoto.downloadPreview(choice.optString("imageUrl"));}catch(Exception ignored){}
   final Bitmap ready=bitmap;
   a.runOnUiThread(()->{if(canceled.get()||a.isFinishing()||a.isDestroyed())return;
    if(ready!=null){image.setImageBitmap(ready);previewStatus.setText("미리보기 준비됨");}
    else{previewStatus.setText("미리보기 실패 · 선택하면 다시 시도");image.setContentDescription("사진 미리보기 불러오기 실패");}
   });
  });
 }

 /** Commit a gallery choice without deleting the previous valid model photo first. */
 static void save(MainActivity a,String brand,String model,int year,JSONObject source,Bitmap bitmap)throws Exception{
  // Serialize with automatic Commons cache writes so a delayed lookup cannot undo a user's choice.
  synchronized(MotorcyclePhoto.class){
   File photo=MotorcyclePhoto.photoFile(a,brand,model,year);
   if(!photo.getParentFile().isDirectory()&&!photo.getParentFile().mkdirs())throw new java.io.IOException("사진 폴더 생성 실패");
   File metadata=new File(photo.getAbsolutePath()+".json");
   // Retain the previous attribution if JPEG encoding fails before committing the new choice.
   byte[] previous=metadata.isFile()&&metadata.length()<=8192?java.nio.file.Files.readAllBytes(metadata.toPath()):null;
   JSONObject selected=new JSONObject(source.toString());selected.put("userSelected",true);
   AtomicFile metaFile=new AtomicFile(metadata);FileOutputStream metaOut=null;
   try{metaOut=metaFile.startWrite();metaOut.write(selected.toString().getBytes(StandardCharsets.UTF_8));metaFile.finishWrite(metaOut);metaOut=null;}
   catch(Exception failure){if(metaOut!=null)metaFile.failWrite(metaOut);throw failure;}
   AtomicFile photoFile=new AtomicFile(photo);FileOutputStream photoOut=null;
   try{photoOut=photoFile.startWrite();if(!bitmap.compress(Bitmap.CompressFormat.JPEG,87,photoOut))throw new java.io.IOException("사진 변환 실패");
    if(photoOut.getChannel().size()>8_000_000L)throw new java.io.IOException("사진 용량이 8MB를 초과했어요.");
    photoFile.finishWrite(photoOut);photoOut=null;}
   catch(Exception failure){
    if(photoOut!=null)photoFile.failWrite(photoOut);
    // The old image is intact; restore its matching metadata as well.
    if(previous==null)metadata.delete();
    else{FileOutputStream restore=null;try{restore=metaFile.startWrite();restore.write(previous);metaFile.finishWrite(restore);}catch(Exception rollback){if(restore!=null)metaFile.failWrite(restore);failure.addSuppressed(rollback);}}
    throw failure;
   }
  }
 }
}
