package com.motohub.next;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.nio.charset.StandardCharsets;
import android.util.AtomicFile;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/** Periodic local safety backup; maintenance notifications are strictly opt-in. No network calls. */
public final class DailyMaintenanceJob extends JobService {
 static final int JOB_ID=4839;
 static boolean remindersEnabled(Context c){return c.getSharedPreferences("notifications",Context.MODE_PRIVATE).getBoolean("maintenance_opt_in",false);}
 static void schedule(Context context){
  try{JobScheduler scheduler=context.getSystemService(JobScheduler.class);
   if(scheduler==null)return;
   if(scheduler.getPendingJob(JOB_ID)!=null)return; // Reopening the app must not postpone the next run.
   JobInfo info=new JobInfo.Builder(JOB_ID,new ComponentName(context,DailyMaintenanceJob.class))
    .setPeriodic(24*60*60*1000L,6*60*60*1000L)
    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
    .setPersisted(false).build();
   scheduler.schedule(info);
  }catch(Exception ignored){} // Local writes must not fail because the scheduler is unavailable.
 }
 static void checkSoon(Context context){
  try{JobScheduler scheduler=context.getSystemService(JobScheduler.class);
   if(scheduler==null)return;
   JobInfo once=new JobInfo.Builder(JOB_ID+1,new ComponentName(context,DailyMaintenanceJob.class))
    .setMinimumLatency(0).setOverrideDeadline(60_000L)
    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE).build();
   scheduler.schedule(once);
  }catch(Exception ignored){}
 }
 @Override public boolean onStartJob(JobParameters params){
  new Thread(()->{
   try{
    File file=new File(getFilesDir(),"motohub-v3.json");
    if(file.isFile()&&file.length()>0&&file.length()<8_000_000){
     JSONObject db=Store.parse(new String(new AtomicFile(file).readFully(),StandardCharsets.UTF_8));
     LocalSafetyBackup.snapshot(this,db); // immutable daily snapshots
     // Do not distract with a noncritical service reminder during an active ride.
     if(remindersEnabled(this)&&!RideService.running)sendMaintenanceReminder(db);
     else {NotificationManager notificationManager=getSystemService(NotificationManager.class);if(notificationManager!=null)notificationManager.cancel(405);}
    }
   }catch(Exception ignored){}finally{jobFinished(params,false);}
  },"MOTO-local-daily").start();return true;
 }
 @Override public boolean onStopJob(JobParameters params){return true;}
 void sendMaintenanceReminder(JSONObject db){
  if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
  NotificationManager manager=getSystemService(NotificationManager.class);
  if(manager==null||!manager.areNotificationsEnabled())return;
  String today=LocalDate.now(ZoneId.of("Asia/Seoul")).toString();
  MaintenanceNotificationSummary reminder=MaintenanceNotificationSummary.from(db,today);
  if(!reminder.hasNotice()){
   manager.cancel(405);
   getSharedPreferences("notifications",MODE_PRIVATE).edit().remove("last_maintenance_notice").apply();
   return;
  }
  String stamp=today+":"+reminder.fingerprint;
  android.content.SharedPreferences prefs=getSharedPreferences("notifications",MODE_PRIVATE);
  if(stamp.equals(prefs.getString("last_maintenance_notice","")))return;
  manager.createNotificationChannel(new NotificationChannel("maintenance","정비 예정 알림",NotificationManager.IMPORTANCE_DEFAULT));
  Intent intent=new Intent(this,MainActivity.class);intent.putExtra(MotoWidgetProvider.EXTRA_TAB,"차고");
  PendingIntent open=PendingIntent.getActivity(this,103,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification notification=new Notification.Builder(this,"maintenance")
   .setSmallIcon(R.drawable.ic_launcher)
   .setContentTitle(reminder.title())
   .setContentText(reminder.detail)
   .setStyle(new Notification.BigTextStyle().bigText(reminder.detail))
   .setVisibility(Notification.VISIBILITY_PRIVATE)
   .setContentIntent(open).setAutoCancel(true).build();
  manager.notify(405,notification);
  prefs.edit().putString("last_maintenance_notice",stamp).apply();
 }
}
