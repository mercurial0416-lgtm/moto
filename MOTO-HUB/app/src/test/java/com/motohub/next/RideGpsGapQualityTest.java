package com.motohub.next;
import org.junit.Test;
import java.io.*;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;
public final class RideGpsGapQualityTest {
 @Test public void poorFixInMiddleCountsOneOutageAndNoTravelAcrossIt()throws Exception{
  File f=Files.createTempFile("moto-gap",".track").toFile();
  try{
   long t=1792100000000L;
   String gps=TrackFile.point(37.0,127.0,4,t)+
    TrackFile.point(37.001,127.0,5,t+10000)+
    TrackFile.point(37.02,127.0,85,t+20000)+
    TrackFile.point(37.021,127.0,4,t+30000)+
    TrackFile.point(37.022,127.0,4,t+40000);
   Files.write(f.toPath(),gps.getBytes(StandardCharsets.UTF_8));
   RideInsights.Result result=RideInsights.analyze(f);
   assertEquals(1,result.gpsGaps);
   assertTrue(result.gpsCoveragePercent()<70);
   assertTrue(result.gpsQuality().contains("GPS 수신률"));
   assertTrue(result.distanceKm<0.3);
  }finally{Files.deleteIfExists(f.toPath());}
 }
}
