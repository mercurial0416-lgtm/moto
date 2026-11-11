package com.motohub.next;
import android.content.*;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.SystemClock;
import java.util.Locale;
/** Approximate device-wide battery trend, never claims app-specific battery attribution. */
final class RidePowerAudit {
 private RidePowerAudit(){}
 private static SharedPreferences prefs(Context c,String bike){
  return c.getSharedPreferences("auto_ride_power_v601_"+bike,0);
 }
 private static int battery(Context c){
  try{
   Intent i=c.registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
   if(i==null)return -1;
   int level=i.getIntExtra(BatteryManager.EXTRA_LEVEL,-1);
   int scale=i.getIntExtra(BatteryManager.EXTRA_SCALE,-1);
   return level<0||scale<=0?-1:Math.max(0,Math.min(100,Math.round(100f*level/scale)));
  }catch(RuntimeException ignored){return -1;}
 }
 private static boolean charging(Context c){
  try{
   Intent i=c.registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
   return i!=null&&i.getIntExtra(BatteryManager.EXTRA_PLUGGED,0)!=0;
  }catch(RuntimeException ignored){return false;}
 }
 static void begin(Context c,String bike){
  if(!RideBikeBinding.valid(bike))return;
  SharedPreferences p=prefs(c,bike);
  if(p.getBoolean("active",false)&&
    SystemClock.elapsedRealtime()-p.getLong("startElapsed",0)<60_000L)return;
  p.edit().putBoolean("active",true).putInt("startPercent",battery(c))
   .putInt("lastPercent",battery(c)).putLong("startElapsed",SystemClock.elapsedRealtime())
   .putLong("lastSample",SystemClock.elapsedRealtime())
   .putBoolean("charged",charging(c)).commit();
 }
 static void sample(Context c,String bike){
  if(!RideBikeBinding.valid(bike))return;
  SharedPreferences p=prefs(c,bike);
  if(!p.getBoolean("active",false))return;
  long now=SystemClock.elapsedRealtime(),last=p.getLong("lastSample",0);
  if(last>0&&now-last<60_000L)return;
  p.edit().putLong("lastSample",now).putInt("lastPercent",battery(c))
   .putBoolean("charged",p.getBoolean("charged",false)||charging(c)).apply();
 }
 static void finish(Context c,String bike){
  if(!RideBikeBinding.valid(bike))return;
  SharedPreferences p=prefs(c,bike);
  if(!p.getBoolean("active",false))return;
  long now=SystemClock.elapsedRealtime(),started=p.getLong("startElapsed",0);
  long minutes=started>0&&now>=started?(now-started)/60_000L:0;
  p.edit().putBoolean("active",false).putLong("minutes",minutes)
   .putInt("endPercent",battery(c)).putBoolean("charged",p.getBoolean("charged",false)||charging(c))
   .putLong("finishedAt",System.currentTimeMillis()).commit();
 }
 static String summary(Context c,String bike){
  if(!RideBikeBinding.valid(bike))return "";
  SharedPreferences p=prefs(c,bike);
  if(p.getBoolean("active",false)){
   long started=p.getLong("startElapsed",0),now=SystemClock.elapsedRealtime();
   if(started>0&&now>=started)
    return "자동기록 감시 배터리 관찰 중 · "+((now-started)/60_000L)+"분 · 기기 전체 잔량 "+p.getInt("lastPercent",-1)+"%";
   return "이전 배터리 관찰이 중단됐습니다 · 다시 연결해 측정";
  }
  if(!p.contains("finishedAt"))return "자동기록 감시 배터리 측정 내역 없음";
  int start=p.getInt("startPercent",-1),end=p.getInt("endPercent",-1);
  boolean charging=p.getBoolean("charged",false);
  return "마지막 자동 감시 "+p.getLong("minutes",0)+"분 · "+
   (start<0||end<0?"배터리 측정 불가":charging?"충전 포함 · 단순 소모량 비교 불가":
    "기기 전체 배터리 "+Math.max(0,start-end)+"%p 감소")+
   "\n※ 휴대폰 전체 사용량이며 MOTO HUB 단독 전력소모량은 아닙니다.";
 }
}
