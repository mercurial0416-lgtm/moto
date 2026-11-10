package com.motohub.next;

/**
 * Confirm that two adjacent, decoded PID 0131 reads agree within a small
 * increase before forwarding to the persistent odometer policy.
 *
 * A lone low glitch must not masquerade as a DTC reset; a lone high
 * glitch must not inflate the estimate. Independent read passes are
 * required, and the persisted bridge also checks time/distance plausibility.
 */
final class DtcCounterConfirmPolicy {
 private static final int MAX_ADJACENT_INCREASE_KM=4;
 private DtcCounterConfirmPolicy(){}

 static int confirmed(int priorCandidate,int current){
  if(!DtcOdoCounterPolicy.valid(priorCandidate)||
     !DtcOdoCounterPolicy.valid(current))return -1;
  if(current<priorCandidate)return -1; // wait for another low reading
  if(current-priorCandidate>MAX_ADJACENT_INCREASE_KM)return -1;
  return current;
 }
}