package com.motohub.next;

/** Human-readable, privacy-preserving diagnosis from *observed* milestones.
 * Never assert ECU support from paired status alone. */
final class ObdTroubleshootingPolicy {
 private ObdTroubleshootingPolicy(){}
 static String advice(boolean enabled,boolean serviceArmed,boolean gpsEnabled,
                      boolean bluetoothEnabled,boolean fineLocation,
                      boolean notifications,boolean speedEverObserved,
                      boolean gpsRideEverStarted){
  if(!enabled)return "자동 기록이 꺼져 있어. 먼저 '자동 기록 시작'을 눌러 줘.";
  if(!fineLocation)return "정확한 위치 권한이 없어 GPS 기록을 시작할 수 없어.";
  if(!notifications)return "알림 권한 또는 앱 알림이 꺼져 있어. 지속 알림을 허용해야 해.";
  if(!gpsEnabled)return "휴대폰 위치(GPS)가 꺼져 있어. 켜면 자동 감지를 재개해.";
  if(!bluetoothEnabled)return "Bluetooth가 꺼져 있어 OBD에 연결할 수 없어.";
  if(!serviceArmed)return "설정은 저장됐지만 감지 서비스가 실행 중이 아니야. 앱을 열어 자동 감지를 재개해.";
  if(!speedEverObserved)return "실제 ECU 속도 수신 기록이 없어. 시동 ON·OBD 케이블·010D 지원 여부부터 확인해.";
  if(!gpsRideEverStarted)return "ECU 센서는 수신했지만 GPS 기록 시작이 확인되지 않았어. 7km/h 이상 속도값 3회와 GPS 상태를 점검해.";
  return "자동 기록 시작 이력이 있어. 오늘 누락됐다면 최근 Bluetooth 재연결·GPS 신호·시작 시각을 비교해 봐.";
 }
}
