package com.motohub.next;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Locale;

/** Safe first-time OBD guidance. Never infers ECU compatibility from Bluetooth alone. */
final class ObdFirstConnectionGuide {
 private ObdFirstConnectionGuide(){}

 /** Only affects sorting and visual labels, never excludes a paired device. */
 static int adapterPriority(String name){
  if(name==null)return 2;
  String candidate=name.trim().toLowerCase(Locale.ROOT);
  if(candidate.contains("vgate")||candidate.contains("icar")||
     candidate.contains("elm327")||candidate.contains("obdii")||
     candidate.contains("obd2")||candidate.contains("obd-"))return 0;
  if(candidate.contains("scanner")||candidate.contains("diagnostic"))return 1;
  return 2;
 }
 static String adapterLabel(String name){
  return adapterPriority(name)==0?"OBD 후보 · ":adapterPriority(name)==1?"진단기 후보 · ":"기타 페어링 · ";
 }
 /** A parked bike can prove zero km/h, but moving auto-start remains unverified. */
 static String result(ObdPidCapabilities.Support speed,boolean gotSpeed,int validSensors){
  if(gotSpeed)return "차량 속도 응답 확인 · 자동 출발 기록은 안전한 장소에서 별도 검증 필요";
  if(validSensors>0&&speed==ObdPidCapabilities.Support.NO)
   return "ECU 센서 일부 수신 · 차량 속도 PID 미지원, OBD 자동 출발 기록 불가";
  if(validSensors>0)return "ECU 센서 응답 확인 · 속도 PID 실측 미확인, 자동 출발은 보류";
  if(speed==ObdPidCapabilities.Support.NO)
   return "차량 속도 PID 미지원 · OBD 자동 출발 기록 불가";
  return "Bluetooth는 연결됐지만 ECU 센서 응답은 아직 확인되지 않았어요";
 }
 static String failure(Exception error){
  if(error instanceof SecurityException)return "Bluetooth 접근 권한이 필요합니다. 설정에서 허용해 주세요.";
  if(error instanceof SocketTimeoutException)return "동글 응답이 늦습니다. 시동과 블루투스 연결을 확인해 주세요.";
  if(error instanceof IOException){
   String message=error.getMessage();
   if(message!=null&&message.startsWith("ECU 응답을"))return message;
  }
  return "OBD 통신 실패 · 시동 ON, 6핀 변환 케이블, Bluetooth 페어링 순서로 확인해 주세요.";
 }
}
