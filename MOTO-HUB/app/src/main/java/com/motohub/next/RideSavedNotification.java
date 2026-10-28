package com.motohub.next;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
/** Verified GPS-only completion alert, containing no location or bike identifiers. */
final class RideSavedNotification {
 private RideSavedNotification(){}
 static void post(Context context){
  try{
   NotificationManager manager=context.getSystemService(NotificationManager.class);
   if(manager==null||!manager.areNotificationsEnabled())return;
   if(Build.VERSION.SDK_INT>=33&&context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
   manager.createNotificationChannel(new NotificationChannel("moto_ride_saved","라이딩 저장 완료",NotificationManager.IMPORTANCE_DEFAULT));
   Intent open=new Intent(context,MainActivity.class).putExtra(MotoWidgetProvider.EXTRA_TAB,"라이딩")
    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
   PendingIntent destination=PendingIntent.getActivity(context,406,open,
    PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   manager.notify(406,new Notification.Builder(context,"moto_ride_saved")
    .setSmallIcon(R.drawable.ic_notification).setContentTitle("라이딩 GPS 저장 완료")
    .setContentText("GPS 원본을 검증해 저장했습니다. 눌러서 주행 내역 보기")
    .setContentIntent(destination).setAutoCancel(true)
    .setVisibility(Notification.VISIBILITY_PRIVATE).build());
  }catch(RuntimeException ignored){/* Notification failure never modifies verified GPS. */}
 }
}
