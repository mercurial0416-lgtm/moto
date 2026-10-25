package com.motohub.next;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
public final class RidePaceSegmentsTest {
 static int count=0;
 static void ok(boolean good,String why){count++;if(!good)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception{
  ok(RidePaceSegments.bucketIndex(-1)==-1,"negative");
  ok(RidePaceSegments.bucketIndex(0)==0,"first bucket");
  ok(RidePaceSegments.bucketIndex(899999)==0,"before boundary");
  ok(RidePaceSegments.bucketIndex(900000)==1,"after boundary");
  ok(RidePaceSegments.bucketIndex(96*900000L)==-1,"bounded max");
  RidePaceSegments.Bucket bucket=new RidePaceSegments.Bucket();
  bucket.distanceKm=1.4;bucket.observedMs=600000;bucket.movingMs=300000;
  ok(RidePaceSegments.describe(0,bucket).contains("1.4 km"),"distance");
  ok(RidePaceSegments.describe(1,bucket).contains("15~30분"),"time labels");
  ok(RidePaceSegments.describe(-1,bucket).contains("없음"),"bad index");
  File track=File.createTempFile("moto-segment-test-", ".track");
  try{
   long t=1_700_000_000_000L;
   String data=TrackFile.point(37.0,127.0,5,t)+
      TrackFile.point(37.00010,127.0,5,t+10000)+
      TrackFile.point(37.00010,127.0,5,t+20000)+
      TrackFile.point(37.1,127.0,5,t+901000)+
      TrackFile.point(37.10010,127.0,5,t+911000);
   Files.write(track.toPath(),data.getBytes(StandardCharsets.UTF_8));
   String result=RidePaceSegments.analyze(track);
   ok(result.contains("0~15분"),"first segment");
   ok(result.contains("15~30분"),"second segment");
   ok(result.contains("GPS 미수신 시간은 정차로 계산하지 않아요"),"missing data honest");
   ok(!result.contains("좌표: 37."),"no raw GPS dump");
  }finally{track.delete();}
  System.out.println("PASS RidePaceSegmentsTest "+count);
 }
}
