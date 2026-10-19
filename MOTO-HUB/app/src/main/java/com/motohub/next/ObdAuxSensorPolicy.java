package com.motohub.next;

import java.util.HashMap;
import java.util.Map;

/**
 * A low-priority RPM/coolant/voltage read must never tear down a healthy
 * speed-driven automatic ride. This policy is only used on the OBD worker
 * thread; it neither writes to the ECU nor persists telemetry.
 */
final class ObdAuxSensorPolicy {
 static final long COOLDOWN_NS = 60_000_000_000L;
 private static final int FAILURE_THRESHOLD = 2;
 private final Map<String,Integer> failures = new HashMap<>();
 private final Map<String,Long> suppressedUntil = new HashMap<>();
 private final Map<String,Integer> totalFailures = new HashMap<>();
 private final Map<String,Integer> totalSuccesses = new HashMap<>();
 private final Map<String,Long> lastSuccessNs = new HashMap<>();

 boolean mayPoll(String pid,long nowNs) {
  if(!known(pid))return false;
  Long until = suppressedUntil.get(pid);
  if(until == null)return true;
  // Subtracting avoids overflow when monotonic clocks have large values.
  return nowNs - until >= 0;
 }
 void succeeded(String pid,long nowNs) {
  if(!known(pid))return;
  totalSuccesses.put(pid,totalSuccesses.getOrDefault(pid,0)+1);
  lastSuccessNs.put(pid,nowNs);
  failures.remove(pid);
  suppressedUntil.remove(pid);
 }
 void failed(String pid,long nowNs) {
  if(!known(pid))return;
  totalFailures.put(pid,totalFailures.getOrDefault(pid,0)+1);
  int count=failures.getOrDefault(pid,0)+1;
  if(count>=FAILURE_THRESHOLD){
   failures.remove(pid);
   suppressedUntil.put(pid,nowNs+COOLDOWN_NS);
  }else failures.put(pid,count);
 }
 /** Local diagnostic only; no Bluetooth IDs or ECU values are persisted. */
 String summary(long nowNs) {
  StringBuilder b=new StringBuilder();
  String[] pids={"010C","0105","0142"};
  String[] names={"RPM","수온","전압"};
  for(int i=0;i<pids.length;i++){
   String pid=pids[i];
   if(i>0)b.append(" · ");
   b.append(names[i]).append(" 정상 ").append(totalSuccesses.getOrDefault(pid,0))
    .append("/오류 ").append(totalFailures.getOrDefault(pid,0));
   Long until=suppressedUntil.get(pid);
   if(until!=null&&nowNs<until)b.append(" (일시중지 ").append((until-nowNs+999_999_999L)/1_000_000_000L).append("초)");
   else if(lastSuccessNs.containsKey(pid)){
    long age=Math.max(0,(nowNs-lastSuccessNs.get(pid))/1_000_000_000L);
    b.append(" (최근 ").append(age).append("초 전)");
   }
  }
  return b.toString();
 }
 private static boolean known(String pid) {
  return "010C".equals(pid)||"0105".equals(pid)||"0142".equals(pid);
 }
}
