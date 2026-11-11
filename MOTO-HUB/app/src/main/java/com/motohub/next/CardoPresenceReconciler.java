package com.motohub.next;
import android.content.Context;
import android.os.SystemClock;
/** Reconcile lost Cardo ACL broadcasts on foreground, with one two-profile
 * Bluetooth probe rather than two independent callbacks racing the same state.
 * Read-only and does not trigger scanning. */
final class CardoPresenceReconciler {
 private static long lastCheck;
 private CardoPresenceReconciler(){}
 static synchronized void onForeground(Context c){
  if(!CardoAutoRidePrefs.enabled(c)||RideService.running||RideService.autoArmed)return;
  long now=SystemClock.elapsedRealtime();
  if(lastCheck>0&&now>=lastCheck&&now-lastCheck<15_000L)return;
  lastCheck=now;
  final String mac=CardoAutoRidePrefs.address(c);
  Context app=c.getApplicationContext();
  CardoConnectionProbe.probe(app,mac,status->{
   if(status==CardoConnectionProbe.Status.CONNECTED&&CardoAutoRidePrefs.matches(app,mac))
    CardoRideStarter.cardoEvent(app,mac,true);
  });
 }
}
