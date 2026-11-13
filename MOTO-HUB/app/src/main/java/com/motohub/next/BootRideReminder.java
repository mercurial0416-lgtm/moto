package com.motohub.next;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import java.io.File;

/** Boot is notification-only. Never starts location, ECU polling, or group sharing. */
public final class BootRideReminder extends BroadcastReceiver {
 static final int NOTICE_ID=406;
 @Override public void onReceive(Context context,Intent intent){
  if(intent==null)return;
  String action=intent.getAction();
  if(Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)){
   MotoWidgetProvider.refresh(context);return;
  }
  if(!Intent.ACTION_BOOT_COMPLETED.equals(action))return;
  final PendingResult result=goAsync();
  new Thread(()->{
   try {
    // JobScheduler non-persisted jobs disappear on reboot. Restore daily local
    // maintenance checks without starting GPS, Bluetooth or group sharing.
    DailyMaintenanceJob.schedule(context);
    MotoWidgetProvider.refresh(context);
    if(DailyMaintenanceJob.remindersEnabled(context))DailyMaintenanceJob.checkSoon(context);
    if(Build.VERSION.SDK_INT>=33&&context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
    NotificationManager manager=context.getSystemService(NotificationManager.class);
    if(manager==null||!manager.areNotificationsEnabled())return;
    // Android does not permit us to silently launch a location FGS from boot.
    // A clear tap-to-resume notification preserves the user's previous consent.
    offerHandsFreeResume(context,manager);
    if(!BootRidePolicy.shouldOffer(true,true,BootRidePolicy.hasRecoverable(context.getFilesDir())))return;
    manager.createNotificationChannel(new NotificationChannel("ride_recovery","중단된 라이딩 복구",NotificationManager.IMPORTANCE_DEFAULT));
    Intent open=new Intent(context,MainActivity.class).putExtra(MotoWidgetProvider.EXTRA_TAB,"라이딩");
    open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
    PendingIntent pending=PendingIntent.getActivity(context,406,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    Notification notification=new Notification.Builder(context,"ride_recovery")
      .setSmallIcon(R.drawable.ic_notification).setContentTitle("확인할 라이딩 기록이 있어요")
      .setContentText("재부팅 전 중단된 경로를 앱에서 확인하고 복구할 수 있어요")
      .setContentIntent(pending).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE).build();
    manager.notify(NOTICE_ID,notification);
   }catch(Exception ignored){/* Boot never alters, deletes, or uploads local GPS data. */}
   finally{result.finish();}
  },"moto-boot-ride-notice").start();
 }
 private static void offerHandsFreeResume(Context context,NotificationManager manager){
  if(!ObdHandsFreeSession.enabled(context)||ObdHandsFreeSession.bikeId(context).isEmpty())return;
  manager.createNotificationChannel(new NotificationChannel("ride_resume","OBD 자동 기록 재개",NotificationManager.IMPORTANCE_DEFAULT));
  Intent open=new Intent(context,MainActivity.class).putExtra(MotoWidgetProvider.EXTRA_TAB,"라이딩");
  open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
  PendingIntent pending=PendingIntent.getActivity(context,407,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification note=new Notification.Builder(context,"ride_resume")
   .setSmallIcon(R.drawable.ic_notification).setContentTitle("OBD 자동 기록 재개")
   .setContentText("휴대폰 재시작 후 MOTO HUB를 한 번 열어 감지를 다시 켜세요")
   .setContentIntent(pending).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE).build();
  manager.notify(407,note);
 }

}
