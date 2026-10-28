package com.motohub.next;

/** Per-file prompt policy. A postponement is not a permanent dismissal.
 * Never resumes a GPS service or alters the saved interrupted track.
 */
final class RideRecoveryPromptPolicy {
 private RideRecoveryPromptPolicy(){}
 static final long SNOOZE_MS=24L*60*60*1000;
 static boolean shouldOffer(String file,String reviewed,String postponed,long postponedAt,long now){
  if(file==null||file.isEmpty()||file.equals(reviewed))return false;
  if(file.equals(postponed)&&now>=postponedAt&&postponedAt>0&&now-postponedAt<SNOOZE_MS)return false;
  return true;
 }
}
