package com.motohub.next;
import java.io.File;
import java.util.Locale;

/** Compares explicitly labelled ride types only. No home/work inference from coordinates. */
final class RideTypeComparison {
 private RideTypeComparison(){}
 static final class Totals {
  int count;double km;long movingMs;
  void add(RideInsights.Result ride){count++;km+=ride.distanceKm;movingMs+=ride.movingMillis;}
  String text(){return count+"회 · "+String.format(Locale.KOREA,"%.1f km",km)+
    " · 이동 "+(movingMs/60000)+"분"+(movingMs>0?
    String.format(Locale.KOREA," · 이동평균 %.1f km/h",km*3600000.0/movingMs):"");}
 }
 final Totals commuteNow=new Totals(),commutePrev=new Totals();
 final Totals windingNow=new Totals(),windingPrev=new Totals();
 void add(File file,long now,RideInsights.Result ride){
  long stamp=RideMonthlyReport.timestamp(file);
  if(stamp<=0||stamp>now||now-stamp>60L*24*3600000)return;
  String category=RideCategory.read(file);
  boolean current=now-stamp<30L*24*3600000;
  if(RideCategory.COMMUTE.equals(category))(current?commuteNow:commutePrev).add(ride);
  else if(RideCategory.WINDING.equals(category))(current?windingNow:windingPrev).add(ride);
 }
 String report(){
  return "직접 지정한 라이딩 분류 기준 (GPS 위치로 출퇴근 여부를 추정하지 않음)"+
   "\n출퇴근 최근 30일: "+commuteNow.text()+
   "\n출퇴근 이전 30일: "+commutePrev.text()+
   "\n와인딩 최근 30일: "+windingNow.text()+
   "\n와인딩 이전 30일: "+windingPrev.text();
 }
}
