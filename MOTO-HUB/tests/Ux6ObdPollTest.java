package com.motohub.next;

/** No invalid ECU reply may force unnecessarily aggressive polling. */
public final class Ux6ObdPollTest {
 static int checks;
 static void check(boolean pass,String why){checks++;if(!pass)throw new AssertionError(why);}
 public static void main(String[] args){
  check(ObdSpeedResponsePolicy.pollDelayMs(0)==4000L,"stationary low-power poll");
  check(ObdSpeedResponsePolicy.pollDelayMs(-1)==4000L,"missing speed conservative poll");
  check(ObdSpeedResponsePolicy.pollDelayMs(256)==4000L,"out-of-protocol speed conservative poll");
  check(ObdSpeedResponsePolicy.pollDelayMs(50)==1100L,"moving responsive poll");
  check(ObdSpeedResponsePolicy.pollDelayMs(255)==1100L,"valid upper end");
  check(ObdSpeedResponsePolicy.retryConnection(6,ObdPidCapabilities.Support.UNKNOWN),"temporary read failures retry");
  check(!ObdSpeedResponsePolicy.retryConnection(6,ObdPidCapabilities.Support.NO),"genuinely unsupported PID not retried");
  System.out.println("PASS UX6 OBD polling: "+checks+" checks");
 }
}
