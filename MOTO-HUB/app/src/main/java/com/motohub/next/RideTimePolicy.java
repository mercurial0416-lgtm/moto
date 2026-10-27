package com.motohub.next;

/** Bounded time estimate from consecutive accurate GPS fixes; no bridging outages. */
final class RideTimePolicy {
 private RideTimePolicy(){}
 static boolean eligible(long deltaMs,double metres,double previousAccuracy,double currentAccuracy){
  return deltaMs>0 && deltaMs<=RideMotionGate.MAX_CONTINUOUS_FIX_MS &&
      Double.isFinite(metres)&&metres>=0&&
      Double.isFinite(previousAccuracy)&&Double.isFinite(currentAccuracy)&&
      previousAccuracy>=0&&currentAccuracy>=0&&
      previousAccuracy<=25&&currentAccuracy<=25&&
      metres*3600.0/deltaMs<=250;
 }
 static boolean moving(double metres,long deltaMs){
  return deltaMs>0&&Double.isFinite(metres)&&metres>=0&&metres*3600.0/deltaMs>=5.0;
 }
 static double movingAverage(double distanceKm,long movingMillis){
  return Double.isFinite(distanceKm)&&distanceKm>=0&&movingMillis>0?
   Math.min(250.0,distanceKm*3600000.0/movingMillis):0;
 }
}