package com.motohub.next;
import java.util.Locale;
/** Deterministic, conservative reasons and directly executable next actions.
 * No stale Bluetooth pairing is ever presented as confirmed ECU communication. */
final class ConnectionIssueGuide {
 enum Issue {
  MODE("자동기록 방식 선택 필요","기록 방식을 아직 선택하지 않았어.","원하는 방식부터 선택해.","방식 선택"),
  OBD_DEVICE("OBD 동글 등록 필요","선택한 방식은 실제 OBD 동글이 필요해.","키 ON → 갤럭시 Bluetooth 페어링 → 동글 등록.","동글 등록"),
  CARDO_DEVICE("카르도 등록 필요","선택한 방식은 Freecom 2X 연결 감지가 필요해.","Freecom 2X 페어링 후 앱에서 카르도를 등록해.","카르도 등록"),
  GPS_CONSENT("GPS 안전망 동의 필요","GPS로 자동 출발을 감지하도록 아직 동의하지 않았어.","배터리/오인 가능성을 확인한 뒤 안전망을 켜.","안전망 설정"),
  BLUETOOTH_OFF("휴대폰 Bluetooth 꺼짐","Bluetooth가 꺼져 있어 장치 상태를 읽을 수 없어.","휴대폰 Bluetooth를 켠 뒤 다시 연결해.","Bluetooth 설정"),
  BT_PERMISSION("주변 기기 권한 필요","앱에 Bluetooth 연결 확인 권한이 없어.","앱 권한에서 주변 기기를 허용해.","앱 권한 열기"),
  BACKGROUND_LOCATION("백그라운드 위치 권한 필요","카르도가 연결돼도 화면이 꺼진 상태에서 Android가 자동기록 서비스를 시작하지 못할 수 있어.","앱 정보 → 권한 → 위치에서 항상 허용을 선택해. 허용할 수 없다면 카르도 시작 알림을 눌러.","위치 권한 열기"),
  LOCATION_PERMISSION("정확한 위치 권한 필요","GPS 경로 저장을 위한 정확한 위치 권한이 없어.","앱 위치 권한을 허용해.","앱 권한 열기"),
  NOTIFICATION_PERMISSION("알림 권한 필요","자동 감지 알림을 보여줄 권한이 없어.","알림 권한을 허용해.","앱 권한 열기"),
  GPS_OFF("GPS 위치 기능 꺼짐","휴대폰 위치 기능이 꺼져 있어.","위치 기능을 켠 다음 다시 확인해.","위치 설정"),
  GPS_NO_FIX("GPS 좌표 수신 대기","기록 서비스는 작동하지만 최신 실제 GPS 좌표가 없어.","실외에서 위치를 확인하고 정확한 위치 권한을 점검해.","위치 설정"),
  ECU_SPEED_UNSUPPORTED("ECU 속도 PID 지원 확인 필요","이전 진단에 속도 PID 미지원 신호가 있어.","정차 상태에서 재검사해. 미지원이면 GPS 안전망을 별도 선택할 수 있어.","OBD 재검사"),
  ECU_NO_REPLY("ECU 통신 오류","OBD 연결/ECU 응답 오류가 기록됐어.","키 ON, 변환 케이블, Bluetooth Classic SPP를 점검하고 정차 후 검사해.","OBD 재검사"),
  OBD_UNVERIFIED("OBD 실제 수신 미확인","Bluetooth 등록 또는 대기만으로 ECU 속도 수신이 증명되지 않아.","정차 상태에서 RPM/속도 표준 PID를 실제로 읽어 봐.","OBD 검사"),
  CARDO_NOT_SEEN("카르도 연결 감지 이력 없음","최근 Freecom 2X 연결 이벤트가 아직 확인되지 않았어.","전원을 켜고 갤럭시 Bluetooth에서 연결 후 이 화면을 다시 확인해.","Bluetooth 설정"),
  BACKGROUND_LIMIT("백그라운드 자동시작 제한 의심","Android가 카르도 연결 후 자동 GPS 시작을 제한했을 수 있어.","알림을 눌러 기록을 시작하거나 앱 위치 권한을 확인해.","앱 권한 확인"),
  READY("즉시 확인된 오류 없음","설정이나 저장된 오류만으로 추가 원인을 특정하지 못했어.","현재 ECU/GPS 상태를 확인하고 실제 첫 주행 저장 결과를 점검해.","현재 상태 보기");
  final String title,reason,next,button;
  Issue(String title,String reason,String next,String button){
   this.title=title;this.reason=reason;this.next=next;this.button=button;
  }
 }
 private ConnectionIssueGuide(){}
 static Issue diagnose(ConnectionSetupMode.Mode mode,boolean hasObd,boolean hasCardo,
   boolean gpsFallback,boolean bluetooth,boolean btPermission,boolean locationPermission,
   boolean notifications,boolean gpsOn,boolean active,boolean running,
   boolean freshGps,boolean freshEcu,String latestError,boolean recentCardo){
  return diagnose(mode,hasObd,hasCardo,gpsFallback,bluetooth,btPermission,
   locationPermission,notifications,gpsOn,active,running,freshGps,freshEcu,
   latestError,recentCardo,false,true);
 }
 static Issue diagnose(ConnectionSetupMode.Mode mode,boolean hasObd,boolean hasCardo,
   boolean gpsFallback,boolean bluetooth,boolean btPermission,boolean locationPermission,
   boolean notifications,boolean gpsOn,boolean active,boolean running,
   boolean freshGps,boolean freshEcu,String latestError,boolean recentCardo,
   boolean backgroundRequired,boolean backgroundGranted){
  if(mode==null||mode==ConnectionSetupMode.Mode.NONE)return Issue.MODE;
  if(!btPermission)return Issue.BT_PERMISSION;
  if(!bluetooth)return Issue.BLUETOOTH_OFF;
  if(mode.needsObd()&&!hasObd)return Issue.OBD_DEVICE;
  if(mode.needsCardo()&&!hasCardo)return Issue.CARDO_DEVICE;
  if(mode==ConnectionSetupMode.Mode.CARDO_GPS&&!gpsFallback)return Issue.GPS_CONSENT;
  if(!btPermission)return Issue.BT_PERMISSION;
  if(!locationPermission)return Issue.LOCATION_PERMISSION;
  if(!notifications)return Issue.NOTIFICATION_PERMISSION;
  if(!gpsOn)return Issue.GPS_OFF;
  if(backgroundRequired&&!backgroundGranted)return Issue.BACKGROUND_LOCATION;
  if(running&&!freshGps)return Issue.GPS_NO_FIX;
  String error=latestError==null?"":latestError.toLowerCase(Locale.ROOT);
  if(active&&(error.contains("010d")||error.contains("속도 pid"))&&
     error.contains("미지원"))return Issue.ECU_SPEED_UNSUPPORTED;
  if(active&&!freshEcu&&(error.contains("obd")||error.contains("ecu"))&&
     (error.contains("실패")||error.contains("응답")||error.contains("종료")||
      error.contains("timeout")||error.contains("no data")))return Issue.ECU_NO_REPLY;
  if(active&&mode.needsObd()&&!freshEcu)return Issue.OBD_UNVERIFIED;
  if(mode.needsCardo()&&!recentCardo&&!active)return Issue.CARDO_NOT_SEEN;
  if(error.contains("백그라운드")||error.contains("background"))return Issue.BACKGROUND_LIMIT;
  return Issue.READY;
 }
}
