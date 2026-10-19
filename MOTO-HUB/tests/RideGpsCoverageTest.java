package com.motohub.next;
import java.io.*;import java.nio.file.*;
/** GPS timeline confidence is observed-data coverage, not an invented stopped timer. */
public final class RideGpsCoverageTest {
 private static int checks;
 static void ok(boolean condition,String why){checks++;if(!condition)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception{
  File track=Files.createTempFile("moto-coverage",".track").toFile();
  try{
   long start=1792120000000L;StringBuilder raw=new StringBuilder();
   raw.append(TrackFile.point(37,127,5,start));
   raw.append(TrackFile.point(37,127,5,start+5000)); // Stationary noise must count as GPS observed.
   raw.append(TrackFile.point(37,127,5,start+10000));
   raw.append(TrackFile.point(37,127,5,start+70000)); // 60s outage cannot become GPS-observed time.
   raw.append(TrackFile.point(37.001,127,5,start+80000));
   Files.writeString(track.toPath(),raw.toString());
   RideInsights.Result r=RideInsights.analyze(track);
   ok(r.durationMillis==80000,"full elapsed stays 80s");
   ok(r.gpsObservedMillis==20000,"only good continuous fixes counted");
   ok(r.gpsCoveragePercent()==25,"timeline coverage explicit");
   ok(r.movingMillis<=r.gpsObservedMillis,"moving subset of observed");
   RideInsights.Result bad=new RideInsights.Result(0,0,0,0,1,1,0,1234);
   ok(bad.gpsObservedMillis==0&&bad.gpsCoveragePercent()==0,"zero duration safe");
   System.out.println("PASS DE GPS coverage: "+checks+" checks");
  }finally{track.delete();}
 }
}
