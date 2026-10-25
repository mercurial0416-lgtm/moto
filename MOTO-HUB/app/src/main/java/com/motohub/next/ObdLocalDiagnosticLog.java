package com.motohub.next;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Local-only, bounded, sanitized OBD session summary, never Bluetooth MAC or GPS. */
final class ObdLocalDiagnosticLog {
 private static final String PREFS="obd_diagnostics_local";
 private static final int MAX_ENTRIES=12;
 private ObdLocalDiagnosticLog(){}

 static void capability(Context c,ObdPidCapabilities support){
  if(support==null)return;
  String[] pids={"010D","010C","0105","0142"};
  StringBuilder bitmap=new StringBuilder();
  for(String pid:pids){
   ObdPidCapabilities.Support value=support.forPid(pid);
   bitmap.append(value==ObdPidCapabilities.Support.YES?'Y':value==ObdPidCapabilities.Support.NO?'N':'?');
  }
  c.getSharedPreferences(PREFS,0).edit().putString("capabilities",bitmap.toString())
   .putLong("capability_time",System.currentTimeMillis()).apply();
  record(c,"PID");
 }
 static void record(Context c,String code){
  if(!ObdDiagnosticEventPolicy.valid(code))return;
  SharedPreferences p=c.getSharedPreferences(PREFS,0);
  String now=new SimpleDateFormat("MM-dd HH:mm",Locale.KOREA).format(new Date());
  String[] previous=p.getString("events","").split("\\n",-1);
  StringBuilder history=new StringBuilder(now).append(" · ").append(ObdDiagnosticEventPolicy.text(code));
  int appended=1;
  for(String item:previous)if(!item.isEmpty()&&appended++<MAX_ENTRIES){
   // Discard unknown legacy/free-form records rather than persisting identifiers.
   if(item.length()<=90&&item.matches("[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2} · [가-힣0-9 ·]+"))
    history.append('\n').append(item);
  }
  p.edit().putString("events",history.toString()).apply();
 }
 static String summary(Context c){
  SharedPreferences p=c.getSharedPreferences(PREFS,0);
  String caps=p.getString("capabilities","");
  StringBuilder text=new StringBuilder("마지막 ECU 지원 목록: ");
  if(caps!=null&&caps.matches("[YN?]{4}")){
   String[] names={"속도","RPM","수온","전압"};
   for(int i=0;i<4;i++){
    if(i>0)text.append(" · ");
    text.append(names[i]).append(" ").append(caps.charAt(i)=='Y'?"지원":caps.charAt(i)=='N'?"미지원":"확인 불가");
   }
   long ms=p.getLong("capability_time",0);
   if(ms>0&&ms<=System.currentTimeMillis())
    text.append("\n확인일: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.KOREA).format(new Date(ms)));
  }else text.append("기록 없음");
  text.append("\n※ 이전 검사 참고값입니다. 다음 연결 때 ECU에서 다시 확인합니다.");
  String events=p.getString("events","");
  if(events!=null&&!events.isEmpty())text.append("\n\n최근 연결 점검\n").append(events);
  return text.toString();
 }
 static void clear(Context c){c.getSharedPreferences(PREFS,0).edit().clear().apply();}
}
