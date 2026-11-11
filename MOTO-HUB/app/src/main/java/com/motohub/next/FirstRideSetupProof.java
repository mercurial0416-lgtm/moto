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
  if(!verified(c,bike))return "첫 주행 검증 전 · 자동기록으로 출발한 뒤 GPS 파일이 실제 저장되면 완료 표시";
  SharedPreferences p=prefs(c,bike);
  java.text.SimpleDateFormat fmt=new java.text.SimpleDateFormat("MM-dd HH:mm",Locale.KOREA);
  return "첫 자동기록 검증 완료 · 실제 GPS 파일 저장 "+fmt.format(new java.util.Date(p.getLong("verifiedAt",0)))+
   " · "+p.getInt("points",0)+"좌표 · "+String.format(Locale.KOREA,"%.2f",p.getFloat("gpsKm",0))+"km";
 }
}
