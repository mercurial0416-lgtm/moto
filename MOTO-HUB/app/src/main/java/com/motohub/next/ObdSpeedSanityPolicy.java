package com.motohub.next;

/** Reject single-frame ECU speed spikes before using them to arm GPS. */
final class ObdSpeedSanityPolicy {
 private int previous=-1;
 private long at=-1;
 boolean accept(int kmh,long elapsedMs){
  if(kmh<0||kmh>200||elapsedMs<0)return false;
  if(at>=0&&elapsedMs<at)return false;
  // Long gaps create a fresh speed sequence, never extrapolate past disconnect.
  boolean close=at>=0&&elapsedMs-at<=2500;
  if(close&&previous>=0&&Math.abs(kmh-previous)>60)return false;
  previous=kmh;at=elapsedMs;return true;
 }
 void reset(){previous=-1;at=-1;}
}
