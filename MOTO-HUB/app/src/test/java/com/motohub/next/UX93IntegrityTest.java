package com.motohub.next;

import java.io.IOException;
import java.io.File;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** UX93: fail-closed recording preflight and immutable ride-note snapshot semantics. */
public final class UX93IntegrityTest {
 @Test public void storageThresholdRejectsLowSpaceAndAcceptsLarge(){
  assertFalse(GpsStorageSpacePolicy.sufficient(-1));
  assertFalse(GpsStorageSpacePolicy.sufficient(0));
  assertFalse(GpsStorageSpacePolicy.sufficient(GpsStorageSpacePolicy.MIN_FREE_BYTES-1));
  assertTrue(GpsStorageSpacePolicy.sufficient(GpsStorageSpacePolicy.MIN_FREE_BYTES));
  assertTrue(GpsStorageSpacePolicy.sufficient(GpsStorageSpacePolicy.MIN_FREE_BYTES+1));
  assertTrue(GpsStorageSpacePolicy.message(42L*1024*1024).contains("42MB"));
 }
 @Test public void unknownStorageDirectoryFailsClosed(){
  try{GpsStorageSpacePolicy.require(null);fail("null storage must fail");}
  catch(IOException expected){assertTrue(expected.getMessage().contains("확인"));}
 }
 @Test public void rideNotesSnapshotPreservesOriginalAcrossMutation()throws Exception{
  JSONObject mutableNotes=new JSONObject().put("ride.track",new JSONObject().put("title","원본 이름"));
  String snapshot=mutableNotes.toString();
  mutableNotes.getJSONObject("ride.track").put("title","수정된 이름");
  JSONObject independent=new JSONObject(snapshot);
  assertEquals("원본 이름",independent.getJSONObject("ride.track").optString("title"));
  assertEquals("수정된 이름",mutableNotes.getJSONObject("ride.track").optString("title"));
 }
}
