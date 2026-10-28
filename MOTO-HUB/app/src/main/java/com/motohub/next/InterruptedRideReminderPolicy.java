package com.motohub.next;

/** Notification-only recovery reminder for incomplete GPS tracks.
 * A "later" dismissal is not a permanent acknowledgment. Never starts GPS or
 * modifies the original GPS track; only the explicit review action marks seen.
 */
final class InterruptedRideReminderPolicy {
 static final long SNOOZE_MS=24L*60*60*1000;
 private InterruptedRideReminderPolicy(){}
 static boolean shouldShow(String candidate,String acknowledged,String snoozedFile,long snoozedAt,long now){
  if(candidate==null||candidate.isEmpty()||candidate.equals(acknowledged)||now<=0)return false;
  if(!candidate.equals(snoozedFile))return true;
  if(snoozedAt<=0)return true;
  // A clock rollback must not spam dialogs.
  return now>=snoozedAt&&now-snoozedAt>=SNOOZE_MS;
 }
}
