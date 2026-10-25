package com.motohub.next;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class CbQualityTest {
 static int checks;
 static void check(boolean good,String description){checks++;if(!good)throw new AssertionError(description);}
 static void close(double a,double b,String description){check(Math.abs(a-b)<.02,description+": "+a+" vs "+b);}
 public static void main(String[] args)throws Exception {
  check(RideMotionGate.countDistance(10000,100,5,5),"normal GPS segment");
  check(RideMotionGate.countDistance(30000,100,5,5),"boundary gap accepted");
  for(long gap:new long[]{-1,0,30001,60000,600000})check(!RideMotionGate.countDistance(gap,100,5,5),"GPS outage boundary: "+gap);
  for(double quality:new double[]{-1,40.1,100,Double.NaN,Double.POSITIVE_INFINITY})check(!RideMotionGate.countDistance(10000,50,5,quality),"poor new accuracy");
  check(!RideMotionGate.countDistance(10000,50,50,5),"poor last accuracy");
  check(!RideMotionGate.countDistance(10000,1000,5,5),"impossible speed");
  check(!RideMotionGate.countDistance(10000,Double.NaN,5,5),"nonfinite distance");
  check(!RideMotionGate.countDistance(10000,-2,5,5),"negative distance");
  check(RideReportBars.render(0,0).equals("············"),"empty bars");
  check(RideReportBars.render(10,20).equals("■■■■■■······"),"half bar");
  check(RideReportBars.render(20,20).equals("■■■■■■■■■■■■"),"full bar");
  check(RideReportBars.render(21,20).equals("비교 불가"),"invalid bar");
  List<MaintenanceNoticeOrder.Notice> tasks=Arrays.asList(
   new MaintenanceNoticeOrder.Notice("체인", "2026-09-01", "2026-11-01", 0),
   new MaintenanceNoticeOrder.Notice(" 체인 ", "2026-10-01", "2026-10-17", 0),
   new MaintenanceNoticeOrder.Notice(" ", "2026-10-01", "2026-10-01", 0));
  List<MaintenanceNoticeOrder.Notice> result=MaintenanceNoticeOrder.prioritized(tasks,"2026-10-18",1200);
  check(result.size()==1,"whitespace-equivalent maintenance deduplicated");
  check(result.get(0).type.equals("체인") && result.get(0).nextDate.equals("2026-10-17"),"most recent maintenance retained");
  Path folder=Files.createTempDirectory("moto-cb-motion");
  try{
   File track=folder.resolve("motion.track").toFile();long t=1792100000000L;
   Files.writeString(track.toPath(),TrackFile.point(37.0000,127,5,t)+
       TrackFile.point(37.0003,127,5,t+10_000)+
       TrackFile.point(37.1000,127,5,t+310_000)+ // 5-min outage, don't add ten kilometres
       TrackFile.point(37.1003,127,5,t+320_000));
   RideInsights.Result ride=RideInsights.analyze(track);
   check(ride.points==4,"all committed samples remain");
   check(ride.distanceKm>0.04&&ride.distanceKm<0.09,"do not bridge GPS outage");
   check(ride.movingMillis==20_000,"do not inflate moving time over gap");
   close(ride.peakKmh,12,"instantaneous segment speed");
   Files.writeString(track.toPath(),TrackFile.point(37.0,127,5,t)+TrackFile.point(37.01,127,90,t+10_000)+TrackFile.point(37.0103,127,5,t+20_000));
   ride=RideInsights.analyze(track);
   close(ride.distanceKm,0,"reject in-between low accuracy point and bridge");
  }finally{try(var walk=Files.walk(folder)){walk.sorted(Comparator.reverseOrder()).forEach(f->{try{Files.deleteIfExists(f);}catch(Exception ignored){}});}}
  System.out.println("PASS CB quality: "+checks+" assertions");
 }
}
