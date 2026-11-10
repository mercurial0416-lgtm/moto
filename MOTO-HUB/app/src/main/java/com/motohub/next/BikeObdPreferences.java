package com.motohub.next;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/** Local, per-bike OBD device and selected live PID identifiers.
 * Never changes vehicle data, ECU settings, GPS tracks or garage odometer.
 * Hardware pairing remains in Android Bluetooth settings.
 */
final class BikeObdPreferences {
 private static final String FILE="bike_obd_preferences";
 private BikeObdPreferences(){}
 private static SharedPreferences prefs(Context context){return context.getSharedPreferences(FILE,Context.MODE_PRIVATE);}
 private static String key(String bikeId,String suffix){
  if(!RideBikeBinding.valid(bikeId))throw new IllegalArgumentException("등록된 바이크 식별자가 필요합니다.");
  return bikeId+"."+suffix;
 }
 static boolean validAddress(String address){
  return address!=null&&address.matches("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}");
 }
 static String address(Context context,String bikeId){
  if(!RideBikeBinding.valid(bikeId))return "";
  String result=prefs(context).getString(key(bikeId,"mac"),"");
  return validAddress(result)?result:"";
 }
 static String deviceName(Context context,String bikeId){
  if(!RideBikeBinding.valid(bikeId))return "";
  return prefs(context).getString(key(bikeId,"name"),"");
 }
 static void setDevice(Context context,String bikeId,String address,String name){
  if(!validAddress(address))throw new IllegalArgumentException("정상적인 Bluetooth 장치 주소가 아닙니다.");
  String safe=name==null||name.trim().isEmpty()?"OBD2 동글":name.trim();
  if(safe.length()>80)safe=safe.substring(0,80);
  SharedPreferences.Editor edit=prefs(context).edit();
  if(!address.equalsIgnoreCase(address(context,bikeId))){
   edit.remove(key(bikeId,"autoRide")).remove(key(bikeId,"gpsFallback"));
   // Auto ride consent belongs to a specific OBD hardware identity.
   // Changing the dongle invalidates the headset-triggered session too.
   if(bikeId.equals(CardoAutoRidePrefs.bikeId(context))){
    CardoCompanionPairing.stopObserving(context);
    CardoAutoRidePrefs.disable(context);
   }
  }
  edit.putString(key(bikeId,"mac"),address.toUpperCase(Locale.ROOT))
   .putString(key(bikeId,"name"),safe).apply();
 }
 static void clearDevice(Context context,String bikeId){
  if(RideBikeBinding.valid(bikeId)&&bikeId.equals(CardoAutoRidePrefs.bikeId(context))){
   CardoCompanionPairing.stopObserving(context);
   CardoAutoRidePrefs.disable(context);
  }
  prefs(context).edit().remove(key(bikeId,"mac")).remove(key(bikeId,"name"))
   .remove(key(bikeId,"autoRide")).remove(key(bikeId,"gpsFallback")).apply();
 }
 static List<String> selectedPids(Context context,String bikeId){
  if(!RideBikeBinding.valid(bikeId))return new ArrayList<>();
  String csv=prefs(context).getString(key(bikeId,"selected"),"");
  LinkedHashSet<String> set=new LinkedHashSet<>();
  for(String pid:csv.split(",")){
   if(pid.matches("01[0-9A-F]{2}")&&!"01FF".equals(pid))set.add(pid);
   if(set.size()>=250)break;
  }
  return new ArrayList<>(set);
 }
 static void setSelectedPids(Context context,String bikeId,List<String> pids){
  if(pids==null)throw new IllegalArgumentException("PID 목록이 없습니다.");
  LinkedHashSet<String> clean=new LinkedHashSet<>();
  for(String pid:pids){
   if(pid==null||!pid.matches("01[0-9A-F]{2}")||"01FF".equals(pid))
    throw new IllegalArgumentException("허용되지 않은 PID입니다.");
   clean.add(pid);
   if(clean.size()>250)throw new IllegalArgumentException("너무 많은 PID가 선택됐습니다.");
  }
  prefs(context).edit().putString(key(bikeId,"selected"),String.join(",",clean)).apply();
 }
 static void clearSelectedPids(Context context,String bikeId){
  prefs(context).edit().remove(key(bikeId,"selected")).apply();
 }
 /** Persistent opt-in flag. Never starts a background service by itself. */
 static boolean autoRideEnabled(Context context,String bikeId){
  return RideBikeBinding.valid(bikeId)&&prefs(context).getBoolean(key(bikeId,"autoRide"),false);
 }
 static void setAutoRideEnabled(Context context,String bikeId,boolean enabled){
  if(!RideBikeBinding.valid(bikeId))throw new IllegalArgumentException("등록된 바이크가 필요합니다.");
  if(enabled&&address(context,bikeId).isEmpty())throw new IllegalStateException("먼저 이 바이크에 OBD 동글을 등록해 주세요.");
  prefs(context).edit().putBoolean(key(bikeId,"autoRide"),enabled).apply();
 }
 /** Disabled by default. Explicit, per-bike GPS energy and false-positive consent. */
 static boolean gpsFallbackEnabled(Context context,String bikeId){
  return RideBikeBinding.valid(bikeId)&&
   CardoAutoRidePrefs.forBike(context,bikeId)&&
   prefs(context).getBoolean(key(bikeId,"gpsFallback"),false);
 }
 static void setGpsFallbackEnabled(Context context,String bikeId,boolean enabled){
  if(!RideBikeBinding.valid(bikeId))throw new IllegalArgumentException("바이크 선택 필요");
  if(enabled&&!CardoAutoRidePrefs.forBike(context,bikeId))
   throw new IllegalStateException("카르도 장치를 먼저 등록해 주세요");
  prefs(context).edit().putBoolean(key(bikeId,"gpsFallback"),enabled).commit();
 }
 static void clearBike(Context context,String bikeId){
  if(!RideBikeBinding.valid(bikeId))return;
  prefs(context).edit().remove(key(bikeId,"mac")).remove(key(bikeId,"name"))
   .remove(key(bikeId,"selected")).remove(key(bikeId,"autoRide"))
   .remove(key(bikeId,"gpsFallback")).apply();
 }

}