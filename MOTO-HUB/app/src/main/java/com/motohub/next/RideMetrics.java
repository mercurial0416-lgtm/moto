package com.motohub.next;
/** Average speed from elapsed time, not only GPS sampling intervals. */
final class RideMetrics {
 private RideMetrics(){}
 static String averageText(double km,long elapsedSeconds){
  if(!Double.isFinite(km)||km<0||elapsedSeconds<=0)return "—";
  return String.format(java.util.Locale.KOREA,"%.1f km/h",Math.min(250,km*3600/elapsedSeconds));
 }
}
