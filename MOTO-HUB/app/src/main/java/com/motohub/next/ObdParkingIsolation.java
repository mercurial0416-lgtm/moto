package com.motohub.next;

/**
 * Treat the phone as a moving object independent of the parked motorcycle.
 * ECU speed must be read fresh; GPS-only movement must not restart an ECU stop.
 * This is a conservative safety hint, not proof of ignition OFF.
 */
final class ObdParkingIsolation {
 static final int PARKED_MAX_KMH=2;
 static final int RESUME_MIN_KMH=4;
 static final int LOST_CONFIRM_SAMPLES=2;
 static final long FRESH_SPEED_MS=12_000;
 static final long LOST_PARKED_GRACE_MS=120_000; // parking candidate, NOT a proof of key-OFF

 private int lastSpeed=-1;
 private int lowSpeedSamples;
 private long lastSpeedAt=-1;
 private long parkingLossAt=-1;
 private boolean hold;
 private boolean confirmedAtLoss;

 void onSpeed(int speedKmh,long now) {
  if(speedKmh<0||speedKmh>200||now<0||
     (lastSpeedAt>=0&&now<=lastSpeedAt))return;
  // Two zero-speed samples separated by a long radio outage are not
  // independent confirmation of parking. Preserve walking isolation.
  if(lastSpeedAt>=0&&now-lastSpeedAt>FRESH_SPEED_MS)lowSpeedSamples=0;
  // Only a fresh ECU speed response proves that the OBD link is live again.
  parkingLossAt=-1;confirmedAtLoss=false;
  lastSpeed=speedKmh;
  lastSpeedAt=now;
  if(speedKmh<=PARKED_MAX_KMH){
   lowSpeedSamples=Math.min(10,lowSpeedSamples+1);
   hold=true; // Phone may walk away while ECU stays at zero.
  }else if(speedKmh>=RESUME_MIN_KMH){
   lowSpeedSamples=0;
   hold=false;
   parkingLossAt=-1;
   confirmedAtLoss=false;
  }
 }
 boolean holdGps(){return hold;}
 boolean observedParking(long now){
  return hold&&lowSpeedSamples>=LOST_CONFIRM_SAMPLES
   &&lastSpeedAt>=0&&now>=lastSpeedAt&&now-lastSpeedAt<=FRESH_SPEED_MS;
 }
 /** On the first transport error, preserve stopped GPS but freeze phone walking. */
 boolean onLinkLoss(long now){
  if(confirmedAtLoss)return true;
  boolean stationary=observedParking(now);
  parkingLossAt=stationary?now:-1;
  confirmedAtLoss=stationary;
  if(stationary)hold=true;
  return stationary;
 }
 /** Bluetooth RFCOMM alone is not a fresh ECU speed. Keep parking-loss deadline. */
 void onLinkConnected(){} 
 boolean dueAfterLost(long now){
  return confirmedAtLoss&&parkingLossAt>=0&&now>=parkingLossAt
   &&now-parkingLossAt>=LOST_PARKED_GRACE_MS;
 }
 long remainingAfterLost(long now){
  if(!confirmedAtLoss||parkingLossAt<0)return -1;
  return Math.max(0,LOST_PARKED_GRACE_MS-Math.max(0,now-parkingLossAt));
 }
 void reset(){lastSpeed=-1;lowSpeedSamples=0;lastSpeedAt=-1;parkingLossAt=-1;hold=false;confirmedAtLoss=false;}
}
