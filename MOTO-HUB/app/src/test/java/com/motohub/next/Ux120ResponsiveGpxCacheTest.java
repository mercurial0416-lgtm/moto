package com.motohub.next;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;

public final class Ux120ResponsiveGpxCacheTest {
 private static final String ID="1700000000000-12345678-1234-4234-9234-123456789abc.track";
 @Test public void digestOfParsedGpsMatchesDirectFingerprint()throws Exception{
  File dir=Files.createTempDirectory("moto-v574-hash").toFile();
  File track=new File(dir,ID),done=new File(dir,ID+".done");
  try{
   Files.write(track.toPath(),(TrackFile.point(37,127,5,1700000000000L)+
    TrackFile.point(37.001,127,5,1700000010000L)).getBytes(StandardCharsets.UTF_8));
   Files.write(done.toPath(),new byte[0]);
   java.security.MessageDigest parsed=java.security.MessageDigest.getInstance("SHA-256");
   RideInsights.Result result=RideInsights.analyze(track,parsed);
   StringBuilder digest=new StringBuilder();
   for(byte b:parsed.digest())digest.append(String.format(Locale.ROOT,"%02x",b&255));
   assertEquals(RideMileageIntegrity.fingerprint(track),digest.toString());
   assertTrue(result.distanceKm>0);
   RideDistanceCache.clear();
   assertEquals(result.distanceKm,RideDistanceCache.read(track).distanceKm,.00001);
  }finally{Files.deleteIfExists(done.toPath());Files.deleteIfExists(track.toPath());Files.deleteIfExists(dir.toPath());}
 }
 @Test public void largerGpxUsesBoundedStreamAndKeepsRealRecordedDate()throws Exception{
  File dir=Files.createTempDirectory("moto-v574-gpx").toFile();
  try{
   String xml="<?xml version=\"1.0\"?><gpx><trk><trkseg>"+
    "<trkpt lat=\"37.000\" lon=\"127.000\"><time>2026-11-01T00:00:00Z</time></trkpt>"+
    "<trkpt lat=\"37.001\" lon=\"127.000\"><time>2026-11-01T00:00:10Z</time></trkpt>"+
    "</trkseg></trk></gpx>";
   GpxImporter.Imported result=GpxImporter.importTrack(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),dir);
   File track=new File(dir,result.name);
   assertEquals(2,result.count);
   assertEquals(TrackFile.MAX_BYTES,GpxImporter.MAX_INPUT_BYTES);
   assertEquals(200_000,GpxImporter.MAX_IMPORT_POINTS);
   assertTrue(new File(track.getPath()+".imported").isFile());
   assertTrue(new File(track.getPath()+".done").isFile());
   assertEquals(java.time.Instant.parse("2026-11-01T00:00:00Z").toEpochMilli(),RideHistoryLabel.startTime(track).longValue());
  }finally{
   File[] all=dir.listFiles();if(all!=null)for(File file:all)Files.deleteIfExists(file.toPath());
   Files.deleteIfExists(dir.toPath());
  }
 }
}
