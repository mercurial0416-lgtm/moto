package com.motohub.next;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.util.Iterator;
import java.util.Locale;

/**
 * Per-bike ODO fallback: a manually observed dashboard reading, plus
 * bike-bound, completed GPS distance. Never claims to be the ECU odometer.
 * A non-additive ledger edit suspends estimation until manual recalibration.
 */
final class HybridOdometerEstimate {
 private static final String PREF="hybrid_odo_v588";
 private static final double MAX_KM=2_000_000.0;
 static final class Reading {
  final boolean configured,valid,ledgerChanged;
  final double estimatedKm,deltaKm,baselineKm,gpsAtBaselineKm;
  final long checkedAt;
  private Reading(boolean configured,boolean valid,boolean ledgerChanged,
    double estimatedKm,double deltaKm,double baselineKm,double gpsAtBaselineKm,long checkedAt){
   this.configured=configured;this.valid=valid;this.ledgerChanged=ledgerChanged;
   this.estimatedKm=estimatedKm;this.deltaKm=deltaKm;
   this.baselineKm=baselineKm;this.gpsAtBaselineKm=gpsAtBaselineKm;this.checkedAt=checkedAt;
  }
 }
 private HybridOdometerEstimate(){}
 private static boolean saneKm(double km){return Double.isFinite(km)&&km>=0&&km<=MAX_KM;}
 static boolean validAnchor(String bikeId,double km,double gpsKm){
  return RideBikeBinding.valid(bikeId)&&saneKm(km)&&saneKm(gpsKm);
 }
 static Reading calculate(double baseline,double gpsAtAnchor,double gpsCompleted,
                          double activeRideKm,long checkedAt){
  if(!saneKm(baseline)||!saneKm(gpsAtAnchor)||!saneKm(gpsCompleted)||
     !saneKm(activeRideKm))
   return new Reading(true,false,false,Double.NaN,Double.NaN,baseline,gpsAtAnchor,checkedAt);
  if(gpsCompleted+0.001<gpsAtAnchor)
   return new Reading(true,false,true,Double.NaN,Double.NaN,baseline,gpsAtAnchor,checkedAt);
  double delta=Math.max(0,gpsCompleted-gpsAtAnchor)+activeRideKm;
  double estimated=baseline+delta;
  if(!saneKm(estimated))
   return new Reading(true,false,false,Double.NaN,Double.NaN,baseline,gpsAtAnchor,checkedAt);
  return new Reading(true,true,false,estimated,delta,baseline,gpsAtAnchor,checkedAt);
 }
 private static SharedPreferences prefs(Context c){return c.getSharedPreferences(PREF,Context.MODE_PRIVATE);}
 private static String k(String bikeId,String field){return bikeId+"."+field;}
 /** Compare each historical GPS track identity AND its recorded distance,
  * not merely the total (removing 10 km and adding 10 km must be detected). */
 static boolean containsAll(JSONObject current,JSONObject previous){
  if(current==null||previous==null)return false;
  Iterator<String> keys=previous.keys();
  while(keys.hasNext()){
   String key=keys.next();
   if(!current.has(key))return false;
   double before=previous.optDouble(key,Double.NaN);
   double now=current.optDouble(key,Double.NaN);
   if(!Double.isFinite(before)||!Double.isFinite(now)||
      Double.compare(before,now)!=0)return false;
  }
  return true;
 }
 static double sum(JSONObject entries){
  if(entries==null)return Double.NaN;
  double total=0;
  Iterator<String> keys=entries.keys();
  while(keys.hasNext()){
   double km=entries.optDouble(keys.next(),Double.NaN);
   if(!Double.isFinite(km)||km<0||km>500)return Double.NaN;
   total+=km;
  }
  return saneKm(total)?total:Double.NaN;
 }
 private static Reading unavailable(double baseline,double anchorGps,long at,boolean changed){
  return new Reading(true,false,changed,Double.NaN,Double.NaN,baseline,anchorGps,at);
 }
 /** The calibration snapshot is written atomically with the physical baseline. */
 static synchronized boolean calibrate(Context c,String bikeId,double dashboardKm){
  if(!RideBikeBinding.valid(bikeId)||!saneKm(dashboardKm)||
     RideService.running||RideService.autoArmed||RideService.saving()||
     RestoreRideGate.isRestoring()||RestoreRideGate.isFormWriting())return false;
  JSONObject entries=RideMileageEstimate.bikeEntries(c,bikeId);
  double gps=sum(entries);
  if(!saneKm(gps))return false;
  return prefs(c).edit()
   .putString(k(bikeId,"baseline"),Double.toString(dashboardKm))
   .putString(k(bikeId,"gps"),Double.toString(gps))
   .putString(k(bikeId,"seen"),entries.toString())
   .putLong(k(bikeId,"at"),System.currentTimeMillis()).commit();
 }
 /** Forget this bike's derived distance ledger and calibration after deletion. */
 static synchronized boolean clearBike(Context c,String bikeId){
  if(!RideBikeBinding.valid(bikeId))return false;
  SharedPreferences.Editor e=prefs(c).edit();
  for(String field:new String[]{"baseline","gps","seen","at"})e.remove(k(bikeId,field));
  return e.commit();
 }
 /** Restoring an old garage can change bike identities or mileage independently
  * of the GPS ledger. Never carry a prior physical ODO anchor across restore. */
 static synchronized boolean suspendAfterRestore(Context c){
  return prefs(c).edit().clear().commit();
 }
 /** All snapshots are serialized per process to avoid losing a completed ride
  * when UI and telemetry update concurrently. Only additive rides are allowed.
  * v588 baselines that lack a track snapshot require a fresh calibration. */
 static synchronized Reading snapshot(Context c,String bikeId){
  if(!RideBikeBinding.valid(bikeId))
   return new Reading(false,false,false,Double.NaN,Double.NaN,Double.NaN,Double.NaN,0);
  SharedPreferences p=prefs(c);
  String raw=p.getString(k(bikeId,"baseline"),null);
  if(raw==null)
   return new Reading(false,false,false,Double.NaN,Double.NaN,Double.NaN,Double.NaN,0);
  double baseline=Double.NaN,anchorGps=Double.NaN;
  long at=p.getLong(k(bikeId,"at"),0);
  try{
   baseline=Double.parseDouble(raw);
   anchorGps=Double.parseDouble(p.getString(k(bikeId,"gps"),"NaN"));
   if(!validAnchor(bikeId,baseline,anchorGps))
    return unavailable(baseline,anchorGps,at,false);
   if(RestoreRideGate.isRestoring()||RestoreRideGate.isFormWriting()||RideService.saving())
    return unavailable(baseline,anchorGps,at,false);
   String seenRaw=p.getString(k(bikeId,"seen"),null);
   if(seenRaw==null)return unavailable(baseline,anchorGps,at,true);
   JSONObject previous=new JSONObject(seenRaw);
   JSONObject current=RideMileageEstimate.bikeEntries(c,bikeId);
   if(current==null)return unavailable(baseline,anchorGps,at,false);
   if(!containsAll(current,previous))
    return unavailable(baseline,anchorGps,at,true);
   // Persist the new set before displaying increased mileage. If a later
   // completed ride is deleted/reassigned, the next snapshot will fail closed.
   if(current.length()!=previous.length()&&
      !p.edit().putString(k(bikeId,"seen"),current.toString()).commit())
    return unavailable(baseline,anchorGps,at,false);
   double active=RideService.running&&bikeId.equals(RideService.activeRideBikeId)
    ?RideService.liveDistanceKm:0;
   return calculate(baseline,anchorGps,sum(current),active,at);
  }catch(Exception e){
   return unavailable(baseline,anchorGps,at,false);
  }
 }
 static String label(Context c,String bikeId){
  if(RideService.saving())return "ODO 추정 일시 대기 · GPS 주행 기록 저장 중";
  Reading r=snapshot(c,bikeId);
  if(!r.configured)return "ODO 대체값 미설정 · 정차 후 실제 계기판 km 1회 확인 필요";
  if(!r.valid)return r.ledgerChanged
    ?"ODO 추정 중지 · GPS 기록 삭제/수정 또는 이전 버전 기준값 · 계기판 재보정 필요"
    :"ODO 추정 보류 · GPS 기록 또는 보정 기준값 확인 필요";
  return String.format(Locale.KOREA,
   "ODO 추정 %.1f km · 계기판 확인 %.1f + GPS 기록 %.1f km (미기록 주행 제외) · 기준값 %s · GPS 누락 시 실제 ODO와 오차 발생",
   r.estimatedKm,r.baselineKm,r.deltaKm,
   r.checkedAt>0&&System.currentTimeMillis()>=r.checkedAt?
     ((System.currentTimeMillis()-r.checkedAt)/86_400_000L)+"일 전":"확인 불가");
 }
}
