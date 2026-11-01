package com.motohub.next;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
public class TrackPrivacyTest {
 @Test public void masksBothEndsAndPreservesSource() throws Exception {
  File file=File.createTempFile("privacy-", ".track");
  try {
   StringBuilder data=new StringBuilder();long t=1750000000000L;
   for(int i=0;i<12;i++)data.append(TrackFile.point(37.0,127.0+i*.004,3,t+i*10000));
   Files.write(file.toPath(),data.toString().getBytes(StandardCharsets.UTF_8));
   ByteArrayOutputStream out=new ByteArrayOutputStream();
   int kept=TrackPrivacy.gpx(file,out,500);
   String xml=out.toString("UTF-8");
   assertTrue(kept>=2);assertFalse(xml.contains("lon=\"127.0\""));
   assertFalse(xml.contains("lon=\"127.044\""));
   assertEquals(data.toString(),Files.readString(file.toPath()));
   assertTrue(xml.contains("<trkseg>"));
  } finally {file.delete();}
 }
 @Test public void refusesShortRouteBeforeWriting() throws Exception {
  File f=File.createTempFile("privacy-short", ".track");
  try {
   Files.write(f.toPath(),(TrackFile.point(37,127,2,1000)+TrackFile.point(37,127.0001,2,2000)+TrackFile.point(37,127.0002,2,3000)).getBytes(StandardCharsets.UTF_8));
   ByteArrayOutputStream out=new ByteArrayOutputStream();
   try {TrackPrivacy.gpx(f,out,1000);fail("must reject");}catch(IOException expected){}
   assertEquals(0,out.size());
  }finally{f.delete();}
 }
 @Test public void unmaskedGpxMaintainsCompatibility() throws Exception {
  File f=File.createTempFile("privacy-legacy", ".track");try{
   Files.write(f.toPath(),(TrackFile.point(37,127,2,1000)+TrackFile.point(37,127.005,2,2000)).getBytes(StandardCharsets.UTF_8));
   ByteArrayOutputStream a=new ByteArrayOutputStream(),b=new ByteArrayOutputStream();
   assertEquals(TrackFile.gpx(f,a),TrackPrivacy.gpx(f,b,0));assertArrayEquals(a.toByteArray(),b.toByteArray());
  }finally{f.delete();}
 }
 @Test public void targetNormalization() {
  assertEquals(0,RideGoals.normalize(42));assertEquals(75,RideGoals.progressPercent(225,300));
  assertEquals(100,RideGoals.progressPercent(2000,100));
  assertEquals(0,RideGoals.progressPercent(Double.NaN,300));
 }
}
