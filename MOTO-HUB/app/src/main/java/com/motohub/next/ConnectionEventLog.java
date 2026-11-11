package com.motohub.next;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.Locale;
/** Bounded per-bike event categories. No Bluetooth MAC, GPS, or raw ECU payloads. */
final class ConnectionEventLog {
 enum Event {
  CARDO_CONNECTED("카르도 연결 감지"),
  CARDO_DISCONNECTED("카르도 연결 끊김"),
  CONNECTION_START("자동 연결 시도"),
  OBD_CONNECTED("OBD Bluetooth 연결"),
  SPEED_SEEN("ECU 실제 속도 수신"),
  GPS_RECORDING("GPS 기록 시작"),
  GPS_SAVED("GPS 파일 저장 확인"),
  OBD_RETRY("OBD 다시 연결 중"),
  OBD_LOST("OBD 통신 끊김"),
  GPS_PERMISSION_BLOCKED("GPS 위치 수신 차단"),
  PERMISSION_BLOCKED("필수 권한·GPS 미충족"),
  BACKGROUND_BLOCKED("백그라운드 위치 권한 미허용 · 시작 알림 필요"),
  ANDROID_BLOCKED("Android 자동시작 제한 · 시작 알림 필요"),
  GPS_FAILED("GPS 기록·저장 중단"),
  AUTO_STOPPED("자동기록 대기 종료"),
  UNKNOWN_ERROR("자동기록 중 오류 발생");
  final String text;
  Event(String t){text=t;}
 }
 private ConnectionEventLog(){}
 private static SharedPreferences p(Context c,String bike){
  return c.getSharedPreferences("moto_connection_events_v604_"+bike,0);
 }
 static synchronized void record(Context c,String bike,Event e){
  if(!RideBikeBinding.valid(bike)||e==null)return;
  SharedPreferences prefs=p(c,bike);
  long now=System.currentTimeMillis(),last=prefs.getLong("last_time",0);
  if(e.name().equals(prefs.getString("last_code",""))&&now>=last&&now-last<8_000L)return;
  String[] old=prefs.getString("events","").split("\\n");
  StringBuilder result=new StringBuilder(now+"|"+e.name());
  int kept=1;
  for(String line:old){
   if(kept>=20)break;
   if(line.matches("[0-9]{12,14}\\|[A-Z_]+")){
    result.append('\n').append(line);kept++;
   }
  }
  prefs.edit().putLong("last_time",now).putString("last_code",e.name())
   .putString("events",result.toString()).apply();
 }
 static void fromAutoStage(Context c,String bike,ObdAutoRideProgress.Stage stage){
  Event e=null;
  switch(stage){
   case CONNECTING:e=Event.CONNECTION_START;break;
   case CONNECTED:e=Event.OBD_CONNECTED;break;
   case SPEED:e=Event.SPEED_SEEN;break;
   case GPS:e=Event.GPS_RECORDING;break;
   case SAVED:e=Event.GPS_SAVED;break;
   case RETRYING:e=Event.OBD_RETRY;break;
   case STOPPED:e=Event.AUTO_STOPPED;break;
   case FAILED:e=Event.UNKNOWN_ERROR;break;
   default:break;
  }
  if(e!=null)record(c,bike,e);
 }
 static void categorizeStop(Context c,String bike,String message){
  String lower=message==null?"":message.toLowerCase(Locale.ROOT);
  Event e=lower.contains("gps")||lower.contains("위치")||lower.contains("좌표")?
   Event.GPS_FAILED:lower.contains("obd")||lower.contains("ecu")||lower.contains("블루투스")?
   Event.OBD_LOST:Event.AUTO_STOPPED;
  record(c,bike,e);
 }
 static String summary(Context c,String bike){
  if(!RideBikeBinding.valid(bike))return "바이크를 선택해.";
  String raw=p(c,bike).getString("events","");
  if(raw.isEmpty())return "아직 연결 단계 기록 없음 · 카르도 감지/OBD 시도부터 기록";
  StringBuilder out=new StringBuilder("최근 자동 연결 단계 (최신순)");
  java.text.SimpleDateFormat date=new java.text.SimpleDateFormat("MM-dd HH:mm:ss",Locale.KOREA);
  int count=0;long now=System.currentTimeMillis();
  for(String row:raw.split("\\n")){
   if(++count>12)break;
   String[] parts=row.split("\\|",2);
   if(parts.length!=2)continue;
   try{
    long when=Long.parseLong(parts[0]);
    Event event=Event.valueOf(parts[1]);
    if(when>0&&when<=now)out.append("\n").append(date.format(new java.util.Date(when)))
      .append(" · ").append(event.text);
   }catch(Exception ignored){}
  }
  return out.toString();
 }
}
