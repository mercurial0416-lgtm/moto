package com.motohub.next;
import java.util.Locale;
/** Local, opt-in rolling 30-day riding target. No GPS or background tracking starts here. */
final class RideGoals {
 private RideGoals() {}
 static final int[] OPTIONS={0,100,300,500,1000};
 static int normalize(int value){for(int option:OPTIONS)if(value==option)return value;return 0;}
 static int progressPercent(double distanceKm,int targetKm){
  if(normalize(targetKm)==0||!Double.isFinite(distanceKm)||distanceKm<=0)return 0;
  return (int)Math.min(100,Math.round(distanceKm/targetKm*100.0));
 }
 static String label(double distanceKm,int targetKm){
  if(normalize(targetKm)==0)return "거리 목표 꺼짐";
  if(!Double.isFinite(distanceKm)||distanceKm<0)distanceKm=0;
  return String.format(Locale.KOREA,"최근 30일 목표 %.1f / %d km · %d%% 달성",distanceKm,targetKm,progressPercent(distanceKm,targetKm));
 }
}
