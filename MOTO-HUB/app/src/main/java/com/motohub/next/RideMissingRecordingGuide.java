package com.motohub.next;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
/** Conservative local diagnosis; does not infer a missing ride from a missing
 * Bluetooth event. Never reads GPS coordinates or mutates a ride/ODO. */
final class RideMissingRecordingGuide {
 private RideMissingRecordingGuide(){}
 static String probableReason(ConnectionSetupMode.Mode mode,boolean obd,boolean cardo,
   boolean gpsConsent,boolean obdAlways,boolean active){
  if(active)return "기록 서비스가 현재 실행 중이야. 실제 GPS 좌표가 기록되는지 확인해.";
  if(mode==ConnectionSetupMode.Mode.NONE)return "자동기록 방식이 선택되지 않았거나 자동 시작 동의가 없어.";
  if(mode.needsObd()&&!obd)return "OBD 동글이 이 바이크에 등록되지 않았어.";
  if(mode.needsCardo()&&!cardo)return "카르도 자동감지 기기가 등록되지 않았어.";
  if(mode==ConnectionSetupMode.Mode.CARDO_GPS&&!gpsConsent)return "GPS 자동 출발 감지 동의가 꺼져 있어.";
  if(mode==ConnectionSetupMode.Mode.OBD_ONLY&&!obdAlways)
   return "OBD 상시 대기 동의가 꺼져 있어. 동글만 등록해서는 자동 시작하지 않아.";
  if(mode==ConnectionSetupMode.Mode.CARDO_OBD&&!gpsConsent)
   return "이 방식은 실제 ECU 속도 PID 010D 수신이 없으면 자동 GPS 기록을 시작하지 않아. GPS 안전망도 꺼져 있어.";
  return "설정은 있지만 카르도 연결 이벤트, ECU 속도, GPS 기록 시작 중 어느 단계가 막혔는지는 기록으로 확인해야 해.";
 }
 static String summary(Context c,String bike){
  if(!RideBikeBinding.valid(bike))return "바이크가 선택되지 않았어.";
  ConnectionSetupMode.Mode mode=ConnectionSetupMode.get(c,bike);
  boolean obd=BikeObdPreferences.validAddress(BikeObdPreferences.address(c,bike));
  boolean cardo=CardoAutoRidePrefs.forBike(c,bike);
  boolean fallback=BikeObdPreferences.gpsFallbackEnabled(c,bike);
  boolean auto=BikeObdPreferences.autoRideEnabled(c,bike);
  boolean active=bike.equals(RideService.activeRideBikeId)&&(RideService.running||RideService.autoArmed);
  boolean bt=Build.VERSION.SDK_INT<31||
   c.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED;
  boolean location=c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;
  boolean background=Build.VERSION.SDK_INT<29||
   c.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)==PackageManager.PERMISSION_GRANTED;
  StringBuilder result=new StringBuilder(
   "기록이 없었다고 해서 사고 없이 자동기록이 실행됐다는 뜻은 아니야. 원인을 확정하려면 같은 앱에 남아 있는 연결 이력을 봐야 해.\n\n");
  result.append("가능한 원인: ").append(probableReason(mode,obd,cardo,fallback,auto,active)).append("\n\n");
  result.append("이 바이크 설정: ").append(mode.label)
   .append("\nOBD 등록: ").append(obd?"있음 · 실제 ECU 속도 수신과 별개":"없음")
   .append("\n카르도 등록: ").append(cardo?"있음 · 실제 Bluetooth 연결과 별개":"없음")
   .append("\nGPS 안전망: ").append(fallback?"동의됨":"동의 안 됨")
   .append("\n주변 기기/정확한 위치: ").append(bt&&location?"허용":"일부 미허용")
   .append("\n백그라운드 위치: ").append(background?"허용/불필요":"미허용 · 자동 시작 제한 가능")
   .append("\n실제 기록 중: ").append(RideService.running?"예":"아니오")
   .append("\n자동 감시 서비스: ").append(RideService.autoArmed?"실행 중":"현재 미실행")
   .append("\n카르도 자동 시작: ").append(CardoStartHandshake.summary(c,bike))
   .append("\n\n마지막 GPS 기록 결과: ").append(RideCaptureHealth.summary(c,false))
   .append("\n\n자동기록 단계:\n").append(ObdAutoRideProgress.summary(c,bike))
   .append("\n\n연결/실패 시각:\n").append(ConnectionEventLog.summary(c,bike))
   .append("\n\n다음 주행 전: 정차 중 연결을 확인하고, 기록이 실제로 시작되지 않으면 라이딩 탭에서 GPS 기록을 직접 시작해.")
   .append("\n※ 별도 APK 버전 간 설정과 기록은 자동 공유되지 않아.");
  return result.toString();
 }
}
