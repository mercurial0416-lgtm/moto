package com.motohub.next;

/** Private on-device diagnostics. Nothing leaves the app without user action. */
final class RideDiagnostics {
 private RideDiagnostics(){}
 static String summarize(boolean locationPermission,boolean gpsEnabled,boolean notifications,
      boolean batteryUnrestricted,boolean obdArmed,String obdStatus,boolean backupReadable){
  return "위치 권한: "+result(locationPermission,"허용","설정에서 정확한 위치 허용")+
   "\n기기 GPS: "+result(gpsEnabled,"켜짐","위치 서비스 활성화 필요")+
   "\n알림 권한: "+result(notifications,"허용","라이딩 유지 알림 허용 필요")+
   "\n배터리 제한: "+(batteryUnrestricted?"제한 없음":"시스템 최적화 대상일 수 있음 · 장거리 라이딩 시 제한 해제 고려")+
   "\nOBD 모드: "+(obdArmed?"사용자가 활성화":"꺼짐")+" · "+(obdStatus==null?"미연결":obdStatus)+
   "\n앱 데이터: "+(backupReadable?"읽기 정상 · 외부 전체 백업도 권장":"데이터 확인 필요 · 삭제하지 말고 백업/복구 확인");
 }
 private static String result(boolean ok,String yes,String no){return ok?yes:no;}
}
