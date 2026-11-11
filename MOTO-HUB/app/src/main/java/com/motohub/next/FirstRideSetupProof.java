package com.motohub.next;
import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
/** Verified first automatic GPS ride for the CURRENT user-selected hardware
 * configuration. Does not touch any garage ODO or the underlying track. */
final class FirstRideSetupProof {
 private FirstRideSetupProof(){}
 static boolean sufficient(boolean marker,int points,double gpsKm){
  return marker&&points>=2&&Double.isFinite(gpsKm)&&gpsKm>=0.10;
 }
 private static SharedPreferences prefs(Context c,String bike){
  return c.getSharedPreferences("first_auto_ride_verified_v603_"+bike,0);
 }
 private static String fingerprint(Context c,String bike){
  String raw=ConnectionSetupMode.get(c,bike).name()+"|"+
   BikeObdPreferences.address(c,bike).toUpperCase(Locale.ROOT)+"|"+
   (CardoAutoRidePrefs.forBike(c,bike)?CardoAutoRidePrefs.address(c).toUpperCase(Locale.ROOT):"")+
   "|"+BikeObdPreferences.gpsFallbackEnabled(c,bike);
  try{
   byte[] digest=MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
   StringBuilder out=new StringBuilder();
   for(byte b:digest)out.append(String.format(Locale.ROOT,"%02x",b&255));
   return out.toString();
  }catch(Exception error){return "";}
 }
 static String attemptReason(File track,boolean verified,int points,double gpsKm){
  if(!verified||track==null||!track.isFile()||
   !new File(track.getPath()+".done").isFile())
   return "GPS 파일 저장 검증 실패 · 원본 복구 화면에서 확인";
  if(points<2)return "실제 GPS 좌표가 2개 미만 · 위치 권한/수신 확인";
  if(!Double.isFinite(gpsKm)||gpsKm<0)return "GPS 이동거리 계산 실패 · 위치 신호 검사";
  if(gpsKm<0.10)return "GPS 이동거리 0.1km 미만 · 짧은 시험 주행";
  return "GPS 경로·이동거리 정상 검증";
 }
 static void reportCompletion(Context c,String bike,File track,boolean fileVerified,int points,double km){
  if(!RideBikeBinding.valid(bike))return;
  String reason=attemptReason(track,fileVerified,points,km);
  boolean accepted=fileVerified&&confirm(c,bike,track,points,km);
  prefs(c,bike).edit().putString("attempt_fingerprint",fingerprint(c,bike))
   .putString("attempt_reason",accepted?"GPS 경로·이동거리 정상 검증":reason)
   .putLong("attempt_at",System.currentTimeMillis()).commit();
 }
 static boolean confirm(Context c,String bike,File track,int points,double gpsKm){
  if(!RideBikeBinding.valid(bike)||track==null||!track.isFile()||
     !new File(track.getPath()+".done").isFile()||
     !sufficient(true,points,gpsKm))return false;
  String fingerprint=fingerprint(c,bike);
  if(fingerprint.isEmpty())return false;
  return prefs(c,bike).edit().putString("fingerprint",fingerprint)
   .putLong("verifiedAt",System.currentTimeMillis())
   .putInt("points",Math.max(2,points))
   .putFloat("gpsKm",(float)gpsKm).commit();
 }
 static boolean verified(Context c,String bike){
  if(!RideBikeBinding.valid(bike))return false;
  SharedPreferences p=prefs(c,bike);
  String value=fingerprint(c,bike);
  return !value.isEmpty()&&value.equals(p.getString("fingerprint",""))&&
   p.getLong("verifiedAt",0)>0&&p.getInt("points",0)>=2&&p.getFloat("gpsKm",0)>=.10f;
 }
 static String summary(Context c,String bike){
  if(!RideBikeBinding.valid(bike))return "첫 주행 확인: 바이크 선택 필요";
  if(!verified(c,bike)){
   SharedPreferences past=prefs(c,bike);
   String reason=past.getString("attempt_reason","");
   String fingerprint=fingerprint(c,bike);
   if(!reason.isEmpty()&&fingerprint.equals(past.getString("attempt_fingerprint","")))
    return "첫 주행 검증 미완료 · "+reason+"\n정상 GPS 저장과 0.1km 이상 실제 이동 필요";
   return "첫 주행 검증 전 · 자동기록 후 정상 GPS 저장과 0.1km 이상 실제 이동 필요";
  }
  SharedPreferences p=prefs(c,bike);
  java.text.SimpleDateFormat fmt=new java.text.SimpleDateFormat("MM-dd HH:mm",Locale.KOREA);
  String result="첫 자동기록 검증 완료 · 실제 GPS 파일 저장 "+
   fmt.format(new java.util.Date(p.getLong("verifiedAt",0)))+
   " · "+p.getInt("points",0)+"좌표 · "+String.format(Locale.KOREA,"%.2f",p.getFloat("gpsKm",0))+"km";
  long last=p.getLong("attempt_at",0);
  if(last>p.getLong("verifiedAt",0)){
   String reason=p.getString("attempt_reason","");
   if(!reason.isEmpty())result+="\n최근 검사: "+reason;
  }
  return result;
 }
}
