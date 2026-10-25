package com.motohub.next;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class CnRideReliabilityTest {
 private static int checks;
 private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);checks++;}
 public static void main(String[] args)throws Exception {
  check(ObdSpeedResponsePolicy.unsupported(ObdPidCapabilities.Support.NO),"explicit unsupported speed must stop");
  check(!ObdSpeedResponsePolicy.unsupported(ObdPidCapabilities.Support.UNKNOWN),"unknown cannot mean unsupported");
  check(!ObdSpeedResponsePolicy.unsupported(ObdPidCapabilities.Support.YES),"supported cannot mean unsupported");
  check(ObdSpeedResponsePolicy.retryConnection(6,ObdPidCapabilities.Support.YES),"six corrupted replies retry");
  check(ObdSpeedResponsePolicy.retryConnection(6,ObdPidCapabilities.Support.UNKNOWN),"unknown replies retry");
  check(!ObdSpeedResponsePolicy.retryConnection(5,ObdPidCapabilities.Support.YES),"transient noise threshold");
  check(!ObdSpeedResponsePolicy.retryConnection(6,ObdPidCapabilities.Support.NO),"unsupported must not retry");
  check(ObdSpeedResponsePolicy.pollDelayMs(0)==4000,"idle polling saves work");
  check(ObdSpeedResponsePolicy.pollDelayMs(7)==1100,"moving polling remains responsive");
  String odo="41 A6 00 01 4C 0B";
  check(ObdOdometer.parseKm(odo)==8500.3,"valid ODO");
  check(ObdOdometer.parseKm(odo+"\r"+odo)==8500.3,"identical ECU reply accepted");
  check(Double.isNaN(ObdOdometer.parseKm(odo+"\r41 A6 00 01 4C 0C")),"ambiguous ODO rejected");
  check(Double.isNaN(ObdOdometer.parseKm("41 A6 00 00 00 00\r"+odo)),"one zero ECU reply rejected");
  check(Double.isNaN(ObdOdometer.parseKm("NO DATA")),"no data rejected");
  File folder=Files.createTempDirectory("moto-cn-gps").toFile();
  File track=new File(folder,"1729250000000-11111111-1111-1111-1111-111111111111.track");
  byte[] first=TrackFile.point(37.1,127.1,5,1729250000000L).getBytes(StandardCharsets.UTF_8);
  byte[] partial="37.2,127.2,5,1729250001000".getBytes(StandardCharsets.UTF_8);
  byte[] original=new byte[first.length+partial.length];System.arraycopy(first,0,original,0,first.length);System.arraycopy(partial,0,original,first.length,partial.length);
  Files.write(track.toPath(),original);
  TrackRecovery.Result result=TrackRecovery.recover(track);
  check(result.points==1,"one complete GPS point recovered");
  check(result.discardedBytes==partial.length,"only incomplete GPS tail removed");
  check(new File(track.getPath()+".recovered").isFile(),"recovered marker persisted");
  check(Files.readAllBytes(track.toPath()).length==first.length,"GPS original complete point preserved");
  try{TrackRecovery.recover(track);throw new AssertionError("double recovery allowed");}
  catch(java.io.IOException expected){checks++;}
  for(File file:folder.listFiles())file.delete();folder.delete();
  System.out.println("PASS CN ride reliability: "+checks+" assertions");
 }
}
