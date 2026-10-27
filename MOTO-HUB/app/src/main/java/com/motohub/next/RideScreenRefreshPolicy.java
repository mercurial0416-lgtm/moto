package com.motohub.next;
/** Only visible UI redraw cadence; never changes GPS acquisition or disk recording. */
final class RideScreenRefreshPolicy {
 private RideScreenRefreshPolicy(){}
 static long interval(boolean gpsFresh,double speedKmh,boolean focused){
  // The focus view hides numeric gauges; only an occasional UI sanity refresh remains.
  if(focused)return 5000;
  if(!gpsFresh)return 3000;
  if(!Double.isFinite(speedKmh)||speedKmh<=3)return 3000;
  return 1200;
 }
}
