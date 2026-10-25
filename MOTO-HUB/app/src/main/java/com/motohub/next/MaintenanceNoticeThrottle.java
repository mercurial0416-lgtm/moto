package com.motohub.next;
/** Opted-in service notices repeat at most weekly while overdue status persists. */
final class MaintenanceNoticeThrottle {
 static final long REPEAT_MS=7L*24*60*60*1000;
 private MaintenanceNoticeThrottle(){}
 static boolean shouldNotify(String fingerprint,String previous,long lastSent,long now){
  if(fingerprint==null||fingerprint.isEmpty()||now<=0)return false;
  if(!fingerprint.equals(previous))return true;
  // Legacy installs with a stamp but no timestamp do not get a surprise
  // immediate duplicate on the upgrade check.
  if(lastSent<=0||now<lastSent)return false;
  return now-lastSent>=REPEAT_MS;
 }
}
