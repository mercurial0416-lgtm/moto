package com.motohub.next;
import java.io.File;
/** Pure local analytics. Uses file creation epoch, never relies on system-modifiable mtime. */
final class RideMonthlyReport {
 static final class Totals {
  int count;double km;long minutes;
  private long durationMs;
  void add(double distance,long elapsedMs){
   km+=distance;durationMs+=elapsedMs;
   // Round once after aggregating all GPS segments, not once per split file.
   minutes=durationMs/60_000L;
  }
 }
 final Totals current=new Totals(),previous=new Totals();int skipped;
 /** One logical ride belongs to its earliest observed 30-day bucket. */
 private final java.util.Map<String,Boolean> earliestBucket=new java.util.HashMap<>();
 /** Standalone session-key parser: legacy pure-Java reports compile without Android JSON. */
 static String sessionKey(File track){
  if(track==null)return "";
  String name=track.getName();
  if(!name.matches("[0-9]{10,15}-[a-f0-9-]{36}\\.track"))return "";
  return name.substring(name.indexOf('-')+1,name.length()-6);
 }
 static long timestamp(File track){
  // Imported GPX tracks are saved under an import-time filename. Use the first
  // recorded point instead, or an old ride imported today appears as a new ride.
  if(track==null)return -1;
  Long recorded=RideHistoryLabel.startTime(track);
  return recorded==null?-1:recorded;
 }
 static boolean recent(File track,long now,int days){long stamp=timestamp(track);return stamp>0&&stamp<=now&&now-stamp<(long)days*24*3600*1000;}
 void add(File track,long now,double distance,long duration){
  if(!recent(track,now,60)||!Double.isFinite(distance)||distance<0||distance>1000||duration<0)return;
  boolean inCurrent=recent(track,now,30);
  Totals target=inCurrent?current:previous;
  String session=sessionKey(track);
  if(session.isEmpty())target.count++;
  else {
   Boolean earlier=earliestBucket.putIfAbsent(session,inCurrent);
   if(earlier==null)target.count++;
   else if(earlier && !inCurrent){
    // A session spanning the 30-day boundary must never count twice.
    current.count--;previous.count++;earliestBucket.put(session,false);
   }
  }
  target.add(distance,duration);
 }
}
