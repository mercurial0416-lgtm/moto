package com.motohub.next;
/** Offline GPS distance filter. Bad/gapped fixes may still be saved, but never inflate distance. */
final class RideMotionGate {
 private RideMotionGate() {}
 static final long MAX_CONTINUOUS_FIX_MS=30_000;
 static boolean countDistance(long gapMillis,double metres,double previousAccuracy,double currentAccuracy){
  if(gapMillis<=0||gapMillis>MAX_CONTINUOUS_FIX_MS||!Double.isFinite(metres)||metres<0||
     !Double.isFinite(previousAccuracy)||!Double.isFinite(currentAccuracy)||
     previousAccuracy>40||currentAccuracy>40||previousAccuracy<0||currentAccuracy<0)return false;
  // Small movements inside the GPS accuracy radius are usually position drift,
  // not motorcycle travel. Keep the raw samples for GPX; exclude drift from
  // live distance and summaries without inventing missing movement.
  double jitterMetres=Math.max(4.0,Math.min(12.0,(previousAccuracy+currentAccuracy)*0.4));
  if(metres<jitterMetres)return false;
  return metres*3600.0/gapMillis<=250;
 }
}
