package com.motohub.next;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;

/** Local ride-date filters. Imported GPX is dated by its recorded first point. */
final class RideHistoryPeriod {
 static final String ALL="전체 기간", TODAY="오늘", DAY="최근 24시간", THIS_MONTH="이번 달", WEEK="최근 7일", MONTH="최근 30일", QUARTER="최근 90일";
 static final String[] OPTIONS={ALL,TODAY,DAY,THIS_MONTH,WEEK,MONTH,QUARTER};
 private RideHistoryPeriod(){}
 static String normalize(String value){for(String option:OPTIONS)if(option.equals(value))return option;return ALL;}
 static String restore(String persisted,String saved){return normalize(saved==null?persisted:saved);}
 static boolean include(Long started,long now,String period){
  return include(started,now,period,ZoneId.systemDefault());
 }
 /** Calendar-based filters use local midnight; rolling filters use elapsed time (DST-safe). */
 static boolean include(Long started,long now,String period,ZoneId zone){
  String value=normalize(period);
  if(ALL.equals(value))return true; // Keep undated/corrupt records visible when unfiltered.
  if(started==null||started<=0||started>now)return false;
  if(TODAY.equals(value)||THIS_MONTH.equals(value)){
   java.time.LocalDate date=Instant.ofEpochMilli(now).atZone(zone).toLocalDate();
   if(THIS_MONTH.equals(value))date=date.withDayOfMonth(1);
   return started>=date.atStartOfDay(zone).toInstant().toEpochMilli();
  }
  long days=DAY.equals(value)?1:WEEK.equals(value)?7:MONTH.equals(value)?30:90;
  return now-started<days*86_400_000L;
 }
 static boolean include(File file,long now,String period){return include(RideHistoryLabel.startTime(file),now,period);}
}
