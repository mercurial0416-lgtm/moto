package com.motohub.next;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Locale;

/**
 * Fallback if a motorcycle ECU does not expose a true lifetime odometer:
 * dashboard-confirmed anchor + completed, bike-bound GPS distance ledger.
 *
 * No OBD/GPS value is ever represented as a verified ECU or dashboard ODO.
 * The original garage ODO, ECU, maintenance and GPS history are untouched.
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
 /** Pure calculation: never silently subtract deleted/reattributed/restored GPS rides. */
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
 private static String k(String bikeId,String f){return bikeId+"."+f;}
 static synchronized boolean calibrate(Context c,String bikeId,double dashboardKm){
  if(!RideBikeBinding.valid(bikeId)||!saneKm(dashboardKm)||RideService.running||
    RideService.saving()||RestoreRideGate.isRestoring()||RestoreRideGate.isFormWriting())return false;
  double gps=RideMileageEstimate.total(c,bikeId);
  if(!saneKm(gps))return false;
  return prefs(c).edit().putString(k(bikeId,"baseline"),Double.toString(dashboardKm))
   .putString(k(bikeId,"gps"),Double.toString(gps))
   .putLong(k(bikeId,"at"),System.currentTimeMillis()).commit();
 }
 static Reading snapshot(Context c,String bikeId){
  if(!RideBikeBinding.valid(bikeId))
   return new Reading(false,false,false,Double.NaN,Double.NaN,Double.NaN,Double.NaN,0);
  SharedPreferences p=prefs(c);
  String raw=p.getString(k(bikeId,"baseline"),null);
  if(raw==null)
   return new Reading(false,false,false,Double.NaN,Double.NaN,Double.NaN,Double.NaN,0);
  try{
   double base=Double.parseDouble(raw);
   double prevGps=Double.parseDouble(p.getString(k(bikeId,"gps"),"NaN"));
   double gps=RideMileageEstimate.total(c,bikeId);
   // Include the uncommitted current ride at most once; once completed it will
   // enter the ledger and running will become false.
   double active=RideService.running&&bikeId.equals(RideService.activeRideBikeId)
       ?RideService.liveDistanceKm:0;
   return calculate(base,prevGps,gps,active,p.getLong(k(bikeId,"at"),0));
  }catch(Exception e){
   return new Reading(true,false,false,Double.NaN,Double.NaN,Double.NaN,Double.NaN,0);
  }
 }
 static String label(Context c,String bikeId){
  Reading r=snapshot(c,bikeId);
  if(!r.configured)return "ODO 대체값 미설정 · 정차 후 실제 계기판 km 1회 확인 필요";
  if(!r.valid)return r.ledgerChanged
    ?"ODO 추정 중지 · GPS 기록이 삭제/복원/재배정되어 계기판 재확인 필요"
    :"ODO 추정 불가 · GPS 거리 기록 또는 기준값 확인 필요";
  return String.format(Locale.KOREA,
   "ODO 추정 %.1f km · 계기판 확인 %.1f + 기록된 GPS %.1f km (미기록 주행 제외)",
   r.estimatedKm,r.baselineKm,r.deltaKm);
 }
}
