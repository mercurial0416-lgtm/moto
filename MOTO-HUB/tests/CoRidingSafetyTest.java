package com.motohub.next;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Regression tests for conflicting OBD ECU replies and safe boot recovery notice. */
public final class CoRidingSafetyTest {
 private static int checks;
 private static void yes(boolean ok, String reason){
  if(!ok)throw new AssertionError(reason);
  checks++;
 }
 private static void removeTree(File dir){
  if(dir.isDirectory()){File[] children=dir.listFiles();if(children!=null)for(File c:children)removeTree(c);}
  dir.delete();
 }
 public static void main(String[] args)throws Exception{
  yes(ObdReadOnlyProtocol.speedKmh("010D\r7E8 03 41 0D 28\r>")==40,"echo + valid speed");
  yes(ObdReadOnlyProtocol.speedKmh("7E8 03 41 0D 28\r7E9 03 41 0D 28") ==40,"same-speed duplicate ECU");
  yes(ObdReadOnlyProtocol.speedKmh("7E8 03 41 0D 28\r7E9 03 41 0D 00")==-1,"conflicting ECU speeds forbidden");
  yes(ObdReadOnlyProtocol.speedKmh("410D28 410D29")==-1,"mixed packed-speed replies forbidden");
  yes(ObdReadOnlyProtocol.speedKmh("410D")==-1,"truncated speed forbidden");
  yes(ObdReadOnlyProtocol.speedKmh("410DFF")==-1,"out-of-range speed forbidden");
  yes(Double.isNaN(ObdSensorReading.number("010C","410C1AF8\r410C1AF9")),"conflicting RPM forbidden");
  yes(ObdReadOnlyProtocol.responseBytes("0142","414230")==null,"truncated ECU voltage forbidden");
  yes(ObdSensorReading.number("010C","410C1AF8")==1726.0,"valid RPM preserved");
  yes("지원 안 함".equals(ObdReadOnlyProtocol.decode("0105","410578\r410579")),"conflicting temperature UI hidden");
  ObdPidCapabilities mixed=ObdPidCapabilities.fromResponses("410008180000\r410000000000",null);
  yes(mixed.forPid("010D")==ObdPidCapabilities.Support.UNKNOWN,"conflicting bitmap cannot claim unsupported");
  yes(ObdPidCapabilities.fromResponses("410008180000\r410008180000",null)
    .forPid("010D")==ObdPidCapabilities.Support.YES,"identical PID bitmaps accepted");
  yes(ObdPidCapabilities.fromResponses(null,null,"41A004000000\r41A000000000")
    .forPid("01A6")==ObdPidCapabilities.Support.UNKNOWN,"conflicting ODO support cannot enable probing");
  try{ObdReadOnlyProtocol.query("04");throw new AssertionError("ECU write permitted");}
  catch(IllegalArgumentException expected){checks++;}
  yes(!RideMotionGate.countDistance(5000,2.0,5,5),"2m GPS drift ignored");
  yes(!RideMotionGate.countDistance(5000,8.0,30,30),"weak-GPS jitter ignored");
  yes(RideMotionGate.countDistance(5000,15.0,5,5),"real movement still counted");
  yes(RideMotionGate.countDistance(10000,100.0,20,20),"normal moving bike with average GPS counted");
  RideNotificationRate throttle=new RideNotificationRate();
  yes(throttle.shouldNotify(1000L),"first auto notification allowed");
  yes(!throttle.shouldNotify(2000L),"routine auto notification not repeated every second");
  yes(throttle.shouldNotify(11000L),"ten-second refresh allowed");
  File dir=Files.createTempDirectory("moto-co-boot").toFile();
  try{
   File rides=TrackFile.directory(dir);
   File partial=new File(rides,"1792122693079-aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa.track");
   Files.write(partial.toPath(),"37.1,127.1,4,1792120000000".getBytes(StandardCharsets.UTF_8));
   yes(!BootRidePolicy.hasRecoverable(dir),"incomplete first row must not trigger boot alert");
   Files.write(partial.toPath(),"not,gps,row,data\n".getBytes(StandardCharsets.UTF_8));
   yes(!BootRidePolicy.hasRecoverable(dir),"bad first row must not trigger boot alert");
   String good=TrackFile.point(37.1,127.1,4,1792120000000L);
   Files.write(partial.toPath(),(good+"37.2,127.").getBytes(StandardCharsets.UTF_8));
   yes(BootRidePolicy.hasRecoverable(dir),"one committed GPS row + partial tail is recoverable");
   new File(partial.getPath()+".done").createNewFile();
   yes(!BootRidePolicy.hasRecoverable(dir),"already completed track must not trigger boot alert");
   new File(partial.getPath()+".done").delete();
   new File(partial.getPath()+".recovered").createNewFile();
   yes(!BootRidePolicy.hasRecoverable(dir),"recovered track must not repeat boot alert");
  }finally{removeTree(dir);}
  System.out.println("PASS CO OBD+boot ride safeguards: "+checks+" assertions");
 }
}
