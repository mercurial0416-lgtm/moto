package com.motohub.next;

/** GPS data freshness uses monotonic elapsed time: a stale fix is never rendered as 0 km/h. */
final class GpsSignalStatus {
 static final long FRESH_MS=15_000L;
 static final long LOST_MS=35_000L;
 static boolean fresh(long lastElapsed,long now){
  return lastElapsed>0&&now>=lastElapsed&&now-lastElapsed<=FRESH_MS;
 }
 static boolean lost(long lastElapsed,long now){
  return lastElapsed>0&&now>=lastElapsed&&now-lastElapsed>LOST_MS;
 }
 static String liveSpeedLabel(double kmh,long lastElapsed,long now){
  if(!fresh(lastElapsed,now))return "GPS 대기";
  return Double.isFinite(kmh)&&kmh>=0&&kmh<=250?Math.round(kmh)+" km/h":"GPS 대기";
 }
 private GpsSignalStatus(){}
}
