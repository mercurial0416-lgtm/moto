package com.motohub.next;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
/** Safe speed summaries: GPS outages must never be counted as real stops. */
public final class RideInsightsMovingAverageTest {
 private static int checks;
 private static void ok(boolean valid,String why){checks++;if(!valid)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception{
  File dir=Files.createTempDirectory("moto-moving-average").toFile();
  File ride=new File(dir,"ride.track");
  try{
   long start=1792120000000L;
   StringBuilder gps=new StringBuilder();
   gps.append(TrackFile.point(37,127,5,start));
   gps.append(TrackFile.point(37.001,127,5,start+10_000)); // ~40 km/h
   gps.append(TrackFile.point(37.001,127,5,start+70_000)); // 60 s gap (no distance)
   gps.append(TrackFile.point(37.002,127,5,start+80_000)); // ~40 km/h
   Files.writeString(ride.toPath(),gps.toString());
   RideInsights.Result r=RideInsights.analyze(ride);
   ok(r.points==4,"GPS samples preserved");
   ok(r.durationMillis==80_000,"elapsed duration");
   ok(r.movingMillis==20_000,"only valid moving intervals observed");
   ok(r.movingAverageKmh>r.averageKmh,"moving average must exceed elapsed average");
   ok(r.distanceKm>0.2&&r.distanceKm<0.25,"distance excludes 60s outage");
   ok(r.movingAverageKmh<=250,"safe speed cap");
   RideInsights.Result bad=new RideInsights.Result(0,0,0,0,10,5,3000);
   ok(bad.durationMillis==0&&bad.movingMillis==0&&bad.movingAverageKmh==0,"invalid duration guarded");
   System.out.println("PASS ride moving metrics: "+checks+" checks");
  }finally{ride.delete();dir.delete();}
 }
}
