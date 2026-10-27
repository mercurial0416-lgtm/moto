package com.motohub.next;

/** Advisory only. Never closes a user's GPS file or infers motion from a stale fix. */
final class RideStopSuggestion {
 private RideStopSuggestion(){}
 static final long MIN_RIDE_MS=5*60_000L;
 static final long STATIONARY_MS=12*60_000L;
 static boolean moving(double kmh){
  return Double.isFinite(kmh) && kmh>=8.0 && kmh<=250.0;
 }
 static boolean shouldSuggest(long now,long rideStart,long lastMotion,
       boolean gpsFresh,double distanceKm,double gpsSpeedKmh,boolean alreadyShown){
  if(alreadyShown||!gpsFresh||!Double.isFinite(distanceKm)||distanceKm<0.3||
     !Double.isFinite(gpsSpeedKmh)||gpsSpeedKmh>5.0||gpsSpeedKmh<0||
     rideStart<=0||lastMotion<rideStart||now<lastMotion||now<rideStart)return false;
  return now-rideStart>=MIN_RIDE_MS&&now-lastMotion>=STATIONARY_MS;
 }
}
