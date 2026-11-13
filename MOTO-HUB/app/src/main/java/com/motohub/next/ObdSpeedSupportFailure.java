package com.motohub.next;

/** A declared missing OBD vehicle-speed PID is not a transient Bluetooth problem. */
final class ObdSpeedSupportFailure {
 private ObdSpeedSupportFailure(){}
 static boolean confirmedUnsupported(String reason){
  return "차량 속도 PID 010D 미지원".equals(reason);
 }
}
