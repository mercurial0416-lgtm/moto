package com.motohub.next;
import android.content.Context;
import android.content.SharedPreferences;
/** Read-only ECU ODO inbox. Writes an isolated confirmed pending value, never the garage DB. */
final class OdometerAutoSyncInbox {
 private static final String PREF="odo_auto_sync";
 private OdometerAutoSyncInbox(){}
 private static SharedPreferences p(Context context){return context.getSharedPreferences(PREF,Context.MODE_PRIVATE);}
 static synchronized boolean enabled(Context c,String bikeId){
  return RideBikeBinding.valid(bikeId)&&p(c).getBoolean("enabled",false)&&bikeId.equals(p(c).getString("bikeId",""));
 }
 static synchronized boolean enable(Context c,String bikeId,double checkedDashboardKm){
  if(!RideBikeBinding.valid(bikeId)||!OdoAutoSyncPolicy.sane(checkedDashboardKm))return false;
  return p(c).edit().clear().putBoolean("enabled",true).putString("bikeId",bikeId)
   .putString("baseline",Double.toString(checkedDashboardKm)).commit();
 }
 static synchronized void disable(Context c){p(c).edit().clear().commit();}
 static synchronized boolean offer(Context c,String bikeId,double one,double two,long now){
  if(!enabled(c,bikeId))return false;
  double actual=OdoAutoSyncPolicy.consistent(one,two);
  if(!OdoAutoSyncPolicy.sane(actual))return false;
  SharedPreferences pref=p(c);
  double previous;
  try{previous=Double.parseDouble(pref.getString("last",pref.getString("baseline",""))); }
  catch(Exception e){return false;}
  long lastAt=pref.getLong("lastAt",0);
  if(lastAt==0){
   if(!OdoAutoSyncPolicy.first(previous,actual))return false;
  }else if(!OdoAutoSyncPolicy.next(previous,actual,Math.max(0,now-lastAt)))return false;
  double pending=0;
  try{pending=Double.parseDouble(pref.getString("pending","0"));}catch(Exception ignored){}
  if(actual+0.05<pending)return false;
  return pref.edit().putString("last",Double.toString(actual)).putLong("lastAt",now)
   .putString("pending",Double.toString(Math.max(pending,actual))).commit();
 }
 static synchronized double pending(Context c,String bikeId){
  if(!enabled(c,bikeId))return Double.NaN;
  try{return Double.parseDouble(p(c).getString("pending","NaN"));}catch(Exception e){return Double.NaN;}
 }
 static synchronized void acknowledged(Context c,String bikeId,double applied){
  if(!enabled(c,bikeId))return;
  double current=pending(c,bikeId);
  if(OdoAutoSyncPolicy.sane(current)&&current<=applied+0.05)p(c).edit().remove("pending").commit();
 }
 static synchronized String display(Context c,String bikeId){
  if(!enabled(c,bikeId))return "OBD 누적거리 동기화 꺼짐";
  double pending=pending(c,bikeId);
  return OdoAutoSyncPolicy.sane(pending)?"OBD 검증값 "+String.format(java.util.Locale.KOREA,"%.1f km",pending)+" · 차고 반영 대기":"OBD 계기판 확인값 대기";
 }
}
