package com.motohub.next;
/** Only visible UI redraw cadence; never changes GPS acquisition or disk recording. */
final class RideScreenRefreshPolicy {
 private RideScreenRefreshPolicy(){}
 static long interval(boolean gpsFresh,double speedKmh,boolean focused){
  if(!gpsFresh)return focused?4000:3000;
  if(!Double.isFinite(speedKmh)||speedKmh<=3)return focused?2500:3000;
  return focused?1500:1200;
 }
}
