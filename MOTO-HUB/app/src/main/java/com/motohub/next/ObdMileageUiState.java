package com.motohub.next;

import android.content.Context;
import java.util.Locale;

/** Display-only priority policy. Does not modify original/verified garage ODO. */
final class ObdMileageUiState {
 enum Source { ECU_VERIFIED, PID_0131_ESTIMATE, GPS_ESTIMATE, UNAVAILABLE }
 private ObdMileageUiState(){}
 static Source choose(boolean actualEcu,boolean dtcEstimate,boolean gpsEstimate){
  if(actualEcu)return Source.ECU_VERIFIED;
  if(dtcEstimate)return Source.PID_0131_ESTIMATE;
  if(gpsEstimate)return Source.GPS_ESTIMATE;
  return Source.UNAVAILABLE;
 }
 static final long RECENT_COUNTER_MS=20L*60_000L;
 static boolean recentCounter(long receivedAt,long now){
  return receivedAt>0&&now>=receivedAt&&now-receivedAt<=RECENT_COUNTER_MS;
 }
 private static String km(double value){
  return String.format(Locale.KOREA,"%,.1f km",value);
 }
 static String primary(Context c,String bikeId){
  if(!RideBikeBinding.valid(bikeId))return "바이크 선택 필요";
  double ecu=OdometerAutoSyncInbox.pending(c,bikeId);
  DtcOdoBridge.Reading dtc=DtcOdoBridge.snapshot(c,bikeId);
  HybridOdometerEstimate.Reading gps=HybridOdometerEstimate.snapshot(c,bikeId);
  boolean dtcRecent=dtc.valid&&recentCounter(dtc.lastAt,System.currentTimeMillis());
  Source selected=choose(OdoAutoSyncPolicy.sane(ecu),dtcRecent,gps.valid);
  switch(selected){
   case ECU_VERIFIED:return "ECU 실제 ODO 검증값 "+km(ecu)+" · 차고 확인 대기";
   case PID_0131_ESTIMATE:return "PID 0131 추정 "+km(dtc.estimatedKm);
   case GPS_ESTIMATE:return "GPS 주행거리 추정 "+km(gps.estimatedKm)+(dtc.valid&&!dtcRecent?" · 0131 장시간 미수신":"");
   default:return dtc.valid?"PID 0131 마지막 확인 "+km(dtc.estimatedKm)+" · 20분 이상 미수신 (현재값 미확인)":"자동 연동값 없음 · 계기판 기준값 등록 필요";
  }
 }
 static String details(Context c,String bikeId,double garageKm){
  if(!RideBikeBinding.valid(bikeId))return "먼저 차고에서 바이크를 등록하고 선택해 주세요.";
  String manual=Double.isFinite(garageKm)&&garageKm>=0?
    "계기판 직접 확인 기록: "+km(garageKm):
    "계기판 직접 확인 기록 없음";
  String real=OdometerAutoSyncInbox.display(c,bikeId);
  DtcOdoBridge.Reading dtc=DtcOdoBridge.snapshot(c,bikeId);
  String dtcLine=dtc.enabled?DtcOdoBridge.label(c,bikeId):
    "PID 0131 보조: 기준값 미설정";
  HybridOdometerEstimate.Reading gps=HybridOdometerEstimate.snapshot(c,bikeId);
  String gpsLine=gps.valid?"GPS 추정: "+km(gps.estimatedKm):
    gps.configured?"GPS 추정: 보정 필요":"GPS 추정: 계기판 기준값 미설정";
  return manual+"\n"+real+"\n"+dtcLine+"\n"+gpsLine+
   "\n※ 실제 ECU ODO와 추정치는 별도 관리 · 자동 원본 덮어쓰기 없음";
 }
}
