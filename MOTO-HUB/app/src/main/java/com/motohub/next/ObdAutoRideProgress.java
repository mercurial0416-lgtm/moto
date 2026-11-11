package com.motohub.next;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Coordinate-free, per-bike checkpoint of each automatically armed ride. */
final class ObdAutoRideProgress {
 enum Stage { CONNECTING, CONNECTED, CARDO_GPS, SPEED, GPS, SAVED, RETRYING, STOPPED, FAILED }
 private ObdAutoRideProgress(){}
 private static SharedPreferences prefs(Context c,String bike){
  return c.getSharedPreferences("obd_auto_progress_v600_"+bike,0);
 }
 static void stage(Context c,String bike,Stage stage){
  if(!RideBikeBinding.valid(bike)||stage==null)return;
  long now=System.currentTimeMillis();
  SharedPreferences.Editor e=prefs(c,bike).edit()
   .putString("stage",stage.name()).putLong("at",now);
  if(stage==Stage.SPEED)e.putLong("speedAt",now);
  if(stage==Stage.SAVED)e.putLong("savedAt",now);
  String line=now+"|"+stage.name();
  String old=prefs(c,bike).getString("timeline","");
  StringBuilder trace=new StringBuilder(line);
  int kept=1;
  for(String prior:old.split("\\n")){
   if(kept>=16)break;
   if(prior.matches("[0-9]{12,14}\\|[A-Z_]+")){
    trace.append('\n').append(prior);kept++;
   }
  }
  e.putString("timeline",trace.toString()).apply();
  ConnectionEventLog.fromAutoStage(c,bike,stage);
 }
 static String summary(Context c,String bike){
  if(!RideBikeBinding.valid(bike))return "바이크 선택 후 자동기록 확인";
  SharedPreferences p=prefs(c,bike);
  String raw=p.getString("stage","");
  if(raw.isEmpty())return "자동기록 시도 내역 없음";
  Stage state;
  try{state=Stage.valueOf(raw);}catch(Exception invalid){return "진단 기록을 다시 확인해 주세요";}
  String message;
  switch(state){
   case CONNECTING:message="OBD Bluetooth 연결 중";break;
   case CONNECTED:message="Bluetooth 연결됨 · ECU 속도 대기";break;
   case CARDO_GPS:message="카르도 연결됨 · OBD 없는 GPS 안전망 대기";break;
   case SPEED:message="ECU 속도 수신됨 · 자동출발 확인";break;
   case GPS:message="GPS 자동 기록 시작";break;
   case SAVED:message="GPS 경로 검증·저장 완료";break;
   case RETRYING:message="OBD 재접속 시도 중";break;
   case STOPPED:message="자동 기록 감지 중지";break;
   default:message="자동 출발 실패 · GPS 기록/권한 확인";break;
  }
  long at=p.getLong("at",0);
  if(at>0&&at<=System.currentTimeMillis())
   message+=" · "+new SimpleDateFormat("MM-dd HH:mm:ss",Locale.KOREA).format(new Date(at));
  long saved=p.getLong("savedAt",0);
  if(saved>0&&saved<=System.currentTimeMillis()&&state!=Stage.SAVED)
   message+="\n마지막 자동저장: "+new SimpleDateFormat("MM-dd HH:mm",Locale.KOREA).format(new Date(saved));
  String trace=p.getString("timeline","");
  if(!trace.isEmpty()){
   StringBuilder shown=new StringBuilder("\n최근 자동기록 단계 (최신순)");
   int displayed=0;
   for(String line:trace.split("\\n")){
    if(++displayed>6)break;
    String[] pieces=line.split("\\|",2);
    if(pieces.length!=2)continue;
    try{
     long time=Long.parseLong(pieces[0]);
     Stage stage=Stage.valueOf(pieces[1]);
     if(time<=0||time>System.currentTimeMillis())continue;
     shown.append("\n")
      .append(new SimpleDateFormat("HH:mm:ss",Locale.KOREA).format(new Date(time)))
      .append(" · ").append(stage.name());
    }catch(Exception ignored){}
   }
   message+=shown;
  }
  return message;
 }
}
