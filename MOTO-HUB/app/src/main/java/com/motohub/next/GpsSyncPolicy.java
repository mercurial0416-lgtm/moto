package com.motohub.next;

/** Bound loss of unsynced GPS data while avoiding a sync() per fix.
 *  Every accepted fix is still written and flushed; full sync on clean STOP.
 *  Unexpected power loss may lose at most approximately 2 seconds of latest fixes,
 *  and the existing interrupted-track recovery preserves previously synced records.
 */
final class GpsSyncPolicy {
 static final long MAX_DIRTY_MS=2000;
 static final int MAX_DIRTY_POINTS=5;
 private int dirty;
 private long lastSyncElapsed=-1;
 boolean shouldSync(long now){
  if(now<0)return true;
  dirty++;
  if(dirty>=MAX_DIRTY_POINTS||lastSyncElapsed<0||
     now<lastSyncElapsed||now-lastSyncElapsed>=MAX_DIRTY_MS){
   dirty=0;lastSyncElapsed=now;return true;
  }
  return false;
 }
 void reset(){dirty=0;lastSyncElapsed=-1;}
}
