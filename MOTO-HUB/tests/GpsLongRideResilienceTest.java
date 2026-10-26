package com.motohub.next;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

/** Disk-level long ride, GPS outage and interrupted-session recovery simulation. */
public final class GpsLongRideResilienceTest {
 private static int count;
 private static void check(boolean v,String name){count++;if(!v)throw new AssertionError(name);}
 public static void main(String[] args)throws Exception{
  File root=Files.createTempDirectory("moto-24h-ride").toFile();
  File dir=TrackFile.directory(root);
  File track=new File(dir,"1790000000000-"+UUID.randomUUID()+".track");
  long start=1790000000000L;
  try(FileOutputStream out=new FileOutputStream(track)){
   for(int i=0;i<17280;i++){
    String line=TrackFile.point(37.0,127.0+i*0.00008,5,start+i*5000L);
    out.write(line.getBytes(StandardCharsets.UTF_8));
   }
   // 35-minute receiver blackout: never bridge its missing 100km as motion.
   long gapStart=start+(17280-1)*5000L+2_100_000L;
   out.write(TrackFile.point(37.0,129.0,5,gapStart).getBytes(StandardCharsets.UTF_8));
   out.write(TrackFile.point(37.0,129.00008,5,gapStart+5000L).getBytes(StandardCharsets.UTF_8));
   out.getFD().sync();
  }
  check(track.length()<TrackFile.MAX_BYTES,"multi-day bounded file size");
  check(TrackFile.read(track,(a,b,c,d)->{})==17282,"strictly ordered committed points");
  TrackCompletion.mark(track,17282);
  check(new File(track+".done").isFile(),"verified completed marker");
  RideInsights.Result result=RideInsights.analyze(track);
  check(result.points==17282,"24h stream intact");
  check(result.distanceKm>50&&result.distanceKm<200,"outage does not fabricate 100km jump");
  check(result.gpsCoveragePercent()<100,"outage lowers observed GPS coverage");
  // Force stop/OS eviction: committed rows remain untouched, partial final row is recoverable.
  File crash=new File(dir,"1791000000000-"+UUID.randomUUID()+".track");
  String valid=TrackFile.point(37,127,5,start)+TrackFile.point(37.0001,127,5,start+5000);
  Files.write(crash.toPath(),(valid+"37.2,127.").getBytes(StandardCharsets.UTF_8));
  check(!new File(crash+".done").exists(),"interrupted session has no completed marker");
  TrackRecovery.Result recovered=TrackRecovery.recover(crash);
  check(recovered.points==2&&recovered.discardedBytes==9,"only uncommitted trailing GPS trimmed");
  check(new File(crash+".recovered").isFile(),"recoverable status marked separately");
  check(Files.readString(crash.toPath()).equals(valid),"original durable points preserved");
  check(RideInsights.analyze(crash).points==2,"recovered track readable offline");
  check(!RideMileageIntegrity.validCompleted(new File(dir,"unrelated.track")),"arbitrary files excluded");
  System.out.println("PASS GpsLongRideResilienceTest "+count);
 }
}
