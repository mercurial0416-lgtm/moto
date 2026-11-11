package com.motohub.next;
/** Pure JVM guard for malformed ECU speed recovery, no Bluetooth required. */
public final class ObdSpeedResponseRecoveryTest {
 private static int n;
 private static void check(boolean yes,String message){
  n++;if(!yes)throw new AssertionError(message);
 }
 public static void main(String[] args){
  check(ObdSpeedResponsePolicy.pollDelayMs(-1)==1100,"no-data must retry promptly");
  check(ObdSpeedResponsePolicy.pollDelayMs(201)==1100,"invalid high speed must retry promptly");
  check(ObdSpeedResponsePolicy.pollDelayMs(0)==4000,"confirmed stop saves power");
  check(ObdSpeedResponsePolicy.pollDelayMs(1)==1100,"moving speed stays responsive");
  check(!ObdSpeedResponsePolicy.retryConnection(5,ObdPidCapabilities.Support.UNKNOWN),"five bad samples not enough");
  check(ObdSpeedResponsePolicy.retryConnection(6,ObdPidCapabilities.Support.UNKNOWN),"six bad samples reconnect");
  check(!ObdSpeedResponsePolicy.retryConnection(6,ObdPidCapabilities.Support.NO),"explicit unsupported is not a noisy adapter");
  System.out.println("PASS ObdSpeedResponseRecoveryTest "+n);
 }
}