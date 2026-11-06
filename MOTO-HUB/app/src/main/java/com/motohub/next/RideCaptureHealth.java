package com.motohub.next;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Durable, coordinate-free health of the last GPS recording attempt.
 * Never infers movement from a started foreground service alone.
 */
final class RideCaptureHealth {
 private static final String PREFS="ride_capture_health";
 private static final String STATE="state",MESSAGE="message",UPDATED="updated";
 private RideCaptureHealth(){}

 private static SharedPreferences prefs(Context context){
  return context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
 }
 private static void put(Context context,String state,String message){
  prefs(context).edit().putString(STATE,state).putString(MESSAGE,message)
   .putLong(UPDATED,System.currentTimeMillis()).commit();
 }
 static void started(Context context){
  put(context,"waiting","GPS 서비스 시작됨 · 유효한 위치 좌표 수신 대기");
 }
 static void firstFix(Context context){
  put(context,"receiving","GPS 좌표 수신 확인 · 주행 경로 기록 중");
 }
 static void finished(Context context,boolean verified,int points){
  if(verified){
   put(context,"saved","GPS 경로 저장 완료 · 좌표 "+Math.max(0,points)+"개");
  }else if(points<=0){
   put(context,"no_fix","기록 없음 · 유효한 GPS 좌표가 0개라 완료 경로가 생성되지 않았어요. 위치·정확한 위치 권한을 확인하고 실외에서 다시 시도해 주세요.");
  }else{
   put(context,"failed","GPS 저장 검증 실패 · 원본이 남아 있을 수 있어요. 라이딩 기록의 중단/복구 항목을 확인해 주세요.");
  }
 }
 static void failedIfActive(Context context,String reason){
  String state=prefs(context).getString(STATE,"");
  if(!"waiting".equals(state)&&!"receiving".equals(state))return;
  String why=reason==null?"원인을 확인하지 못했어요.":reason.trim();
  if(why.length()>160)why=why.substring(0,160);
  put(context,"failed","GPS 기록 중단 · "+why);
 }
 static String summary(Context context,boolean running){
  SharedPreferences p=prefs(context);
  String state=p.getString(STATE,"");
  if(state.isEmpty())return "";
  if(!running&&("waiting".equals(state)||"receiving".equals(state)))
   return "이전 기록이 정상 종료되지 않았을 수 있어요. 라이딩 기록에서 중단된 경로를 확인해 주세요.";
  return p.getString(MESSAGE,"");
 }
 static long updatedAt(Context context){return prefs(context).getLong(UPDATED,0L);}
 static String state(Context context){return prefs(context).getString(STATE,"");}
}
