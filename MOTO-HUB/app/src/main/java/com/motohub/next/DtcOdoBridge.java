package com.motohub.next;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Locale;

/**
 * Opt-in, per-bike derived ODO from SAE PID 0131. The initial actual dashboard
 * reading and simultaneous PID 0131 value are physically confirmed once.
 *
 * Stored in a separate preference namespace; this never overwrites verified
 * garage odometer, service history, or an actual SAE 01A6 lifetime reading.
 */
final class DtcOdoBridge {
 private static final String PREF="obd_since_clear_odo_v590";
 private DtcOdoBridge(){}
 static final class Reading {
  final boolean enabled,valid,paused;
  final double estimatedKm,dashboardAnchorKm;
  final int counterAtAnchor,lastCounter;
  final long lastAt;
  final String reason;
  Reading(boolean enabled,boolean valid,boolean paused,double estimatedKm,
    double dashboardAnchorKm,int counterAtAnchor,int lastCounter,long lastAt,String reason){
   this.enabled=enabled;this.valid=valid;this.paused=paused;
   this.estimatedKm=estimatedKm;this.dashboardAnchorKm=dashboardAnchorKm;
   this.counterAtAnchor=counterAtAnchor;this.lastCounter=lastCounter;
   this.lastAt=lastAt;this.reason=reason;
  }
 }
 private static SharedPreferences prefs(Context c){return c.getSharedPreferences(PREF,Context.MODE_PRIVATE);}
 private static String k(String bikeId,String field){return bikeId+"."+field;}
 static synchronized boolean enabled(Context c,String bikeId){
  return RideBikeBinding.valid(bikeId)&&prefs(c).getBoolean(k(bikeId,"enabled"),false);
 }
 static synchronized boolean calibrate(Context c,String bikeId,double dashboardKm,int sinceClearKm){
  if(!RideBikeBinding.valid(bikeId)||!Double.isFinite(dashboardKm)||dashboardKm<0||
     dashboardKm>2_000_000||sinceClearKm<0||sinceClearKm>=65535||
     RideService.running||RideService.autoArmed||RideService.saving()||
     RestoreRideGate.isRestoring()||RestoreRideGate.isFormWriting())return false;
  long now=System.currentTimeMillis();
  return prefs(c).edit()
    .putBoolean(k(bikeId,"enabled"),true)
    .putString(k(bikeId,"baseline"),Double.toString(dashboardKm))
    .putInt(k(bikeId,"anchor"),sinceClearKm)
    .putInt(k(bikeId,"last"),sinceClearKm)
    .putLong(k(bikeId,"lastAt"),now)
    .putBoolean(k(bikeId,"paused"),false)
    .remove(k(bikeId,"reason"))
    .commit();
 }
 static synchronized boolean disable(Context c,String bikeId){
  if(!RideBikeBinding.valid(bikeId))return false;
  return prefs(c).edit().remove(k(bikeId,"enabled"))
    .remove(k(bikeId,"baseline")).remove(k(bikeId,"anchor"))
    .remove(k(bikeId,"last")).remove(k(bikeId,"lastAt"))
    .remove(k(bikeId,"paused")).remove(k(bikeId,"reason")).commit();
 }
 /** Forget all per-bike OBD-derived state after permanently deleting its garage entry. */
 static synchronized boolean clearBike(Context c,String bikeId){
  if(!RideBikeBinding.valid(bikeId))return false;
  SharedPreferences.Editor e=prefs(c).edit();
  for(String field:new String[]{"enabled","baseline","anchor","last","lastAt","paused","reason"})
   e.remove(k(bikeId,field));
  return e.commit();
 }
 /** A backend change, restore, or replacement bike invalidates local baselines. */
 static synchronized boolean suspendAfterRestore(Context c){return prefs(c).edit().clear().commit();}
 static synchronized boolean offer(Context c,String bikeId,int current,long nowMs){
  if(!enabled(c,bikeId)||!DtcOdoCounterPolicy.valid(current)||nowMs<=0)return false;
  SharedPreferences p=prefs(c);
  if(p.getBoolean(k(bikeId,"paused"),false))return false;
  int last=p.getInt(k(bikeId,"last"),-1);
  int anchor=p.getInt(k(bikeId,"anchor"),-1);
  long previousAt=p.getLong(k(bikeId,"lastAt"),0);
  double baseline;
  try{baseline=Double.parseDouble(p.getString(k(bikeId,"baseline"),"NaN"));}
  catch(Exception e){return false;}
  if(!Double.isFinite(DtcOdoCounterPolicy.estimated(baseline,anchor,last))||
     nowMs<=previousAt||previousAt<=0)return false;
  DtcOdoCounterPolicy.Decision verdict=DtcOdoCounterPolicy.check(last,current,nowMs-previousAt);
  if(verdict==DtcOdoCounterPolicy.Decision.RESET){
   p.edit().putBoolean(k(bikeId,"paused"),true)
    .putString(k(bikeId,"reason"),"0131 카운터 감소 · 고장코드 삭제/ECU 리셋 가능 · 계기판 재보정 필요")
    .commit();return false;
  }
  if(verdict==DtcOdoCounterPolicy.Decision.SATURATED){
   p.edit().putBoolean(k(bikeId,"paused"),true)
    .putString(k(bikeId,"reason"),"0131 값 65,535km 포화 · 더 이상 주행거리 측정 불가 · GPS 사용/재보정")
    .commit();return false;
  }
  if(verdict==DtcOdoCounterPolicy.Decision.IMPLAUSIBLE){
   // A noisy single PID sample is ignored. Keep the older, last good sample.
   p.edit().putString(k(bikeId,"reason"),"0131 거리 급증/시간 오류 의심 · 값 무시").apply();
   return false;
  }
  return p.edit().putInt(k(bikeId,"last"),current)
    .putLong(k(bikeId,"lastAt"),nowMs).remove(k(bikeId,"reason")).commit();
 }
 static synchronized Reading snapshot(Context c,String bikeId){
  if(!enabled(c,bikeId))return new Reading(false,false,false,Double.NaN,
    Double.NaN,-1,-1,0,"미설정");
  SharedPreferences p=prefs(c);
  int anchor=p.getInt(k(bikeId,"anchor"),-1),last=p.getInt(k(bikeId,"last"),-1);
  long lastAt=p.getLong(k(bikeId,"lastAt"),0);
  boolean paused=p.getBoolean(k(bikeId,"paused"),false);
  String reason=p.getString(k(bikeId,"reason"),"");
  double base=Double.NaN;
  try{base=Double.parseDouble(p.getString(k(bikeId,"baseline"),"NaN"));}catch(Exception ignored){}
  double estimate=DtcOdoCounterPolicy.estimated(base,anchor,last);
  boolean valid=!paused&&Double.isFinite(estimate)&&lastAt>0;
  return new Reading(true,valid,paused,estimate,base,anchor,last,lastAt,reason);
 }
 static String label(Context c,String bikeId){
  Reading r=snapshot(c,bikeId);
  if(!r.enabled)return "PID 0131 보조 주행거리: 기준값 미설정";
  if(r.paused)return "PID 0131 보조 주행거리 중지 · "+r.reason;
  if(!r.valid)return "PID 0131 보조 주행거리 오류 · 계기판 재확인 필요";
  long ageMs=System.currentTimeMillis()-r.lastAt;
  String age=ageMs<0?"시각 변경 의심":ageMs>=1_200_000?"오래된 마지막 수신":"마지막 유효 수신";
  return String.format(Locale.KOREA,
    "PID 0131 추정 ODO %.1f km · 계기판 %.1f + (코드삭제 후 %d - 기준 %d)km · %s",
    r.estimatedKm,r.dashboardAnchorKm,r.lastCounter,r.counterAtAnchor,age);
 }
}
