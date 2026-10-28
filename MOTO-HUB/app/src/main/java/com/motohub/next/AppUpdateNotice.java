package com.motohub.next;

import android.app.AlertDialog;
import android.os.Build;
import android.content.Context;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/** Automatic *check*, never an automatic install; only shows a notice when safe. */
final class AppUpdateNotice {
 private static final AtomicBoolean ACTIVE=new AtomicBoolean();
 private static final long CHECK_INTERVAL=6L*60*60*1000;
 private AppUpdateNotice(){}
 static void checkOnResume(MainActivity a) {
  if(BuildConfig.STORE_DISTRIBUTION||BuildConfig.UPDATE_MANIFEST_URL.isEmpty()||a.readOnly
      ||a.suppressUpdatePrompt()||RideService.running||RideService.autoArmed||RestoreRideGate.isExporting()
      ||RestoreRideGate.isRestoring())return;
  if(!UpdateReadiness.fixedIdentity(SigningIdentity.current(a),UpdateManifest.PINNED_CERT_SHA256))return;
  long now=System.currentTimeMillis();
  android.content.SharedPreferences prefs=a.getSharedPreferences("update_notice",Context.MODE_PRIVATE);
  long last=prefs.getLong("last_check",0);
  if(now>last&&now-last<CHECK_INTERVAL)return;
  if(!ACTIVE.compareAndSet(false,true))return;
  // Failure also gets a short retry window: avoid a network request on every resume.
  prefs.edit().putLong("last_check",now).apply();
  new Thread(()->{
   HttpURLConnection connection=null;
   try{
    connection=AppUpdater.connect(BuildConfig.UPDATE_MANIFEST_URL);
    UpdateManifest manifest;
    try(InputStream in=connection.getInputStream()){
     manifest=new UpdateManifest(new String(UpdateManifest.bounded(in,65536),
           StandardCharsets.UTF_8),a.getPackageName());
    }
    if(manifest.minSdk>Build.VERSION.SDK_INT||
       !UpdateVersionPolicy.isUpgrade(manifest.versionCode,BuildConfig.VERSION_CODE))return;
    final long newest=manifest.versionCode;
    a.runOnUiThread(()->{
     if(a.isFinishing()||a.isDestroyed()||a.dialog!=null||a.suppressUpdatePrompt()||
        RideService.running||RideService.autoArmed||
        RestoreRideGate.isExporting()||RestoreRideGate.isRestoring())return;
     if(!UpdatePromptPolicy.shouldPrompt(newest,BuildConfig.VERSION_CODE,
          prefs.getLong("snoozed_version",0),prefs.getLong("snoozed_at",0),
          System.currentTimeMillis()))return;
     new AlertDialog.Builder(a).setTitle("MOTO HUB 새 버전")
      .setMessage("새 버전 "+manifest.versionName+"을(를) 앱 안에서 설치할 수 있습니다. 기존 데이터 유지를 위해 서명과 파일 무결성을 확인합니다.")
      .setNegativeButton("내일 다시 알림",(d,w)->prefs.edit()
       .putLong("snoozed_version",newest)
       .putLong("snoozed_at",System.currentTimeMillis()).apply())
      .setPositiveButton("앱 업데이트",(d,w)->{
       prefs.edit().putLong("snoozed_version",newest)
        .putLong("snoozed_at",System.currentTimeMillis()).apply();
       new AppUpdater(a).show();
      }).show();
    });
   }catch(Exception ignored){ /* Offline or bad manifest: current app keeps working. */ }
   finally{if(connection!=null)connection.disconnect();ACTIVE.set(false);}
  },"moto-update-check").start();
 }
}
