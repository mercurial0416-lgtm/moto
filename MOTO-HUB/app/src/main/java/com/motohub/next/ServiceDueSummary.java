package com.motohub.next;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
/** Read-only remaining service schedule. Both independent due triggers remain visible. */
final class ServiceDueSummary {
 private ServiceDueSummary(){}
 static String caption(String date,double nextKm,String today,double odo){
  String km="",days="";
  if(Double.isFinite(nextKm)&&nextKm>0&&Double.isFinite(odo)&&odo>=0){
   double diff=nextKm-odo;
   km=diff<=0?String.format(Locale.KOREA,"%,.0f km 초과",Math.abs(diff)):String.format(Locale.KOREA,"%,.0f km 남음",diff);
  }
  if(date!=null&&!date.isEmpty())try{
   long left=ChronoUnit.DAYS.between(LocalDate.parse(today),LocalDate.parse(date));
   days=left<0?(-left)+"일 경과":left==0?"오늘":"D-"+left;
  }catch(Exception ignored){}
  if(!days.isEmpty()&&!km.isEmpty())return days+" · "+km;
  return !days.isEmpty()?days:!km.isEmpty()?km:"미설정";
 }
 static String nearest(java.util.List<String> dates,java.util.List<Double> kms,String today,double odo){
  long nearestDay=Long.MAX_VALUE;double nearestKm=Double.POSITIVE_INFINITY;
  for(String date:dates)try{long delta=ChronoUnit.DAYS.between(LocalDate.parse(today),LocalDate.parse(date));if(delta<nearestDay)nearestDay=delta;}catch(Exception ignored){}
  if(Double.isFinite(odo)&&odo>=0)for(Double km:kms)if(km!=null&&Double.isFinite(km)&&km>0)nearestKm=Math.min(nearestKm,km-odo);
  String day=nearestDay==Long.MAX_VALUE?"":nearestDay<0?(-nearestDay)+"일 경과":nearestDay==0?"오늘":"D-"+nearestDay;
  String km=nearestKm==Double.POSITIVE_INFINITY?"":nearestKm<=0?String.format(Locale.KOREA,"%,.0f km 초과",Math.abs(nearestKm)):String.format(Locale.KOREA,"%,.0f km",nearestKm);
  return !day.isEmpty()&&!km.isEmpty()?day+" / "+km:!day.isEmpty()?day:!km.isEmpty()?km:"미설정";
 }
}
