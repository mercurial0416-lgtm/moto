package com.motohub.next;

/** Keep silent postponement bounded; a newer version is never hidden by the old decision. */
final class UpdatePromptPolicy {
 static final long REMIND_AFTER_MS=24L*60*60*1000;
 private UpdatePromptPolicy(){}
 static boolean shouldPrompt(long available,long installed,long postponedVersion,
    long postponedAt,long now){
  if(available<=installed||now<=0)return false;
  if(available!=postponedVersion)return true;
  return postponedAt<=0||now<postponedAt||now-postponedAt>=REMIND_AFTER_MS;
 }
}