package com.motohub.next;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.SystemClock;

/**
 * Exact-MAC Cardo trigger. No scan loop. Foreground service starts only if
 * location permissions permit it; Android background denial -> tap notification.
 */
final class CardoRideStarter {
 static final String ACTION_TAP="com.motohub.next.CARDO_TAP_START";
 private static final String CHANNEL="cardo_auto_v595";
 private CardoRideStarter(){}

 static boolean eligible(Context c){
  if(!CardoAutoRidePrefs.enabled(c)||!CardoAutoRidePrefs.forBike(c,CardoAutoRidePrefs.bikeId(c)))return false;
  if(c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)return false;
  if(Build.VERSION.SDK_INT>=31&&c.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)return false;
  if(Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return false;
  LocationManager lm=c.getSystemService(LocationManager.class);
  return lm!=null&&lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
 }

 static void cardoEvent(Context c,String mac,boolean connected){
  if(!CardoAutoRidePrefs.matches(c,mac))return;
  if(!connected)CardoAutoRidePrefs.noteDisconnect(c,mac);
  else CardoAutoRidePrefs.notePresence(c,mac,true);
  // Update the already-running FGS directly. Android can reject starting a
  // service from an ACL broadcast in the background. Two presence sources
  // may emit the same event; the FGS uses a 10-second disconnect grace.
  if(RideService.cardoPresenceChanged(CardoAutoRidePrefs.bikeId(c),connected)){
   CardoAutoRidePrefs.status(c,connected?
     "카르도 재연결됨 · 자동 기록 감지 유지":
     "카르도 잠시 끊김 · 10초 재연결 확인");
   return;
  }
  if(!connected){
   cancelTap(c);
   CardoAutoRidePrefs.status(c,"카르도 연결 해제 · 다음 연결 대기");
   return;
  }
  // Do not consume the debounce slot until permissions and FGS conditions pass.
  if(!eligible(c)){CardoAutoRidePrefs.status(c,"권한/GPS 설정 필요 · 알림에서 확인");notifyTap(c);return;}
  if(Build.VERSION.SDK_INT>=29&&!MainActivity.obdVisible&&
     c.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)!=PackageManager.PERMISSION_GRANTED){
   CardoAutoRidePrefs.status(c,"백그라운드 위치 허용 안 됨 · 알림에서 시작");
   notifyTap(c);return;
  }
  boolean inRide=RideService.autoArmed||RideService.running;
  if(!CardoAutoRidePrefs.claim(c,mac,inRide))return;
  if(!start(c)){
   CardoAutoRidePrefs.releaseClaim(c,mac);
   CardoAutoRidePrefs.status(c,"Android 백그라운드 시작 제한 · 알림으로 시작");
   notifyTap(c);
  }
 }
 static boolean start(Context c){
  if(!eligible(c)||RideService.running||RideService.autoArmed)return false;
  String bike=CardoAutoRidePrefs.bikeId(c);
  String obd=BikeObdPreferences.address(c,bike);
  if(!CardoAutoRidePrefs.forBike(c,bike)||
     (!BikeObdPreferences.validAddress(obd)&&!BikeObdPreferences.gpsFallbackEnabled(c,bike)))return false;
  try{
   Intent i=new Intent(c,RideService.class).setAction(RideService.AUTO_ARM)
     .putExtra(RideService.EXTRA_BIKE_ID,bike)
     .putExtra(RideService.EXTRA_DEVICE,obd)
     .putExtra(RideService.EXTRA_CARDO_TRIGGER,true);
   if(Build.VERSION.SDK_INT>=26)c.startForegroundService(i);
   else c.startService(i);
   CardoAutoRidePrefs.status(c,"카르도 감지 → OBD 연결 시도 중");
   cancelTap(c);
   return true;
  }catch(RuntimeException ex){return false;}
 }
 static void startFromTap(MainActivity a){startFromTap(a,false);}
 static void startFromTap(MainActivity a,boolean fromNotification){
  if(!CardoAutoRidePrefs.enabled(a))return;
  if(fromNotification&&!CardoAutoRidePrefs.recentlyConnected(a)){
   cancelTap(a);a.toast("카르도 연결 상태를 다시 확인해 주세요 · 오래된 알림은 실행하지 않았습니다");
   return;
  }
  if(!eligible(a)){a.toast("정확한 위치·주변 기기·알림 권한 및 GPS 확인");return;}
  if(RideService.autoArmed||RideService.running){a.toast("OBD 자동 감지가 이미 진행 중");cancelTap(a);return;}
  if(!start(a))a.toast("OBD 시작 제한 · 차고에서 연결 설정을 확인해 주세요");
  else a.toast("카르도 자동 라이딩 연결 시작 · 출발하면 GPS 기록");
 }
 static void notifyTap(Context c){
  if(Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
  NotificationManager nm=c.getSystemService(NotificationManager.class);
  if(nm==null||!nm.areNotificationsEnabled())return;
  nm.createNotificationChannel(new NotificationChannel(CHANNEL,"카르도 라이딩 시작",NotificationManager.IMPORTANCE_DEFAULT));
  Intent open=new Intent(c,MainActivity.class).setAction(ACTION_TAP)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
  PendingIntent action=PendingIntent.getActivity(c,595,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification notice=new Notification.Builder(c,CHANNEL)
    .setSmallIcon(android.R.drawable.ic_menu_mylocation)
    .setContentTitle("카르도 연결 감지 · OBD 라이딩 시작")
    .setContentText("Android 백그라운드 시작 제한 · 눌러서 자동기록 대기")
    .setVisibility(Notification.VISIBILITY_PRIVATE)
    .setAutoCancel(true).setContentIntent(action).build();
  nm.notify(595,notice);
 }
 static void cancelTap(Context c){
  NotificationManager nm=c.getSystemService(NotificationManager.class);
  if(nm!=null)nm.cancel(595);
 }
}