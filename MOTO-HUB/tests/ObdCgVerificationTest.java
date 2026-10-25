package com.motohub.next;

final class ObdCgVerificationTest {
 private static int count;
 private static void yes(boolean expression,String message){if(!expression)throw new AssertionError(message);count++;}
 private static void blocked(String input,double previous){try{OdometerSyncPolicy.checkedManual(input,previous);throw new AssertionError("Unsafe ODO accepted: "+input);}catch(IllegalArgumentException expected){count++;}}
 public static void main(String[] args){
  yes("0100\r".equals(ObdReadOnlyProtocol.query("0100")),"supported-PIDs 0100 must be read-only");
  yes("0140\r".equals(ObdReadOnlyProtocol.query("0140")),"supported-PIDs 0140 must be read-only");
  try{ObdReadOnlyProtocol.query("0121");throw new AssertionError("unsafe PID accepted");}catch(IllegalArgumentException expected){count++;}
  ObdPidCapabilities cap=ObdPidCapabilities.fromResponses("SEARCHING...\r7E8 06 41 00 08 18 00 00\r>","41 40 40 00 00 00");
  yes(cap.forPid("0105")==ObdPidCapabilities.Support.YES,"coolant support");
  yes(cap.forPid("010C")==ObdPidCapabilities.Support.YES,"rpm support");
  yes(cap.forPid("010D")==ObdPidCapabilities.Support.YES,"speed support");
  yes(cap.forPid("0142")==ObdPidCapabilities.Support.YES,"voltage support");
  yes(cap.forPid("0111")==ObdPidCapabilities.Support.NO,"not supported PID");
  yes(cap.forPid("0145")==ObdPidCapabilities.Support.NO,"not supported 0140 PID");
  yes(cap.forPid("0199")==ObdPidCapabilities.Support.UNKNOWN,"out-of-range PID unknown");
  yes(cap.summary().contains("ODO) 미검증"),"never claim ODO support");
  ObdPidCapabilities unsupported=ObdPidCapabilities.fromResponses("410000000000",null);
  yes(unsupported.forPid("010D")==ObdPidCapabilities.Support.NO,"explicit no-speed");
  yes(unsupported.forPid("0142")==ObdPidCapabilities.Support.UNKNOWN,"missing 0140 unknown");
  ObdPidCapabilities unknown=ObdPidCapabilities.fromResponses("NO DATA","NO DATA");
  yes(unknown.forPid("010D")==ObdPidCapabilities.Support.UNKNOWN,"unavailable bitmap remains unknown");
  yes(ObdPidCapabilities.fromResponses("41 00 08 18",null).forPid("010D")==ObdPidCapabilities.Support.UNKNOWN,"truncated bitmap ignored");
  yes(ObdPidCapabilities.fromResponses(null,"41 40 40 00 00 00").forPid("010D")==ObdPidCapabilities.Support.UNKNOWN,"missing initial bitmap");
  yes(ObdPidCapabilities.fromResponses("41 40 40 00 00 00",null).forPid("010D")==ObdPidCapabilities.Support.UNKNOWN,"wrong header");
  yes(OdometerSyncPolicy.checkedManual("8500",8200)==8500,"manual odo increase");
  yes(OdometerSyncPolicy.checkedManual("8500.5",8500)==8500.5,"one decimal place");
  yes(OdometerSyncPolicy.checkedManual("100",100)==100,"unchanged allowed");
  blocked("8000",8500);blocked("-1",100);blocked("NaN",100);blocked("Infinity",100);
  blocked("100.23",0);blocked("3e4",0);blocked("2000001",0);blocked("",0);blocked(null,0);
  blocked("100",Double.NaN);blocked("100",-1);
  System.out.println("CG capability + manual ODO tests passed: "+count);
 }
}
