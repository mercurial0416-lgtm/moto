package com.motohub.next;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
/** Offline 24-hour simulation. No claim of battery, hardware or background Android runtime validation. */
public final class RideDayLongSimulationTest {
 private static int checks=0;
 private static void ok(boolean truth,String why){checks++;if(!truth)throw new AssertionError(why);}
 private static byte[] sha(Path path)throws Exception{return MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));}
 public static void main(String[] args)throws Exception{
  Path file=Files.createTempFile("moto-24h-gps", ".track");
  final long start=1792120000000L,interval=5000;
  final int total=(int)(24*3600*1000/interval)+1;
  try{
   try(BufferedWriter writer=Files.newBufferedWriter(file,StandardCharsets.UTF_8)){
    for(int i=0;i<total;i++){
     // Moving within a small radius: deterministic and valid without map network.
     double lat=37.0+(i%200)*0.00001;
     double lon=127.0;
     writer.write(TrackFile.point(lat,lon,6,start+i*interval));
    }
   }
   long expected=file.toFile().length();byte[] original=sha(file);
   ok(TrackFile.read(file.toFile(),(a,b,c,d)->{})==total,"24-hour points count");
   RideInsights.Result result=RideInsights.analyze(file.toFile());
   ok(result.points==total,"all samples summarized");
   ok(result.durationMillis==24L*3600*1000,"24-hour elapsed exact");
   ok(Double.isFinite(result.averageKmh)&&Double.isFinite(result.movingAverageKmh),"summary finite");
   ok(result.movingMillis<=result.durationMillis,"moving duration bounded");
   ok(Arrays.equals(original,sha(file))&&file.toFile().length()==expected,"byte exact GPS source preserved");
   ok(!GpsFixPolicy.acceptTime(start+121_001,start,start),"far future GPS rejected");
   ok(GpsFixPolicy.acceptTime(start+1000,start,start),"allowed small GPS future skew");
   ok(!GpsFixPolicy.acceptTime(start,start+10000,start),"out of order rejected");
   ok(!ObdTelemetryFreshness.speedFresh(100,12_101),"OBD reading expires after 12 seconds");
   ok(!ObdTelemetryFreshness.speedFresh(200,150),"monotonic clock reversal never fresh");
   System.out.println("PASS 24h simulated GPS: "+checks+" checks, "+total+" points, "+expected+" bytes");
  }finally{Files.deleteIfExists(file);}
 }
}
