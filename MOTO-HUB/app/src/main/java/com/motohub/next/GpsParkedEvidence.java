package com.motohub.next;

/**
 * Requires a continuous sequence of accurate, consecutive stationary GPS fixes
 * before a missing OBD link can close an auto ride. A single fix after a long
 * tunnel/parking-garage outage must never count as minutes of stationary proof.
 */
final class GpsParkedEvidence {
 static final long MAX_FIX_INTERVAL_MS=30_000L;
 static final long FRESH_MS=15_000L;
 static final double STATIONARY_MAX_KMH=2.0;
 private long stationarySince=-1L,lastFixElapsed=-1L;
 synchronized void reset(){stationarySince=-1L;lastFixElapsed=-1L;}
 synchronized void observe(long fixElapsed,long fixIntervalMillis,double distanceMetres,
                           double previousAccuracyMetres,double currentAccuracyMetres){
  if(fixElapsed<=0||fixIntervalMillis<=0||
     fixIntervalMillis>MAX_FIX_INTERVAL_MS||
     !Double.isFinite(distanceMetres)||distanceMetres<0||
     !Double.isFinite(previousAccuracyMetres)||!Double.isFinite(currentAccuracyMetres)||
     previousAccuracyMetres>20||currentAccuracyMetres>20||
     previousAccuracyMetres<0||currentAccuracyMetres<0||
     lastFixElapsed>fixElapsed){
   reset();return;
  }
  if(lastFixElapsed>0&&fixElapsed-lastFixElapsed>MAX_FIX_INTERVAL_MS)stationarySince=-1;
  lastFixElapsed=fixElapsed;
  double kmh=distanceMetres*3600.0/fixIntervalMillis;
  if(kmh<=STATIONARY_MAX_KMH){
   if(stationarySince<0)stationarySince=fixElapsed;
  }else stationarySince=-1;
 }
 synchronized boolean sustained(long nowElapsed,long durationMs){
  return durationMs>=180_000L&&durationMs<=600_000L&&stationarySince>0
   &&lastFixElapsed>0&&nowElapsed>=lastFixElapsed&&nowElapsed-lastFixElapsed<=FRESH_MS
   &&nowElapsed-stationarySince>=durationMs;
 }
}
