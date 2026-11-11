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
 private static SharedPreferences prefs(Context c){
  String id="";
  if(c instanceof MainActivity){
   org.json.JSONObject bike=((MainActivity)c).bike();
   if(bike!=null)id=bike.optString("id","");
  }
  if(!RideBikeBinding.valid(id))id=RideService.activeRideBikeId;
  if(!RideBikeBinding.valid(id))return c.getSharedPreferences(PREFS,0);
  String mac=id.equals(RideService.activeRideBikeId)?
    RideService.instance==null?"":RideService.instance.activeAutoAdapterAddress:"";
  if(!BikeObdPreferences.validAddress(mac))mac=BikeObdPreferences.address(c,id);
  if(!BikeObdPreferences.validAddress(mac))
   return c.getSharedPreferences(PREFS+"_"+id+"_no_obd",0);
  // A local random salt keeps MAC addresses out of preference names and logs;
  // switching dongles never inherits another dongle's measured capabilities.
  try{
   SharedPreferences ns=c.getSharedPreferences("obd_diag_namespace_v601",0);
   String salt=ns.getString("salt","");
   if(salt.length()!=64){
    byte[] bytes=new byte[32];new java.security.SecureRandom().nextBytes(bytes);
    StringBuilder b=new StringBuilder();
    for(byte v:bytes)b.append(String.format(java.util.Locale.ROOT,"%02x",v&255));
    salt=b.toString();ns.edit().putString("salt",salt).commit();
   }
   java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-256");
   byte[] fingerprint=digest.digest((salt+"|"+id+"|"+mac.toUpperCase(Locale.ROOT))
     .getBytes(java.nio.charset.StandardCharsets.UTF_8));
   StringBuilder hex=new StringBuilder();
   for(int i=0;i<12;i++)hex.append(String.format(Locale.ROOT,"%02x",fingerprint[i]&255));
   return c.getSharedPreferences(PREFS+"_"+id+"_"+hex,0);
  }catch(Exception unavailable){return c.getSharedPreferences(PREFS+"_"+id+"_local",0);}
 }
 private ObdLocalDiagnosticLog(){}

 static void capability(Context c,ObdPidCapabilities support){
  if(support==null)return;
  String[] pids={"010D","010C","0105","0142","012F"};
  StringBuilder bitmap=new StringBuilder();
  for(String pid:pids){
   ObdPidCapabilities.Support value=support.forPid(pid);
   bitmap.append(value==ObdPidCapabilities.Support.YES?'Y':value==ObdPidCapabilities.Support.NO?'N':'?');
  }
  prefs(c).edit().putString("capabilities",bitmap.toString())
   .putInt("observed_mask",0).putLong("capability_time",System.currentTimeMillis()).apply();
  record(c,"PID");
 }
 /** A capability bitmap says a PID exists; only a decoded reply proves that we received values. */
 static int bitForPid(String pid){
  for(int i=0;i<ObdReadOnlyProtocol.SENSOR_PIDS.length;i++)
   if(ObdReadOnlyProtocol.SENSOR_PIDS[i].equals(pid))return 1<<i;
  return 0;
 }
 static void observed(Context c,String pid){
  int bit=bitForPid(pid);
  if(bit==0)return;
  SharedPreferences preferences=prefs(c);
  int previous=preferences.getInt("observed_mask",0);
  if((previous&bit)==0)preferences.edit().putInt("observed_mask",previous|bit).apply();
 }
 /** End-of-session auto OBD report: bounded counters and one allowlisted diagnosis.
  * Never store adapter MAC, connection address, raw ECU response or GPX coordinates. */
 static void automaticSession(Context c,int attempts,int successes,int reconnects,
                              int queryFailures,long speedSamples,String reason){
  String safe=ObdFailureGuide.classify(reason);
  prefs(c).edit()
   .putLong("auto_at",System.currentTimeMillis())
   .putInt("auto_attempts",Math.max(0,Math.min(attempts,10_000)))
   .putInt("auto_successes",Math.max(0,Math.min(successes,10_000)))
   .putInt("auto_reconnects",Math.max(0,Math.min(reconnects,10_000)))
   .putInt("auto_failures",Math.max(0,Math.min(queryFailures,1_000_000)))
   .putLong("auto_samples",Math.max(0,Math.min(speedSamples,10_000_000L)))
   .putString("auto_reason",safe)
   .apply();
 }
 /** Store one bounded diagnostic summary at session end, never MAC, raw payload or coordinates. */
 static void probeMetrics(Context c,boolean connected,int validReplies,int failures,
                          long averageLatencyMs,long lastReceivedAt){
  prefs(c).edit()
   .putLong("probe_at",System.currentTimeMillis())
   .putBoolean("probe_connected",connected)
   .putInt("probe_valid",Math.max(0,Math.min(validReplies,1_000_000)))
   .putInt("probe_failed",Math.max(0,Math.min(failures,1_000_000)))
   .putLong("probe_latency_ms",Math.max(-1,Math.min(averageLatencyMs,60_000)))
   .putLong("probe_last_received",lastReceivedAt>0&&lastReceivedAt<=System.currentTimeMillis()?lastReceivedAt:0)
   .apply();
 }
 static void record(Context c,String code){
  if(!ObdDiagnosticEventPolicy.valid(code))return;
  SharedPreferences p=prefs(c);
  String now=new SimpleDateFormat("MM-dd HH:mm",Locale.KOREA).format(new Date());
  String[] previous=p.getString("events","").split("\\n",-1);
  StringBuilder history=new StringBuilder(now).append(" · ").append(ObdDiagnosticEventPolicy.text(code));
  int appended=1;
  for(String item:previous){
   // Malformed legacy records must not consume the limited safe-history quota.
   if(item.isEmpty()||item.length()>90||
      !item.matches("[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2} · [가-힣0-9 ·]+"))continue;
   if(appended>=MAX_ENTRIES)break;
   history.append('\n').append(item);
   appended++;
  }
  p.edit().putString("events",history.toString()).apply();
 }
 static String summary(Context c){
  SharedPreferences p=prefs(c);
  String caps=p.getString("capabilities","");
  String scope=c instanceof MainActivity||RideBikeBinding.valid(RideService.activeRideBikeId)?
   "진단 범위: 선택 바이크 + 현재 OBD 동글 (MAC 비저장)\n":"";

  StringBuilder text=new StringBuilder(scope+"마지막 ECU PID 선언 목록 (실제 수신과 별개): ");
  if(caps!=null&&caps.matches("[YN?]{4,5}")){
   String[] names={"속도","RPM","수온","전압","연료"};
   for(int i=0;i<caps.length();i++){
    if(i>0)text.append(" · ");
    text.append(names[i]).append(" ").append(caps.charAt(i)=='Y'?"지원":caps.charAt(i)=='N'?"미지원":"확인 불가");
   }
   long ms=p.getLong("capability_time",0);
   if(ms>0&&ms<=System.currentTimeMillis())
    text.append("\n확인일: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.KOREA).format(new Date(ms)));
  }else text.append("기록 없음");
  text.append("\n※ 이전 검사 참고값입니다. 다음 연결 때 ECU에서 다시 확인합니다.");
  int observed=p.getInt("observed_mask",0);
  if(observed!=0){
   StringBuilder samples=new StringBuilder("\n이 기기에서 실제 수신한 센서: ");
   for(int i=0;i<ObdReadOnlyProtocol.SENSOR_PIDS.length;i++)if((observed&(1<<i))!=0){
    if(samples.charAt(samples.length()-1)!=' ')samples.append(" · ");
    samples.append(ObdReadOnlyProtocol.SENSOR_NAMES[i]);
   }
   text.append(samples);
  }else text.append("\n실제 센서값 수신 기록 없음 · PID 목록만 확인됨");
  long probeAt=p.getLong("probe_at",0),lastReply=p.getLong("probe_last_received",0);
  if(probeAt>0&&probeAt<=System.currentTimeMillis()){
   text.append("\n\n마지막 수동 OBD 센서 검사: ")
    .append(new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.KOREA).format(new Date(probeAt)))
    .append("\n연결: ").append(p.getBoolean("probe_connected",false)?"Bluetooth 성공":"실패/중단")
    .append(" · 센서 응답 ").append(Math.max(0,p.getInt("probe_valid",0))).append("회")
    .append(" · 실패 ").append(Math.max(0,p.getInt("probe_failed",0))).append("회")
    .append(" · 자동 재연결 0회 (수동 검사)");
   long latency=p.getLong("probe_latency_ms",-1);
   text.append("\n평균 ECU 응답: ").append(latency>=0?latency+"ms":"측정 불가");
   if(lastReply>0&&lastReply<=System.currentTimeMillis())
    text.append("\n마지막 정상 센서 수신: ")
     .append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.KOREA).format(new Date(lastReply)));
  }
  if(RideService.autoArmed){
   text.append("\n\n현재 OBD 자동 기록 · 연결 시도 ")
    .append(Math.max(0,RideService.obdConnectionAttempts)).append("회 · 성공 ")
    .append(Math.max(0,RideService.obdConnectionSuccesses)).append("회 · 재연결 ")
    .append(Math.max(0,RideService.obdReconnects)).append("회 · 쿼리 실패 ")
    .append(Math.max(0,RideService.obdQueryFailures)).append("회");
   text.append("\n최근 측정 응답: ")
    .append(RideService.obdAverageLatencyMs>=0?RideService.obdAverageLatencyMs+"ms":"측정 불가");
  }
  long ended=p.getLong("auto_at",0);
  if(ended>0&&ended<=System.currentTimeMillis()){
   text.append("\n\n마지막 OBD 자동 연결: ")
    .append(new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.KOREA).format(new Date(ended)))
    .append("\n연결 시도 ").append(p.getInt("auto_attempts",0)).append("회 · 성공 ")
    .append(p.getInt("auto_successes",0)).append("회 · 재시도 ")
    .append(p.getInt("auto_reconnects",0)).append("회")
    .append("\n속도 응답 ").append(p.getLong("auto_samples",0)).append("회 · 쿼리 오류 ")
    .append(p.getInt("auto_failures",0)).append("회")
    .append("\n진단: ").append(p.getString("auto_reason","확인되지 않음"))
    .append("\n실차 ECU 호환성은 연결 상태에서 직접 확인해야 합니다.");
  }
  String events=p.getString("events","");
  if(events!=null&&!events.isEmpty())text.append("\n\n최근 연결 점검\n").append(events);
  return text.toString();
 }
 static void clear(Context c){prefs(c).edit().clear().apply();}
}
