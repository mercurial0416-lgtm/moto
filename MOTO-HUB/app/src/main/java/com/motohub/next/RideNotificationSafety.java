package com.motohub.next;

/** Nonessential notification failures must not abort a durable GPS session. */
final class RideNotificationSafety {
 private RideNotificationSafety(){}

 /** Returns false if notification delivery is unavailable. Never swallows fatal JVM errors. */
 static boolean run(Runnable notification){
  if(notification==null)return true;
  try{notification.run();return true;}
  catch(RuntimeException ignored){return false;}
 }
}
