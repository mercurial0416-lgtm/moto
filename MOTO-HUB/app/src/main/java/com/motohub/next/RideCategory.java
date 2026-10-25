package com.motohub.next;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/** Local, user-controlled labels. A heuristic must never infer home/work destinations. */
final class RideCategory {
 static final String AUTO="자동 분류",DAILY="일상",COMMUTE="출퇴근",WINDING="와인딩",LONG="장거리",TOUR="투어";
 static final String[] OPTIONS={AUTO,DAILY,COMMUTE,WINDING,LONG,TOUR};
 private RideCategory(){}
 static boolean valid(String category){
  if(category==null)return false;
  for(String item:OPTIONS)if(item.equals(category))return true;
  return false;
 }
 static String suggested(double km,long durationMs,double averageKmh){
  if(!Double.isFinite(km)||km<0||durationMs<0||!Double.isFinite(averageKmh)||averageKmh<0)return DAILY;
  if(km>=100)return LONG;
  if(km>=25&&durationMs>=45*60_000L&&averageKmh<=48)return WINDING+" (추정)";
  return DAILY;
 }
 static String label(File track,RideInsights.Result info){
  String saved=read(track);
  return AUTO.equals(saved)?suggested(info.distanceKm,info.durationMillis,info.averageKmh):saved;
 }
 static String read(File track){
  File sidecar=new File(track.getPath()+".category");
  if(!sidecar.isFile())return AUTO;
  try{
   if(sidecar.length()>64)return AUTO;
   String stored=new String(Files.readAllBytes(sidecar.toPath()),StandardCharsets.UTF_8);
   return valid(stored)?stored:AUTO;
  }catch(IOException ignored){return AUTO;}
 }
 static void write(File track,String category)throws IOException {
  if(!valid(category))throw new IOException("올바르지 않은 라이딩 분류");
  File parent=track.getCanonicalFile().getParentFile();
  if(parent==null||!"rides".equals(parent.getName())||!track.getName().matches("[0-9]{10,15}-[a-f0-9-]{36}\\.track")||!track.isFile())
   throw new IOException("저장된 로컬 라이딩만 분류할 수 있어요.");
  File sidecar=new File(track.getPath()+".category");
  if(AUTO.equals(category)){if(sidecar.isFile()&&!sidecar.delete())throw new IOException("자동 분류로 초기화하지 못했어요.");return;}
  File temporary=new File(sidecar.getPath()+".writing");
  try{
   try(java.io.FileOutputStream out=new java.io.FileOutputStream(temporary)){
    out.write(category.getBytes(StandardCharsets.UTF_8));out.getFD().sync();
   }
   try{Files.move(temporary.toPath(),sidecar.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
   catch(java.nio.file.AtomicMoveNotSupportedException ex){Files.move(temporary.toPath(),sidecar.toPath(),StandardCopyOption.REPLACE_EXISTING);}
  }finally{temporary.delete();}
 }
}
