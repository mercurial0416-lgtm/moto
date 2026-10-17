package com.motohub.next;

/** One-shot consent for a user-initiated GPS ride; never auto-start on permission denial. */
final class RideStartConsent {
 private boolean pending;
 void approve(){pending=true;}
 void cancel(){pending=false;}
 boolean pending(){return pending;}
 /** Permission callbacks may resume only the original ride-screen request. */
 boolean mayContinue(boolean rideScreen,boolean permissionGranted){
  if(!pending)return false;
  if(!rideScreen||!permissionGranted){cancel();return false;}
  return true;
 }
 /** Consume consent exactly once before asking Android to start recording. */
 boolean consume(boolean rideScreen,boolean ready){
  if(!pending||!rideScreen||!ready)return false;
  cancel();return true;
 }
}
