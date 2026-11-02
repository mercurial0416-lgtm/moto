package com.motohub.next;

import org.junit.Test;
import static org.junit.Assert.*;
import java.time.*;

public final class Ux109RideJournalChartObdTest {
 private static final String TRACK="1762063200000-12345678-1234-4234-9234-123456789abc.track";
 @Test public void ridePhotoNameCannotEscapeLocalRoot(){
  String name=RidePhotoJournal.photoName(TRACK);
  assertEquals("ride-photo-1762063200000-12345678-1234-4234-9234-123456789abc.jpg",name);
  assertTrue(BackupFormat.isPhoto("photos/"+name));
  assertTrue(BackupFormat.allowed("photos/"+name,false));
  assertEquals(BackupFormat.MAX_TRACK_BYTES,BackupFormat.entryLimit("photos/"+name));
  assertTrue(BackupFormat.isPhoto("photos/garage-photo-12345678-1234-4234-9234-123456789abc.jpg"));
  assertFalse(BackupFormat.allowed("photos/../"+name,false));
  assertFalse(BackupFormat.allowed("photos/ride-photo-invalid.jpg",false));
  try{RidePhotoJournal.photoName("../"+TRACK);fail("unsafe path");}
  catch(IllegalArgumentException expected){}
 }
 @Test public void sixMonthStatsGroupByRealRideDateInSeoul(){
  RideStatsSeries series=new RideStatsSeries(YearMonth.of(2026,11));
  long october=LocalDate.of(2026,10,30).atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
  long june=LocalDate.of(2026,6,1).atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
  long april=LocalDate.of(2026,4,30).atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
  assertTrue(series.add(october,50.5,2*3600_000));
  assertTrue(series.add(june,12,600_000));
  assertFalse(series.add(april,200,10_000));
  assertFalse(series.add(october,Double.NaN,10_000));
  assertEquals("6월",series.labels[0]);
  assertEquals("11월",series.labels[5]);
  assertEquals(2,series.rides());
  assertEquals(50.5,series.km[4],.0001);
  assertEquals(62.5,series.totalKm,.0001);
  assertEquals(1,series.excluded);
  assertEquals(120,series.minutes[4]);
 }
 @Test public void elmErrorsCannotArmRideEvenWhenStaleSensorFrameFollows(){
  for(String status:new String[]{"NO DATA","UNABLE TO CONNECT","STOPPED","CAN ERROR","BUS ERROR","BUFFER FULL","?","DATA ERROR"}){
   assertFalse("rejected "+status,ObdReadOnlyProtocol.replyError(status).isEmpty());
   assertEquals(-1,ObdReadOnlyProtocol.speedKmh("41 0D 35\r"+status+"\r>"));
   assertEquals(ObdPidCapabilities.Support.UNKNOWN,
     ObdPidCapabilities.fromResponses("41 00 FF FF FF FF\r"+status,null).forPid("010D"));
  }
  assertEquals(53,ObdReadOnlyProtocol.speedKmh("7E8 03 41 0D 35\r>"));
  assertEquals(53,ObdReadOnlyProtocol.speedKmh("410D35\r>"));
  assertEquals(-1,ObdReadOnlyProtocol.speedKmh("41 0D 35\r41 0D 36\r>"));
  assertEquals(-1,ObdReadOnlyProtocol.speedKmh(null));
  assertThrows(IllegalArgumentException.class,()->ObdReadOnlyProtocol.query("04"));
 }
}
