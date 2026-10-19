package com.motohub.next;
import java.io.File;
import java.nio.file.Files;

public final class CmOdoAndRecoveryTest {
 private static int passed;
 private static void check(boolean yes,String message){if(!yes)throw new AssertionError(message);passed++;}
 public static void main(String[] args){
  check("01A0\r".equals(ObdReadOnlyProtocol.query("01A0")),"bitmap is whitelisted");
  check("01A6\r".equals(ObdReadOnlyProtocol.query("01A6")),"read-only odo is whitelisted");
  try{ObdReadOnlyProtocol.query("04");throw new AssertionError("ECU clear codes must be denied");}
  catch(IllegalArgumentException expected){passed++;}
  ObdPidCapabilities yes=ObdPidCapabilities.fromResponses(null,null,"41 A0 04 00 00 00");
  check(yes.forPid("01A6")==ObdPidCapabilities.Support.YES,"ODO bit 26 supported");
  check(yes.forPid("01A5")==ObdPidCapabilities.Support.NO,"other A0 PID absent");
  check(ObdPidCapabilities.fromResponses(null,null,"41 A0 00 00 00 00").forPid("01A6")==ObdPidCapabilities.Support.NO,"ODO unsupported");
  check(ObdPidCapabilities.unknown().forPid("01A6")==ObdPidCapabilities.Support.UNKNOWN,"no bitmap must be unknown");
  check(ObdPidCapabilities.fromResponses(null,null,"41 A0 04 00").forPid("01A6")==ObdPidCapabilities.Support.UNKNOWN,"incomplete bitmap unknown");
  // 8500.3 km = 0x00014C0B decimeters
  check(ObdOdometer.parseKm("41 A6 00 01 4C 0B") == 8500.3,"decoding 0.1 km");
  check(ObdOdometer.parseKm("7E8 06 41A600014C0B\r>") == 8500.3,"compact ELM reply");
  check(Double.isNaN(ObdOdometer.parseKm("NO DATA")),"no data");
  check(Double.isNaN(ObdOdometer.parseKm("41 A6 00 01 4C")),"partial ODO must fail");
  check(Double.isNaN(ObdOdometer.parseKm("41 A6 FF FF FF FF")),"implausible ODO must fail");
  check(Double.isNaN(ObdOdometer.parseKm("41 A6 00 00 00 00")),"zero ODO must not modify records");
  check(Double.isNaN(ObdOdometer.parseKm("41 A6 00 01 4C 0BZ")),"corrupt reply must fail");
  check("엔진 작동 추정 (RPM)".equals(ObdEngineState.label(1600,1000,2000)),"running rpm");
  check("엔진 정지 추정 (RPM)".equals(ObdEngineState.label(0,1000,2000)),"stationary engine");
  check("시동 상태 확인 불가".equals(ObdEngineState.label(Double.NaN,1000,2000)),"unknown rpm");
  check("시동 상태 확인 불가".equals(ObdEngineState.label(1600,1000,60_000)),"stale rpm unknown");
  check("시동 상태 확인 불가".equals(ObdEngineState.label(1600,0,2000)),"missing rpm unknown");
  check(BootRidePolicy.shouldOffer(true,true,true),"interrupted track on boot");
  check(!BootRidePolicy.shouldOffer(false,true,true),"not a boot");
  check(!BootRidePolicy.shouldOffer(true,false,true),"no notification permission");
  check(!BootRidePolicy.shouldOffer(true,true,false),"no interrupted rides");
  try {
   File data=Files.createTempDirectory("moto-boot-policy").toFile();
   File rides=new File(data,"rides");rides.mkdirs();
   check(!BootRidePolicy.hasRecoverable(data),"empty ride folder");
   File ride=new File(rides,"1729250000000-11111111-1111-1111-1111-111111111111.track");
   Files.write(ride.toPath(),"37.1,127.1,4.0,1729250000000\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
   check(BootRidePolicy.hasRecoverable(data),"interrupted track exists");
   File done=new File(ride.getPath()+".done");done.createNewFile();
   check(!BootRidePolicy.hasRecoverable(data),"finished track ignored");
   done.delete();Files.write(ride.toPath(),new byte[0]);
   check(!BootRidePolicy.hasRecoverable(data),"empty track ignored");
   ride.delete();rides.delete();data.delete();
  }catch(Exception e){throw new RuntimeException(e);}
  System.out.println("PASS: "+passed+" CM ODO / engine / boot policy assertions");
 }
}
