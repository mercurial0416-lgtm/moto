package com.motohub.next;
import java.io.File;
/** Pure local analytics. Uses file creation epoch, never relies on system-modifiable mtime. */
final class RideMonthlyReport {
 static final class Totals {int count;double km;long minutes;}
 final Totals current=new Totals(),previous=new Totals();int skipped;
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
  Totals target=recent(track,now,30)?current:previous;
  target.count++;target.km+=distance;target.minutes+=duration/60000;
 }
}
