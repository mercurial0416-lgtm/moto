package com.motohub.next;
/** Never import unsupported, ambiguous, regressing or implausibly jumping ECU odometer values. */
final class OdoAutoSyncPolicy {
 static boolean sane(double km){return Double.isFinite(km)&&km>0&&km<=2_000_000;}
 /** First automatic sample must agree with manually confirmed dashboard baseline. */
 static boolean first(double baseline,double actual){return sane(baseline)&&sane(actual)&&Math.abs(actual-baseline)<=1.0;}
 /** Later sample cannot roll back or jump beyond physical travel elapsed at 200km/h. */
 static boolean next(double previous,double actual,long elapsedMillis){
  if(!sane(previous)||!sane(actual)||elapsedMillis<0)return false;
  if(actual+0.05<previous)return false;
  double plausible=Math.max(0.6,elapsedMillis*200.0/3_600_000.0+0.5);
  return actual-previous<=plausible;
 }
 /** Two independent samples must agree within normal 0.1km ECU resolution. */
 static double consistent(double one,double two){
  return sane(one)&&sane(two)&&Math.abs(one-two)<=0.2?Math.max(one,two):Double.NaN;
 }
 private OdoAutoSyncPolicy(){}
}
