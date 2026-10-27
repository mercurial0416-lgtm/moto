package com.motohub.next;

import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import static org.junit.Assert.*;

/** A stopped bike should not be reported as moving or a GPS outage. */
public final class RideInsightsRestPeakTest {
 @Test public void detectsRealStationaryStreakSpeedPeakAndGpsGap()throws Exception{
  File f=Files.createTempFile("moto-ride-rest",".track").toFile();
  try{
   long at=1_792_120_000_000L;
   StringBuilder gps=new StringBuilder();
   for(int i=0;i<=18;i++){
    gps.append(TrackFile.point(37.0+(i%2)*0.000001,127.0,4,at+i*10_000));
   }
   gps.append(TrackFile.point(37.001,127.0,4,at+190_000)); // valid 40km/h segment
   gps.append(TrackFile.point(37.001,127.0,4,at+260_000)); // 70s outage
   Files.writeString(f.toPath(),gps.toString());
   RideInsights.Result result=RideInsights.analyze(f);
   assertEquals(1,result.restStops);
   assertEquals(1,result.gpsGaps);
   assertTrue(result.peakKmh>10);
   assertTrue(RideMapOverlayPolicy.validPeak(result.peakKmh,result.peakLatitude,result.peakLongitude));
   assertTrue(result.distanceKm>0.05);
  }finally{Files.deleteIfExists(f.toPath());}
 }
}
