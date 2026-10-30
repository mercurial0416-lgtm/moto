package com.motohub.next;

/** Keep silent postponement bounded; a newer version is never hidden by the old decision. */
final class UpdatePromptPolicy {
 static final long REMIND_AFTER_MS=24L*60*60*1000;
 static final long CHECK_INTERVAL_MS=6L*60*60*1000;
 static final long FAILURE_RETRY_MS=15L*60*1000;
 /** A failed network check must not delay future update notices for six hours. */
 static boolean checkDue(long now,long lastCheck,boolean lastSucceeded){
  if(now<=0)return false;
  if(lastCheck<=0||lastCheck>=now)return true; // First check or device clock corrected.
  long interval=lastSucceeded?CHECK_INTERVAL_MS:FAILURE_RETRY_MS;
  return now-lastCheck>=interval;
 }
 private UpdatePromptPolicy(){}
 static boolean shouldPrompt(long available,long installed,long postponedVersion,
    long postponedAt,long now){
  if(available<=installed||now<=0)return false;
  if(available!=postponedVersion)return true;
  return postponedAt<=0||now<postponedAt||now-postponedAt>=REMIND_AFTER_MS;
 }
}