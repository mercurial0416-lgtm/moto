package com.motohub.next;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
/**
 * Per-bike, locally confirmed ODO inbox. ECU samples never modify garage directly.
 * Legacy single-bike baseline is migrated without losing pending verified km.
 */
final class OdometerAutoSyncInbox {
 private static final String PREF="odo_auto_sync";
 private OdometerAutoSyncInbox(){}
 private static SharedPreferences p(Context context){return context.getSharedPreferences(PREF,Context.MODE_PRIVATE);}
 private static String k(String id,String field){return id+"."+field;}
 private static List<String> bikeIds(Context c){
  LinkedHashSet<String> ids=new LinkedHashSet<>();
  for(String id:p(c).getString("bikeIds","").split(","))if(RideBikeBinding.valid(id))ids.add(id);
  return new ArrayList<>(ids);
 }
 private static void migrateLegacy(Context c){
  SharedPreferences pref=p(c);
  String oldId=pref.getString("bikeId","");
  if(!RideBikeBinding.valid(oldId))return;
  SharedPreferences.Editor e=pref.edit();
  List<String> ids=bikeIds(c);
  if(!ids.contains(oldId))ids.add(oldId);
  for(String field:new String[]{"enabled","baseline","last","lastAt","pending","lastNote"}){
   if(!pref.contains(field))continue;
   String target=k(oldId,field);
   if(!pref.contains(target)){
    if("enabled".equals(field))e.putBoolean(target,pref.getBoolean(field,false));
    else if("lastAt".equals(field))e.putLong(target,pref.getLong(field,0));
    else e.putString(target,pref.getString(field,""));
   }
   e.remove(field);
  }
  e.remove("bikeId").putString("bikeIds",String.join(",",ids)).commit();
 }
 static synchronized String configuredBikeId(Context c){
  migrateLegacy(c);
  String first="";
  for(String id:bikeIds(c))if(enabled(c,id)){
   if(first.isEmpty())first=id;
   if(OdoAutoSyncPolicy.sane(pending(c,id)))return id;
  }
  return first;
 }
 static synchronized boolean enabled(Context c,String bikeId){
  migrateLegacy(c);
  return RideBikeBinding.valid(bikeId)&&p(c).getBoolean(k(bikeId,"enabled"),false);
 }
 static synchronized boolean enable(Context c,String bikeId,double checkedDashboardKm){
  if(!RideBikeBinding.valid(bikeId)||!OdoAutoSyncPolicy.sane(checkedDashboardKm))return false;
  migrateLegacy(c);
  List<String> ids=bikeIds(c);if(!ids.contains(bikeId))ids.add(bikeId);
  return p(c).edit().putString("bikeIds",String.join(",",ids))
   .putBoolean(k(bikeId,"enabled"),true)
   .putString(k(bikeId,"baseline"),Double.toString(checkedDashboardKm))
   .remove(k(bikeId,"last")).remove(k(bikeId,"lastAt"))
   .remove(k(bikeId,"pending")).remove(k(bikeId,"lastNote")).commit();
 }
 /** Legacy single-selection API: disable only its originally selected bike. */
 static synchronized void disable(Context c){
  String id=configuredBikeId(c);if(RideBikeBinding.valid(id))disable(c,id);
 }
 static synchronized void disable(Context c,String bikeId){
  if(!RideBikeBinding.valid(bikeId))return;
  migrateLegacy(c);
  p(c).edit().remove(k(bikeId,"enabled")).remove(k(bikeId,"baseline"))
   .remove(k(bikeId,"last")).remove(k(bikeId,"lastAt"))
   .remove(k(bikeId,"pending")).remove(k(bikeId,"lastNote")).commit();
 }
 static synchronized void clearBike(Context c,String bikeId){
  if(!RideBikeBinding.valid(bikeId))return;
  disable(c,bikeId);
  List<String> ids=bikeIds(c);ids.remove(bikeId);
  p(c).edit().putString("bikeIds",String.join(",",ids)).commit();
 }
 static synchronized boolean rebaseline(Context c,String bikeId,double confirmedDashboardKm,double garageKm){
  if(!enabled(c,bikeId)||!OdoAutoSyncPolicy.sane(confirmedDashboardKm)
    ||!OdoAutoSyncPolicy.sane(garageKm)||confirmedDashboardKm<garageKm
    ||confirmedDashboardKm-garageKm>=2000)return false;
  return p(c).edit().putString(k(bikeId,"baseline"),Double.toString(confirmedDashboardKm))
   .remove(k(bikeId,"last")).remove(k(bikeId,"lastAt"))
   .remove(k(bikeId,"pending")).remove(k(bikeId,"lastNote")).commit();
 }
 /** After a completed full restore, invalidate all old ECU baselines and pending sync. */
 static synchronized boolean suspendAfterRestore(Context c){
  boolean persisted=p(c).edit().clear().commit();
  if(!persisted)p(c).edit().clear().apply();
  return persisted;
 }
 static synchronized boolean offer(Context c,String bikeId,double one,double two,long now){
  if(!enabled(c,bikeId))return false;
  double actual=OdoAutoSyncPolicy.consistent(one,two);
  if(!OdoAutoSyncPolicy.sane(actual))return false;
  SharedPreferences pref=p(c);
  double previous;
  try{previous=Double.parseDouble(pref.getString(k(bikeId,"last"),pref.getString(k(bikeId,"baseline"),"")));}
  catch(Exception e){return false;}
  long lastAt=pref.getLong(k(bikeId,"lastAt"),0);
  if(lastAt==0){
   if(!OdoAutoSyncPolicy.first(previous,actual)){
    pref.edit().putString(k(bikeId,"lastNote"),"첫 ECU 값이 계기판 확인 기준과 3km 이상 차이 납니다. 정차 후 기준값 재확인이 필요합니다.").apply();
    return false;
   }
  }else if(!OdoAutoSyncPolicy.next(previous,actual,Math.max(0,now-lastAt))){
   pref.edit().putString(k(bikeId,"lastNote"),"ECU 누적거리 감소·급증 의심으로 반영을 보류했습니다.").apply();
   return false;
  }
  double previousPending=0;
  try{previousPending=Double.parseDouble(pref.getString(k(bikeId,"pending"),"0"));}catch(Exception ignored){}
  if(actual+0.05<previousPending)return false;
  return pref.edit().putString(k(bikeId,"last"),Double.toString(actual))
   .putLong(k(bikeId,"lastAt"),now)
   .putString(k(bikeId,"pending"),Double.toString(Math.max(previousPending,actual)))
   .remove(k(bikeId,"lastNote")).commit();
 }
 static synchronized double pending(Context c,String bikeId){
  if(!enabled(c,bikeId))return Double.NaN;
  try{return Double.parseDouble(p(c).getString(k(bikeId,"pending"),"NaN"));}catch(Exception e){return Double.NaN;}
 }
 /** Return true only when a pending candidate is absent or its removal committed. */
 static synchronized boolean acknowledged(Context c,String bikeId,double applied){
  if(!enabled(c,bikeId))return false;
  double current=pending(c,bikeId);
  if(!OdoAutoSyncPolicy.sane(current))return true;
  if(current>applied+0.05)return false; // A newer ECU sample superseded this write.
  return p(c).edit().remove(k(bikeId,"pending")).commit();
 }
 static synchronized String display(Context c,String bikeId){
  if(!enabled(c,bikeId))return "OBD 누적거리 동기화 꺼짐";
  double candidate=pending(c,bikeId);
  if(OdoAutoSyncPolicy.sane(candidate))
   return "OBD 검증값 "+String.format(java.util.Locale.KOREA,"%.1f km",candidate)+" · 차고 반영 대기";
  String note=p(c).getString(k(bikeId,"lastNote"),"");
  return note.isEmpty()?"OBD 계기판 확인값 대기":note;
 }
}