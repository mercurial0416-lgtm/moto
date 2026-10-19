package com.motohub.next;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
public final class RideHistoryPeriodTest {
 static int n=0;
 static void check(boolean yes,String message){n++;if(!yes)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception{
  long now=1_800_000_000_000L,day=86_400_000L;
  check(RideHistoryPeriod.ALL.equals(RideHistoryPeriod.normalize("invalid")),"invalid period fail-open");
  check(RideHistoryPeriod.WEEK.equals(RideHistoryPeriod.restore("x",RideHistoryPeriod.WEEK)),"saved state precedence");
  check(RideHistoryPeriod.MONTH.equals(RideHistoryPeriod.restore(RideHistoryPeriod.MONTH,null)),"persisted period");
  check(RideHistoryPeriod.include((Long)null,now,RideHistoryPeriod.ALL),"undated visible in all");
  check(!RideHistoryPeriod.include((Long)null,now,RideHistoryPeriod.WEEK),"undated excluded in recent");
  check(RideHistoryPeriod.include(now-6*day,now,RideHistoryPeriod.WEEK),"six day ride");
  check(!RideHistoryPeriod.include(now-7*day,now,RideHistoryPeriod.WEEK),"seven day cutoff");
  check(RideHistoryPeriod.include(now-29*day,now,RideHistoryPeriod.MONTH),"month inclusion");
  check(!RideHistoryPeriod.include(now-30*day,now,RideHistoryPeriod.MONTH),"month cutoff");
  check(RideHistoryPeriod.include(now-89*day,now,RideHistoryPeriod.QUARTER),"quarter inclusion");
  check(!RideHistoryPeriod.include(now-90*day,now,RideHistoryPeriod.QUARTER),"quarter cutoff");
  check(!RideHistoryPeriod.include(now+1,now,RideHistoryPeriod.MONTH),"future ride excluded");
  java.time.ZoneId seoul=java.time.ZoneId.of("Asia/Seoul");
  long localNow=java.time.ZonedDateTime.of(2026,10,19,9,30,0,0,seoul).toInstant().toEpochMilli();
  long midnight=java.time.ZonedDateTime.of(2026,10,19,0,0,0,0,seoul).toInstant().toEpochMilli();
  long monthStart=java.time.ZonedDateTime.of(2026,10,1,0,0,0,0,seoul).toInstant().toEpochMilli();
  check(RideHistoryPeriod.include(midnight,localNow,RideHistoryPeriod.TODAY,seoul),"today starts at local midnight");
  check(!RideHistoryPeriod.include(midnight-1,localNow,RideHistoryPeriod.TODAY,seoul),"yesterday excluded from today");
  check(RideHistoryPeriod.include(monthStart,localNow,RideHistoryPeriod.THIS_MONTH,seoul),"month starts on local first");
  check(!RideHistoryPeriod.include(monthStart-1,localNow,RideHistoryPeriod.THIS_MONTH,seoul),"previous month excluded");
  check(RideHistoryPeriod.include(localNow-23*3600_000L,localNow,RideHistoryPeriod.DAY,seoul),"rolling 24 hours includes previous night");
  check(!RideHistoryPeriod.include(localNow-24*3600_000L,localNow,RideHistoryPeriod.DAY,seoul),"rolling 24 hours cutoff");
  check(!RideHistoryPeriod.include(localNow+1,localNow,RideHistoryPeriod.TODAY,seoul),"future excluded from today");
  check(RideHistoryPeriod.OPTIONS.length==7,"seven local date options");
  java.time.ZoneId berlin=java.time.ZoneId.of("Europe/Berlin");
  long dstNow=java.time.ZonedDateTime.of(2026,3,29,12,0,0,0,berlin).toInstant().toEpochMilli();
  long dstMidnight=java.time.ZonedDateTime.of(2026,3,29,0,0,0,0,berlin).toInstant().toEpochMilli();
  check(RideHistoryPeriod.include(dstMidnight,dstNow,RideHistoryPeriod.TODAY,berlin),"DST transition uses calendar midnight");
  check(!RideHistoryPeriod.include(dstMidnight-1,dstNow,RideHistoryPeriod.TODAY,berlin),"DST previous date excluded");
  Path dir=Files.createTempDirectory("moto-period-test");
  try{
   File imported=dir.resolve((now-1_000L)+"-00000000-0000-4000-8000-000000000001.track").toFile();
   long old=now-40*day;
   Files.write(imported.toPath(),("37.0,127.0,3.0,"+old+"\n").getBytes(StandardCharsets.UTF_8));
   Files.createFile(Paths.get(imported.getPath()+".imported"));
   check(!RideHistoryPeriod.include(imported,now,RideHistoryPeriod.MONTH),"GPX uses recorded date, not import date");
   check(RideHistoryPeriod.include(imported,now,RideHistoryPeriod.QUARTER),"GPX within 90d");
  }finally{try(java.util.stream.Stream<Path> files=Files.list(dir)){files.forEach(p->{try{Files.deleteIfExists(p);}catch(IOException e){throw new RuntimeException(e);}});}Files.deleteIfExists(dir);}
  System.out.println("PASS: "+n+" ride date filter assertions");
 }
}
