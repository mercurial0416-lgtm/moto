package com.motohub.next;
import java.io.File;
import java.util.Locale;

/** Compares explicitly labelled ride types only. No home/work inference from coordinates. */
final class RideTypeComparison {
 RideTypeComparison(){}
 static final class Totals {
  int count;double km;long movingMs;
  final java.util.Set<String> sessions=new java.util.HashSet<>();
  void add(RideInsights.Result ride){add(ride.distanceKm,ride.movingMillis);}
  void add(RideDistanceCache.Summary ride){add(ride.distanceKm,ride.movingMillis);}
  void add(double distance,long moving){add("",distance,moving);}
  void add(String session,double distance,long moving){
   if(session.isEmpty()||sessions.add(session))count++;
   addDistance(distance,moving);
  }
  void addDistance(double distance,long moving){km+=distance;movingMs+=moving;}
  void removeCount(String session){if(sessions.remove(session))count--;}
  String text(){return count+"회 · "+String.format(Locale.KOREA,"%.1f km",km)+
    " · 이동 "+(movingMs/60000)+"분"+(movingMs>0?
    String.format(Locale.KOREA," · 이동평균 %.1f km/h",km*3600000.0/movingMs):"");}
 }
 final Totals commuteNow=new Totals(),commutePrev=new Totals();
 final Totals windingNow=new Totals(),windingPrev=new Totals();
 private final java.util.Map<String,Boolean> commuteBuckets=new java.util.HashMap<>();
 private final java.util.Map<String,Boolean> windingBuckets=new java.util.HashMap<>();
 /** Earliest chronological segment determines the logical ride's count bucket. */
 private static void addGrouped(Totals recent,Totals earlier,java.util.Map<String,Boolean> buckets,
    String session,boolean current,double distance,long moving){
  Totals target=current?recent:earlier;
  if(session.isEmpty()){target.add("",distance,moving);return;}
  Boolean first=buckets.putIfAbsent(session,current);
  if(first==null){target.add(session,distance,moving);return;}
  if(first&&!current){
   recent.removeCount(session);earlier.add(session,distance,moving);
   buckets.put(session,false);
  }else target.addDistance(distance,moving);
 }
 void add(File file,long now,RideInsights.Result ride){
  add(file,now,ride.distanceKm,ride.movingMillis);
 }
 void add(File file,long now,RideDistanceCache.Summary ride){
  add(file,now,ride.distanceKm,ride.movingMillis);
 }
 void add(File file,long now,double distanceKm,long movingMillis){
  long stamp=RideMonthlyReport.timestamp(file);
  if(stamp<=0||stamp>now||now-stamp>60L*24*3600000||
     !Double.isFinite(distanceKm)||distanceKm<0||movingMillis<0)return;
  String category=RideCategory.read(file);
  boolean current=now-stamp<30L*24*3600000;
  String session=RideMonthlyReport.sessionKey(file);
  if(RideCategory.COMMUTE.equals(category))
   addGrouped(commuteNow,commutePrev,commuteBuckets,session,current,distanceKm,movingMillis);
  else if(RideCategory.WINDING.equals(category))
   addGrouped(windingNow,windingPrev,windingBuckets,session,current,distanceKm,movingMillis);
 }
 String report(){
  return "직접 지정한 라이딩 분류 기준 (GPS 위치로 출퇴근 여부를 추정하지 않음)"+
   "\n출퇴근 최근 30일: "+commuteNow.text()+
   "\n출퇴근 이전 30일: "+commutePrev.text()+
   "\n와인딩 최근 30일: "+windingNow.text()+
   "\n와인딩 이전 30일: "+windingPrev.text();
 }
}
