package com.motohub.next;

import java.util.List;

/** Pure index selection from recorded GPS timestamps; never interpolates through missing fixes. */
final class RideReplayTimeline {
 static final int MAX_PROGRESS=1000;
 private RideReplayTimeline(){}

 /** Reject corrupted or out-of-order timestamps before enabling binary-search playback. */
 static boolean validTimes(List<Long> times){
  if(times==null||times.size()<2)return false;
  long previous=0,first=0;
  for(Long value:times){
   if(value==null||value<=0||value<previous)return false;
   if(first==0)first=value;
   previous=value;
  }
  return previous>first;
 }
 static int indexAt(List<Long> times,int progress){
  if(times==null||times.isEmpty())return -1;
  if(times.size()==1||progress<=0)return 0;
  int last=times.size()-1;
  if(progress>=MAX_PROGRESS)return last;
  long first=times.get(0),end=times.get(last);
  if(first<=0||end<=first)return 0;
  long span=end-first;
  long target=first+(span*Math.max(0,progress))/MAX_PROGRESS;
  int low=0,high=last;
  while(low<high){
   int mid=low+(high-low+1)/2;
   if(times.get(mid)<=target)low=mid;else high=mid-1;
  }
  return low;
 }

 /** Replay preserves relative recorded intervals, compressed to an accessible 30-180 second preview. */
 static long durationMs(List<Long> times){
  if(times==null||times.size()<2)return 30_000;
  long start=times.get(0),end=times.get(times.size()-1);
  if(start<=0||end<=start)return 30_000;
  long fast=(end-start)/60;
  return Math.max(30_000L,Math.min(180_000L,fast));
 }

 static int progressAt(long elapsedMs,long durationMs,int initial){
  int base=Math.max(0,Math.min(MAX_PROGRESS,initial));
  if(elapsedMs<=0)return base;
  if(durationMs<=0)return MAX_PROGRESS;
  long left=MAX_PROGRESS-base;
  long moved=elapsedMs>=durationMs?left:(elapsedMs*left)/durationMs;
  return (int)Math.min(MAX_PROGRESS,base+moved);
 }
}
