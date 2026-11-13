package com.motohub.next;

/** Pure safety decisions for a user-authorized, continuing hands-free OBD session. */
final class ObdHandsFreePolicy {
 private ObdHandsFreePolicy(){}
 /** Visible-app only: retry a failed restart sooner without spamming repeated
  * Activity resume callbacks. A monotonic clock reset signals phone reboot. */
 static boolean visibleResumeDue(long nowElapsed,long previousAttemptElapsed){
  if(nowElapsed<0)return false;
  return previousAttemptElapsed<=0||nowElapsed<previousAttemptElapsed
    ||nowElapsed-previousAttemptElapsed>=15_000L;
 }
 /** After the inner reader exhausts its bounded retries, avoid tight Bluetooth polling. */
 static long reconnectCooldownMs(int exhaustedRounds) {
  if(exhaustedRounds<=1)return 30_000L;
  if(exhaustedRounds==2)return 60_000L;
  if(exhaustedRounds==3)return 120_000L;
  return 300_000L;
 }
 /** Only auto-finish a GPS track when fresh, accurate GPS confirms a sustained stop.
  * Never infer parking solely from Bluetooth disappearing or a GPS outage. */
 static boolean finishAfterObdLoss(boolean armed, boolean recording, boolean gpsFresh,
                                   boolean obdSpeedFresh, int points, double distanceKm,
                                   double speedKmh, long startedElapsed, long lastMotionElapsed,
                                   long nowElapsed, long idleMillis) {
  if(!armed||!recording||!gpsFresh||obdSpeedFresh||points<3||distanceKm<0.1)return false;
  if(!Double.isFinite(speedKmh)||speedKmh>2.0)return false;
  if(nowElapsed<0||startedElapsed<=0||lastMotionElapsed<startedElapsed||nowElapsed<lastMotionElapsed)return false;
  if(idleMillis<180_000L||idleMillis>600_000L)return false;
  return nowElapsed-startedElapsed>=idleMillis && nowElapsed-lastMotionElapsed>=idleMillis;
 }
}
