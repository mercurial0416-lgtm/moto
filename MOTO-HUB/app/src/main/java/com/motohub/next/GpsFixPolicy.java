package com.motohub.next;

/** Reject impossible wall-clock GPS timestamps before they poison a ride's order. */
final class GpsFixPolicy {
 static final long MAX_FUTURE_SKEW_MS=120_000L;
 private GpsFixPolicy() {}
 /** Only device-supplied timestamps are saved; no synthetic points or times. */
 static boolean acceptTime(long fixTime,long wallNow,long previous){
  return fixTime>0 && fixTime>previous && wallNow>0 &&
   (fixTime<=wallNow || fixTime-wallNow<=MAX_FUTURE_SKEW_MS);
 }
}
