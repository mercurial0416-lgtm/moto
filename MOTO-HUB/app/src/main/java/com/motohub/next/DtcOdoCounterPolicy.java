package com.motohub.next;

/**
 * A PID 0131 counter can decrease when diagnostic information is cleared,
 * and must saturate at 65535 km rather than wrap. Never infer lifetime
 * distance across a reset or after saturation without re-calibration.
 */
final class DtcOdoCounterPolicy {
 enum Decision { ACCEPT, RESET, SATURATED, IMPLAUSIBLE }
 private DtcOdoCounterPolicy(){}
 static boolean valid(int km){return km>=0&&km<=65535;}
 static Decision check(int previous,int current,long elapsedMs){
  if(!valid(previous)||!valid(current)||elapsedMs<0)return Decision.IMPLAUSIBLE;
  if(current<previous)return Decision.RESET;
  if(current==65535)return Decision.SATURATED;
  // Prevent single spurious replies from suddenly adding hundreds of km.
  // Two km margin for coarse integer counter quantization / slow polling.
  double maxDelta=2.0+(elapsedMs*220.0/3_600_000.0);
  return current-previous<=maxDelta?Decision.ACCEPT:Decision.IMPLAUSIBLE;
 }
 static double estimated(double dashAtAnchor,int counterAtAnchor,int counterNow){
  if(!Double.isFinite(dashAtAnchor)||dashAtAnchor<0||
     dashAtAnchor>2_000_000||!valid(counterAtAnchor)||!valid(counterNow)||
     counterNow<counterAtAnchor||counterNow==65535)return Double.NaN;
  double total=dashAtAnchor+(counterNow-counterAtAnchor);
  return total<=2_000_000?total:Double.NaN;
 }
}
