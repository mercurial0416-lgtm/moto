package com.motohub.next;

/** Offline-safe helpers for the optional "reuse last record" action. */
final class RecentRecordReuse {
 private RecentRecordReuse() {}

 /** Prefer the actual transaction date, then odometer, then keep the first entry on ties. */
 static boolean isNewer(String candidateDate,double candidateKm,String previousDate,double previousKm) {
  String a=candidateDate==null?"":candidateDate.trim();
  String b=previousDate==null?"":previousDate.trim();
  int byDate=a.compareTo(b);
  if(byDate!=0)return byDate>0;
  double aKm=Double.isFinite(candidateKm)&&candidateKm>=0?candidateKm:0;
  double bKm=Double.isFinite(previousKm)&&previousKm>=0?previousKm:0;
  return aKm>bKm;
 }

 /** A saved nextKm is an absolute odometer target, never copy it verbatim. */
 static long nextServiceKm(double previousMileage,double previousTarget,double currentMileage) {
  if(!Double.isFinite(previousMileage)||!Double.isFinite(previousTarget)||!Double.isFinite(currentMileage)
    ||previousMileage<0||currentMileage<0||previousTarget<=previousMileage)return 0;
  double interval=previousTarget-previousMileage;
  double next=currentMileage+interval;
  // Keep the value in the app's numeric validation range; avoid overflows.
  if(!Double.isFinite(next)||next>1e13)return 0;
  return (long)Math.ceil(next);
 }
}
