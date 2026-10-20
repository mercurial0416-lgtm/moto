package com.motohub.next;
import java.util.ArrayList;
import java.util.List;
/** Explains device checks without starting recording or requesting any background permission. */
final class RideReadinessReport {
 private RideReadinessReport(){}
 static List<String> summary(boolean fine,boolean gps,boolean notification,boolean batteryExempt,boolean bluetooth){
  List<String> result=new ArrayList<>();
  result.add(fine?"정확한 위치 권한: 허용":"정확한 위치 권한: 필요 · 설정에서 앱 권한 허용");
  result.add(gps?"GPS 제공자: 켜짐":"GPS 제공자: 꺼짐 · 위치 서비스를 켜 주세요");
  result.add(notification?"지속 알림: 허용":"지속 알림: 차단 · 기록 서비스가 중단될 수 있습니다");
  result.add(batteryExempt?"배터리 최적화: 제외됨":"배터리 최적화: 제한 가능 · 화면이 꺼진 뒤 실제 주행 테스트 필요");
  result.add(bluetooth?"Bluetooth: 연결 가능한 상태":"Bluetooth: 꺼짐/미지원 · OBD2 연동에만 필요");
  result.add("앱 강제 종료·재부팅 후 기록 자동 재시작은 지원하지 않습니다. 미완료 GPS 원본은 복구 화면에 남습니다.");
  result.add("그룹 위치 공유는 자동 활성화되지 않습니다. 라이딩 중 별도 동의가 필요합니다.");
  return result;
 }
}
