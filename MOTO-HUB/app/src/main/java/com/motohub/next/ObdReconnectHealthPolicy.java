package com.motohub.next;

/** Pure bounded connected-session health. A historic lifetime sample total cannot
 * grant a newly reconnected adapter a clean retry budget after one valid reply. */
final class ObdReconnectHealthPolicy {
 static final int STABLE_SPEED_SAMPLES=30;
 private ObdReconnectHealthPolicy(){}
 static int acceptValidSpeed(int current){
  if(current<0)return 1;
  return current>=STABLE_SPEED_SAMPLES?STABLE_SPEED_SAMPLES:current+1;
 }
 static boolean stable(int current){return current>=STABLE_SPEED_SAMPLES;}
}
