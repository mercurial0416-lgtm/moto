package com.motohub.next;
import java.time.*;
import java.time.temporal.ChronoUnit;

/** Pure 6-month GPS chart aggregator. Uses recorded ride time, never file import time. */
final class RideStatsSeries {
 final int[] count=new int[6];
 final double[] km=new double[6];
 final long[] minutes=new long[6];
 final String[] labels=new String[6];
 final YearMonth anchor;
 final ZoneId zone;
 int excluded=0, scanned=0;
 int invalidCompleted=0,oversize=0,unreadable=0,invalidMetrics=0;
 double totalKm=0;
 RideStatsSeries(YearMonth anchor){this(anchor,ZoneId.systemDefault());}
 RideStatsSeries(YearMonth anchor,ZoneId zone){
  if(anchor==null||zone==null)throw new IllegalArgumentException("기준 월 또는 시간대 없음");
  this.anchor=anchor;this.zone=zone;
  for(int i=0;i<6;i++){
   YearMonth month=anchor.minusMonths(5-i);
   labels[i]=month.getMonthValue()+"월";
  }
 }
 boolean add(long startMs,double distanceKm,long elapsedMs){
  if(startMs<=0||!Double.isFinite(distanceKm)||distanceKm<0||distanceKm>1000||elapsedMs<0||elapsedMs>4L*24*3600*1000){
   excluded++;return false;
  }
  YearMonth month=YearMonth.from(Instant.ofEpochMilli(startMs).atZone(zone));
  long index=ChronoUnit.MONTHS.between(anchor.minusMonths(5),month);
  if(index<0||index>=6)return false;
  int i=(int)index;count[i]++;km[i]+=distanceKm;minutes[i]+=elapsedMs/60000;totalKm+=distanceKm;return true;
 }
 /** Inclusive start of the six displayed local calendar months, including DST boundaries. */
 long earliestStartMs(){
  return anchor.minusMonths(5).atDay(1).atStartOfDay(zone)
   .toInstant().toEpochMilli();
 }
 /** A fixed-day rolling cutoff can silently discard the first days of six calendar months. */
 boolean inDisplayedPeriod(long startMs,long nowMs){
  return startMs>=earliestStartMs()&&startMs<=nowMs&&startMs>0;
 }
 long totalMinutes(){long sum=0;for(long value:minutes)sum+=value;return sum;}
 int rides(){int sum=0;for(int countValue:count)sum+=countValue;return sum;}
 double maxKm(){double result=0;for(double d:km)result=Math.max(result,d);return result;}
}
