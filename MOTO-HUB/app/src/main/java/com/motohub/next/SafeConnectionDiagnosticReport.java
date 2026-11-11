package com.motohub.next;
import android.Manifest;
import android.bluetooth.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import java.util.Locale;
/** Explicitly copied, local diagnostic report; never includes coordinates,
 * Bluetooth MAC, bike IDs, raw ECU responses or user-entered freeform errors. */
final class SafeConnectionDiagnosticReport {
 private SafeConnectionDiagnosticReport(){}
 static String redact(String text){
  if(text==null)return "";
  String s=text.replaceAll("(?i)\\b(?:[0-9a-f]{2}[:-]){5}[0-9a-f]{2}\\b","[Bluetooth 주소 숨김]");
  s=s.replaceAll("(?<![\\d.])[+-]?\\d{1,3}\\.\\d{4,}\\s*[,/]\\s*[+-]?\\d{1,3}\\.\\d{4,}(?![\\d.])",
    "[GPS 좌표 숨김]");
  return s.length()>5000?s.substring(0,5000)+"\n[보고서 길이 제한]":s;
 }
 static String create(Context c,String bike){
  if(!RideBikeBinding.valid(bike))return "MOTO HUB 연결 진단: 선택 바이크 없음";
  ConnectionSetupMode.Mode mode=ConnectionSetupMode.get(c,bike);
  boolean fine=c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;
  boolean bt=Build.VERSION.SDK_INT<31||
    c.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED;
  boolean notification=Build.VERSION.SDK_INT<33||
    c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;
  boolean background=Build.VERSION.SDK_INT<29||
    c.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)==PackageManager.PERMISSION_GRANTED;
  BluetoothManager manager=c.getSystemService(BluetoothManager.class);
  BluetoothAdapter adapter=manager==null?null:manager.getAdapter();
  boolean bluetoothOn=false;
  if(adapter!=null&&bt)try{bluetoothOn=adapter.isEnabled();}catch(SecurityException ignored){}
  LocationManager location=c.getSystemService(LocationManager.class);
  boolean gps=location!=null&&location.isProviderEnabled(LocationManager.GPS_PROVIDER);
  StringBuilder out=new StringBuilder("MOTO HUB 연결 진단 · v604\n");
  out.append("기록 방식: ").append(mode.label).append('\n');
  // Bluetooth display names can contain a rider's real name or phone number.
  // Never include user-generated device names even when there is no MAC.
  out.append("OBD 동글: ").append(BikeObdPreferences.validAddress(BikeObdPreferences.address(c,bike))?
    "등록됨 · 장치 이름 비공개":"미등록").append('\n');
  out.append("카르도: ").append(CardoAutoRidePrefs.forBike(c,bike)?
    "등록됨 · 장치 이름 비공개":"미등록").append('\n');
  out.append("카르도 최근 연결 이벤트: ").append(CardoAutoRidePrefs.recentlyConnected(c)?"있음 · 현재 연결과 별개":"없음").append('\n');
  out.append("GPS 안전망 동의: ").append(BikeObdPreferences.gpsFallbackEnabled(c,bike)?"켜짐":"꺼짐").append('\n');
  out.append("주변 기기 권한: ").append(bt?"허용":"미허용").append('\n');
  out.append("Bluetooth 전원: ").append(!bt?"권한 미허용으로 확인 불가":bluetoothOn?"켜짐":"꺼짐/확인 불가").append('\n');
  out.append("정확한 위치 권한: ").append(fine?"허용":"미허용").append('\n');
  out.append("백그라운드 위치 권한: ").append(background?"허용 또는 해당 없음":"미허용").append('\n');
  out.append("알림 권한: ").append(notification?"허용":"미허용").append('\n');
  out.append("GPS 시스템 위치: ").append(gps?"켜짐":"꺼짐/확인 불가").append('\n');
  boolean same=bike.equals(RideService.activeRideBikeId);
  out.append("기록 서비스: ").append(same&&RideService.running?"GPS 주행 중":same&&RideService.autoArmed?"자동 출발 감시 중":"실시간 기록 시작 전").append('\n');
  out.append("실제 ECU 속도: ").append(same&&RideService.autoArmed&&RideService.obdSpeedKmh>=0&&
   ObdTelemetryFreshness.speedFresh(RideService.obdLastSampleElapsed,android.os.SystemClock.elapsedRealtime())?
   "최근 응답 있음":"최근 응답 확인 안 됨").append('\n');
  out.append("최근 ECU 읽기 테스트:\n").append(ObdLocalDiagnosticLog.recentManualSummary(c)).append('\n');
  out.append("첫 주행 확인:\n").append(FirstRideSetupProof.summary(c,bike)).append('\n');
  out.append("최근 자동 연결:\n").append(ConnectionEventLog.summary(c,bike)).append('\n');
  out.append("자동기록 진행:\n").append(ObdAutoRideProgress.summary(c,bike)).append('\n');
  out.append("\n이 보고서에는 GPS 좌표, 기기 주소, 원본 ECU 응답이 포함되지 않아.\n");
  return redact(out.toString());
 }
}
