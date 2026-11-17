package com.motohub.next;
/** Keep ECU speed/GPS start priority but collect consented odometer before
 * the old minute-scale / parking-delay telemetry interval. */
final class ObdOdoPollPolicy {
 private ObdOdoPollPolicy(){}
 static boolean due(int completedSpeedPolls,boolean speedValid,boolean supportConfirmed,boolean consented){
  if(!speedValid||!supportConfirmed||!consented||completedSpeedPolls<0)return false;
  return completedSpeedPolls==4||(completedSpeedPolls>0&&completedSpeedPolls%60==0);
 }
}
