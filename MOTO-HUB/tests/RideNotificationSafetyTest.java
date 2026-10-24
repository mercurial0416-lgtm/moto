package com.motohub.next;

public final class RideNotificationSafetyTest {
 static int checks;
 static void check(boolean condition,String message){
  checks++;
  if(!condition)throw new AssertionError(message);
 }
 public static void main(String[] args){
  int[] notifications={0};
  check(RideNotificationSafety.run(()->notifications[0]++),"normal notification succeeds");
  check(notifications[0]==1,"normal notification executed once");
  check(!RideNotificationSafety.run(()->{throw new SecurityException("notifications denied");}),
   "revoked notification permission must be non-fatal");
  check(!RideNotificationSafety.run(()->{throw new IllegalStateException("channel gone");}),
   "notification manager failure must be non-fatal");
  check(RideNotificationSafety.run(()->notifications[0]++),
   "later notification remains usable after an exception");
  check(notifications[0]==2,"later notification is delivered");
  check(RideNotificationSafety.run(null),"null notification is harmless");
  System.out.println("PASS ride notification failure isolation: "+checks+" assertions");
 }
}
