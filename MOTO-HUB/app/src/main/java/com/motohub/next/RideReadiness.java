package com.motohub.next;

/** Device-side checks shown before starting a location foreground service. */
final class RideReadiness {
 private RideReadiness() {}
 enum State { RECORDING, LOCATION_PERMISSION, GPS_DISABLED, NOTIFICATION_PERMISSION, NOTIFICATIONS_DISABLED, READY }
 static State check(boolean recording, boolean fineLocation, boolean gpsEnabled,
                    boolean notificationPermission, boolean notificationsEnabled) {
  if(recording) return State.RECORDING;
  if(!fineLocation) return State.LOCATION_PERMISSION;
  if(!gpsEnabled) return State.GPS_DISABLED;
  if(!notificationPermission) return State.NOTIFICATION_PERMISSION;
  if(!notificationsEnabled) return State.NOTIFICATIONS_DISABLED;
  return State.READY;
 }
 static String title(State state) {
  return switch(state) {
   case RECORDING -> "GPS 기록 중";
   case LOCATION_PERMISSION -> "정확한 위치 권한 필요";
   case GPS_DISABLED -> "기기 GPS가 꺼져 있어요";
   case NOTIFICATION_PERMISSION -> "라이딩 알림 권한 필요";
   case NOTIFICATIONS_DISABLED -> "앱 알림이 차단되어 있어요";
   case READY -> "GPS 기록 준비 완료";
  };
 }
 static String guidance(State state) {
  return switch(state) {
   case RECORDING -> "라이딩 종료 버튼으로 언제든 기록을 중지할 수 있어요.";
   case LOCATION_PERMISSION -> "GPS 경로 기록에는 정확한 위치 허용이 필요해요. 시작을 누르면 동의 후 권한을 요청합니다.";
   case GPS_DISABLED -> "기기 위치 서비스를 켜야 GPS 경로를 기록할 수 있어요.";
   case NOTIFICATION_PERMISSION -> "화면을 꺼도 기록 상태를 알릴 수 있도록 알림 권한을 허용해 주세요.";
   case NOTIFICATIONS_DISABLED -> "시스템 설정에서 MOTO HUB 알림을 허용한 뒤 시작해 주세요.";
   case READY -> "기기에만 기록되며, 그룹 공유는 별도 동의가 필요해요.";
  };
 }
 static String action(State state) {
  return switch(state) {
   case GPS_DISABLED -> "위치 설정 열기";
   case NOTIFICATIONS_DISABLED -> "앱 알림 설정 열기";
   case LOCATION_PERMISSION, NOTIFICATION_PERMISSION -> "동의 후 권한 요청";
   case READY, RECORDING -> "";
  };
 }
}
