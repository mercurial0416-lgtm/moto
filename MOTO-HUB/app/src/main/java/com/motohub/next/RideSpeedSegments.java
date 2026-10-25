package com.motohub.next;
/** Pure GPS display grouping. -1 means no valid continuous speed segment. */
final class RideSpeedSegments {
 static final int UNKNOWN=-1, SLOW=0, CRUISE=1, FAST=2;
 private RideSpeedSegments(){}
 static int category(long deltaMs,double distanceMetres,double previousAccuracy,double accuracy){
  if(distanceMetres<Math.max(5,Math.max(previousAccuracy,accuracy))||!RideMotionGate.countDistance(deltaMs,distanceMetres,previousAccuracy,accuracy))return UNKNOWN;
  double kmh=distanceMetres*3600.0/deltaMs;
  if(!Double.isFinite(kmh)||kmh>200)return UNKNOWN;
  return kmh<30?SLOW:kmh<70?CRUISE:FAST;
 }
}
