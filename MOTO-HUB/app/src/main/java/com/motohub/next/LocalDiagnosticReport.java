package com.motohub.next;
/** Explicitly shared, allowlisted state report. Never include locations, usernames, URLs or tokens. */
final class LocalDiagnosticReport {
 private LocalDiagnosticReport(){}
 static String create(String version,boolean signerFixed,boolean locationPerm,boolean gpsEnabled,
  boolean notifications,boolean bluetooth,boolean running,boolean sharing,
  boolean serverConfigured,boolean loggedIn,int gpsPoints){
  // Version is build-controlled, but sanitize in case this helper is later reused.
  String safeVersion=(version!=null&&version.matches("[0-9]+\\.[0-9]+\\.[0-9]+-[a-zA-Z0-9._-]{1,70}"))?version:"unknown";
  if(safeVersion.length()>80)safeVersion=safeVersion.substring(0,80);
  return "MOTO HUB 로컬 진단 (사용자가 명시적으로 복사함)\n"
   +"버전: "+safeVersion+"\n"
   +"고정 서명: "+yes(signerFixed)+"\n"
   +"위치 권한: "+yes(locationPerm)+"\n"
   +"GPS 제공자: "+yes(gpsEnabled)+"\n"
   +"알림: "+yes(notifications)+"\n"
   +"Bluetooth: "+yes(bluetooth)+"\n"
   +"GPS 기록 실행: "+yes(running)+"\n"
   +"그룹 공유 상태: "+(sharing?"사용자 승인 후 켜짐":"꺼짐")+"\n"
   +"친구 서버 설정: "+yes(serverConfigured)+"\n"
   +"친구 서버 로그인 여부: "+yes(loggedIn)+"\n"
   +"이번 기록 GPS 좌표 수: "+Math.max(0,gpsPoints)+"\n"
   +"이 보고서에는 GPS 좌표·동선·계정·인증 토큰·파일 경로·오류 원문이 포함되지 않습니다.\n";
 }
 private static String yes(boolean value){return value?"예":"아니요";}
}
