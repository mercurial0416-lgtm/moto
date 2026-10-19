package com.motohub.next;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
public final class CaImprovementsTest {
 static int assertions=0;
 static void check(boolean yes,String message){assertions++;if(!yes)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception {
  check(!GarageBikeStatus.sold(null),"null sold date not archived");
  check(!GarageBikeStatus.sold("   "),"blank sold date not archived");
  check(GarageBikeStatus.sold("2026-10-18"),"sold date archived");
  check(GarageBikeStatus.label("커브","2026-10-18").contains("판매 완료"),"archived label");
  check(GarageBikeStatus.rank("")<GarageBikeStatus.rank("2026-10-18"),"active before sold");
  check(MaintenancePreset.next(5000,0)==6000,"1,000 km suggestion");
  check(MaintenancePreset.next(5000,1)==8000,"3,000 km suggestion");
  check(MaintenancePreset.next(5000,2)==10000,"5,000 km suggestion");
  check(MaintenancePreset.next(5000,3)==15000,"10,000 km suggestion");
  check(MaintenancePreset.next(5000,4)==0,"not set");
  for(double invalid:new double[]{Double.NaN,Double.POSITIVE_INFINITY,-1,10_000_001}){
   boolean failed=false;try{MaintenancePreset.next(invalid,0);}catch(IllegalArgumentException ok){failed=true;}
   check(failed,"invalid odometer accepted");
  }
  boolean badChoice=false;try{MaintenancePreset.next(5000,-1);}catch(IllegalArgumentException ok){badChoice=true;}
  check(badChoice,"invalid preset accepted");
  Path dir=Files.createTempDirectory("moto-ca-ride");
  try{
   File file=dir.resolve("sample.track").toFile();
   long start=1_793_000_000_000L;
   String track=TrackFile.point(37,127,4,start)+TrackFile.point(37.001,127,4,start+10_000)
    +TrackFile.point(37.001,127,4,start+20_000);
   Files.writeString(file.toPath(),track,StandardCharsets.UTF_8);
   RideInsights.Result result=RideInsights.analyze(file);
   check(result.points==3,"ride point count");
   check(result.movingMillis==10_000,"moving time includes valid moving segment, not stationary segment");
   check(result.durationMillis==20_000,"total ride duration unchanged");
   check(result.distanceKm>0.1&&result.distanceKm<0.12,"distance unchanged");
  }finally{Files.deleteIfExists(dir.resolve("sample.track"));Files.deleteIfExists(dir);}
  System.out.println("PASS: "+assertions+" CA pure-JVM assertions");
 }
}
