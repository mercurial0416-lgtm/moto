package com.motohub.next;

/** Deterministic pure JVM tests with a synthetic monotonic clock. */
public final class ObdAuxSensorPolicyTest {
 private static int assertions;
 private static void check(boolean ok,String description) {
  assertions++;
  if(!ok)throw new AssertionError(description);
 }
 public static void main(String[] args) {
  ObdAuxSensorPolicy policy=new ObdAuxSensorPolicy();
  long start=10_000_000_000L;
  for(String pid:new String[]{"010C","0105","0142"}) {
   check(policy.mayPoll(pid,start),pid+" initially available");
   policy.failed(pid,start);
   check(policy.mayPoll(pid,start+1),pid+" first failure should not disable");
   policy.failed(pid,start+1);
   check(!policy.mayPoll(pid,start+2),pid+" second failure should cool down");
   check(!policy.mayPoll(pid,start+ObdAuxSensorPolicy.COOLDOWN_NS-1),pid+" cooldown prevents busy loop");
   check(policy.mayPoll(pid,start+ObdAuxSensorPolicy.COOLDOWN_NS+1),pid+" recovers after cooldown");
   policy.succeeded(pid,start+ObdAuxSensorPolicy.COOLDOWN_NS+2);
   check(policy.mayPoll(pid,start+3),pid+" success clears failures");
  }
  policy.failed("010C",start);
  check(policy.mayPoll("0105",start),"independent sensor cooldowns");
  check(!policy.mayPoll("010D",start),"speed never treated as optional sensor");
  check(!policy.mayPoll("FFFF",start),"unknown PID never scheduled");
  policy.failed("010C",start+2);
  check(!policy.mayPoll("010C",start+3),"repeated failures trigger cooldown");
  policy.succeeded("010C",start+ObdAuxSensorPolicy.COOLDOWN_NS+5);
  check(policy.mayPoll("010C",start+3),"healthy after success");
  System.out.println("PASS: ObdAuxSensorPolicy "+assertions+" assertions");
 }
}
