package com.motohub.next;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
/** Safe owner-operated real-device verification guide. These are read-only
 * evidence labels, NOT a substitute for physical Duke 390/Freecom 2X testing. */
final class RideHardwareVerification {
 private RideHardwareVerification(){}
 static String checklist(Context c,String bike){
  if(!RideBikeBinding.valid(bike))return "먼저 바이크를 선택해.";
  ConnectionSetupMode.Mode mode=ConnectionSetupMode.get(c,bike);
  boolean bt=Build.VERSION.SDK_INT<31||
   c.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED;
  boolean loc=c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;
  boolean background=Build.VERSION.SDK_INT<29||
   c.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)==PackageManager.PERMISSION_GRANTED;
  StringBuilder s=new StringBuilder(
   "정차 상태에서만 기기 설정을 확인해. 주행 중 휴대폰을 조작하지 마.\n\n"+
   "1. 바이크 키 ON · OBD 동글 연결 · ECU 읽기 전용 검사\n"+
   "  OBD 등록: "+(BikeObdPreferences.validAddress(BikeObdPreferences.address(c,bike))?"등록됨 · ECU 응답과 별개":"미등록")+"\n"+
   "  마지막 실측: "+ObdLocalDiagnosticLog.recentManualSummary(c)+"\n\n"+
   "2. Freecom 2X 전원 ON · 갤럭시 Bluetooth 오디오 연결 확인\n"+
   "  카르도 등록: "+(CardoAutoRidePrefs.forBike(c,bike)?"등록됨 · 실시간 연결과 별개":"미등록")+"\n"+
   "  주변 기기 권한: "+(bt?"허용":"미허용")+"\n\n"+
   "3. 자동 출발 감시 준비 확인 (GPS 기록을 시작하기 전)\n"+
   "  상태: "+CardoStartHandshake.summary(c,bike)+"\n"+
   "  정확한 위치: "+(loc?"허용":"미허용")+
   " · 백그라운드 위치: "+(background?"허용/불필요":"미허용")+"\n\n"+
   "4. 안전한 실제 라이딩 뒤 정차 후 기록 검증\n"+
   "  "+FirstRideSetupProof.summary(c,bike)+"\n\n"+
   "5. 화면 OFF/절전·블루투스 끊김/재연결·배터리 소모 확인\n"+
   "  자동 테스트로 통과 처리하지 않음 · 실제 기기에서 확인 필요\n"+
   "  실패 시 '진단 결과 복사'로 로그를 공유해.\n\n"+
   "⚠ 소프트웨어 검증만으로 실제 모터사이클 ECU 호환성을 증명할 수 없어.");
  if(mode==ConnectionSetupMode.Mode.CARDO_GPS)
   s.append("\n현재 방식: 카르도 + GPS · OBD 센서가 없어도 출발 판정 가능하지만 오인 가능");
  return s.toString();
 }
}
