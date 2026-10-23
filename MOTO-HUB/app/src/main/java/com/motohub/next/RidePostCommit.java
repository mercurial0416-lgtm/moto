package com.motohub.next;
/** Optional after-write actions must never close an already-synced local GPS track. */
final class RidePostCommit {
 private RidePostCommit(){}
 static void afterWrite(Runnable sharing,Runnable onShareFailure,Runnable notification){
  try{if(sharing!=null)sharing.run();}
  catch(RuntimeException failure){
   // Disable a failing optional sharing path, keeping consented GPS capture live.
   try{if(onShareFailure!=null)onShareFailure.run();}catch(RuntimeException ignored){}
  }
  try{if(notification!=null)notification.run();}catch(RuntimeException ignored){}
 }
}
