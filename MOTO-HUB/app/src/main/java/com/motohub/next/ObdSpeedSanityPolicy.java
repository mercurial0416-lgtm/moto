package com.motohub.next;

/** Reject single-frame ECU speed spikes before using them to arm GPS. */
final class ObdSpeedSanityPolicy {
 private int previous=-1;
 private long at=-1;
 private int pendingJump=-1;
 private long pendingAt=-1;
 boolean accept(int kmh,long elapsedMs){
  if(kmh<0||kmh>200||elapsedMs<0)return false;
  if(at>=0&&elapsedMs<at)return false;
  // Long gaps create a fresh speed sequence, never extrapolate past disconnect.
  boolean close=at>=0&&elapsedMs-at<=2500;
  if(close&&previous>=0&&Math.abs(kmh-previous)>60){
   // One outlier cannot start GPS; two agreeing consecutive frames restore
   // a real speed transition instead of permanently rejecting it.
   if(pendingJump>=0&&pendingAt>=0&&elapsedMs>=pendingAt&&
      elapsedMs-pendingAt<=2500&&Math.abs(kmh-pendingJump)<=10){
    previous=kmh;at=elapsedMs;pendingJump=-1;pendingAt=-1;return true;
   }
   pendingJump=kmh;pendingAt=elapsedMs;return false;
  }
  previous=kmh;at=elapsedMs;pendingJump=-1;pendingAt=-1;return true;
 }
 void reset(){previous=-1;at=-1;pendingJump=-1;pendingAt=-1;}
}
