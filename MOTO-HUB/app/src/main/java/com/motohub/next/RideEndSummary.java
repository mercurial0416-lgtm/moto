package com.motohub.next;

import java.util.Locale;

/** Snapshot of observed GPS metrics. No invented values or stored ECU data. */
final class RideEndSummary {
 private RideEndSummary(){}
 static String text(double km,long elapsedMs,long movingMs,long stationaryMs,int points){
  double safeKm=Double.isFinite(km)&&km>=0?km:0;
  long elapsed=Math.max(0,elapsedMs);
  long moving=Math.max(0,Math.min(elapsed,movingMs));
  long idle=Math.max(0,Math.min(elapsed-moving,stationaryMs));
  return String.format(Locale.KOREA,
   "GPS 거리 %.2f km\n기록 시간 %d분\n이동 %d분 · 정차 %d분 (관측 구간)\n저장된 위치 %d개",
   safeKm,elapsed/60000,moving/60000,idle/60000,Math.max(0,points));
 }
}
