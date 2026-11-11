package com.motohub.next;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.UUID;
/** Per-bike, per-request Android FGS start acknowledgment.  A successful
 * startForegroundService call is only a request, never proof of an armed ride. */
final class CardoStartHandshake {
 static final String EXTRA_TOKEN="cardo_start_request_v605";
 static final long ACK_TIMEOUT_MS=25_000L, AUTO_RETRY_COOLDOWN_MS=90_000L;
 private CardoStartHandshake(){}
 private static SharedPreferences prefs(Context c,String bike){
  return c.getSharedPreferences("cardo_start_ack_v605_"+bike,0);
 }
 static boolean within(long now,long at,long limit){
  return at>0&&now>=at&&now-at<limit;
 }
 private static void expire(Context c,String bike){
  if(!RideBikeBinding.valid(bike))return;
  SharedPreferences p=prefs(c,bike);
  if(!"requested".equals(p.getString("state","")))return;
  long now=System.currentTimeMillis();
  if(within(now,p.getLong("requestedAt",0),ACK_TIMEOUT_MS))return;
  p.edit().putString("state","timeout").putLong("failedAt",now).commit();
  CardoAutoRidePrefs.status(c,"서비스 시작 확인 안 됨 · 알림에서 다시 시작");
  CardoAutoRidePrefs.releaseClaim(c,CardoAutoRidePrefs.address(c));
  ConnectionEventLog.record(c,bike,ConnectionEventLog.Event.ANDROID_BLOCKED);
 }
 static boolean pending(Context c,String bike){
  if(!RideBikeBinding.valid(bike))return false;
  expire(c,bike);
  return "requested".equals(prefs(c,bike).getString("state",""));
 }
 static boolean automaticAllowed(Context c,String bike){
  if(!RideBikeBinding.valid(bike)||pending(c,bike))return false;
  SharedPreferences p=prefs(c,bike);
  String state=p.getString("state","");
  return !("failed".equals(state)||"timeout".equals(state))||
    !within(System.currentTimeMillis(),p.getLong("failedAt",0),AUTO_RETRY_COOLDOWN_MS);
 }
 static String request(Context c,String bike){
  if(!RideBikeBinding.valid(bike)||pending(c,bike))return "";
  String token=UUID.randomUUID().toString();
  boolean saved=prefs(c,bike).edit().putString("token",token)
   .putString("state","requested").putLong("requestedAt",System.currentTimeMillis()).commit();
  if(saved)CardoAutoRidePrefs.status(c,"Android에 자동 시작 요청 전달 · 서비스 준비 응답 대기");
  return saved?token:"";
 }
 static void armed(Context c,String bike,String token,boolean gpsOnly){
  if(token==null||token.isEmpty()||!RideBikeBinding.valid(bike))return;
  SharedPreferences p=prefs(c,bike);
  if(!token.equals(p.getString("token",""))||!"requested".equals(p.getString("state","")))return;
  p.edit().putString("state","armed").putLong("confirmedAt",System.currentTimeMillis()).commit();
  CardoAutoRidePrefs.status(c,gpsOnly?
   "자동 감시 서비스 준비 완료 · GPS 이동 감지는 아직 대기 중":
   "자동 감시 서비스 준비 완료 · 실제 ECU 속도 수신은 별도 확인 필요");
  CardoRideStarter.cancelTap(c);
 }
 static void rejected(Context c,String bike,String token){
  if(token==null||token.isEmpty()||!RideBikeBinding.valid(bike))return;
  SharedPreferences p=prefs(c,bike);
  if(!token.equals(p.getString("token",""))||!"requested".equals(p.getString("state","")))return;
  p.edit().putString("state","failed").putLong("failedAt",System.currentTimeMillis()).commit();
  CardoAutoRidePrefs.status(c,"자동 시작 거절됨 · 카르도 상태와 권한 확인 후 알림에서 재시작");
  CardoAutoRidePrefs.releaseClaim(c,CardoAutoRidePrefs.address(c));
  ConnectionEventLog.record(c,bike,ConnectionEventLog.Event.ANDROID_BLOCKED);
 }
 static void stopped(Context c,String bike,String token){
  if(token==null||token.isEmpty()||!RideBikeBinding.valid(bike))return;
  SharedPreferences p=prefs(c,bike);
  if(token.equals(p.getString("token",""))&&"armed".equals(p.getString("state","")))
   p.edit().putString("state","stopped").commit();
 }
 static String summary(Context c,String bike){
  if(!RideBikeBinding.valid(bike))return "바이크 선택 필요";
  if(pending(c,bike))return "Android 시작 요청 전달됨 · 실제 서비스 준비 확인 중";
  SharedPreferences p=prefs(c,bike);
  switch(p.getString("state","")){
   case "armed":return RideService.autoArmed&&bike.equals(RideService.activeRideBikeId)?
    "출발 감시 서비스 준비 완료 · ECU/GPS 데이터는 별도 확인":
    "이전 출발 감시가 중단됐을 수 있음 · 현재 상태 다시 확인";
   case "failed":return "자동 시작 실패 · 권한 확인 후 알림에서 수동 시작";
   case "timeout":return "Android 서비스 시작 응답 없음 · 알림에서 다시 시작";
   case "stopped":return "자동 출발 감시 종료됨 · 다음 연결 이벤트 대기";
   default:return "자동 시작 시도 전 · 실제 출발 감시 확인 안 됨";
  }
 }
 static void clear(Context c,String bike){
  if(RideBikeBinding.valid(bike))prefs(c,bike).edit().clear().commit();
 }
}
