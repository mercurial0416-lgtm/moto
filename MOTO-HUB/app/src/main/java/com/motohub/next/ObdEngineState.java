package com.motohub.next;
/** RPM-based engine state is an estimate, not a manufacturer's ignition flag. */
final class ObdEngineState {
 static String label(double rpm,long sampledElapsed,long nowElapsed){
  if(!Double.isFinite(rpm)||!ObdTelemetryFreshness.fresh(sampledElapsed,nowElapsed))return "시동 상태 확인 불가";
  if(rpm>=400&&rpm<=15000)return "엔진 작동 추정 (RPM)";
  if(rpm>=0&&rpm<400)return "엔진 정지 추정 (RPM)";
  return "시동 상태 확인 불가";
 }
 private ObdEngineState(){}
}
