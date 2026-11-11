package com.motohub.next;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
public class V605RideSessionVerificationTest {
 @Rule public TemporaryFolder tmp=new TemporaryFolder();
 private File part(String id,long stamp,int points)throws Exception{
  File folder=TrackFile.directory(tmp.getRoot());
  File f=new File(folder,stamp+"-"+id+".track");
  try(FileOutputStream o=new FileOutputStream(f)){
   for(int i=0;i<points;i++)
    o.write(TrackFile.point(37.0+i*.001,127.0,5,stamp+i*2000).getBytes(StandardCharsets.UTF_8));
  }
  TrackCompletion.mark(f,points);return f;
 }
 @Test public void multiPartValidatesAsWhole()throws Exception{
  String id="12345678-1234-1234-1234-123456789abc";
  part(id,1783000000000L,2);
  File last=part(id,1783001000000L,2);
  RideSessionVerification.Result result=RideSessionVerification.verify(tmp.getRoot(),last,1,4,.3);
  assertTrue(result.reason,result.ok);
  assertEquals(2,result.parts);
  assertEquals(4,result.points);
 }
 @Test public void missingEarlierMarkerIsNotCalledSuccessful()throws Exception{
  String id="12345678-1234-1234-1234-123456789abc";
  File earlier=part(id,1783000000000L,2);
  File last=part(id,1783001000000L,2);
  assertTrue(new File(earlier.getPath()+".done").delete());
  RideSessionVerification.Result result=RideSessionVerification.verify(tmp.getRoot(),last,1,4,.3);
  assertFalse(result.ok);
  assertTrue(result.reason.contains("마커"));
 }
 @Test public void anUnlistedSplitFailsConservatively()throws Exception{
  String id="12345678-1234-1234-1234-123456789abc";
  File last=part(id,1783001000000L,2);
  assertFalse(RideSessionVerification.verify(tmp.getRoot(),last,1,2,.3).ok);
 }
}
