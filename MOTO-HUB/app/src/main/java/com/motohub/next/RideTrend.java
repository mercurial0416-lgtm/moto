package com.motohub.next;
import java.util.Locale;
final class RideTrend {
 private RideTrend(){}
 static String compare(double current,double previous){
  if(!Double.isFinite(current)||!Double.isFinite(previous)||current<0||previous<0)return "거리 비교 불가";
  if(previous==0)return current==0?"이전 30일 대비 동일":"이전 30일 기록 없음";
  double change=(current-previous)/previous*100;
  return String.format(Locale.KOREA,"이전 30일 대비 %s%.0f%%",change>0?"+":"",change);
 }
}
