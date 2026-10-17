package com.motohub.next;
import android.app.*;import android.content.*;import android.content.pm.PackageManager;import android.content.pm.ServiceInfo;import android.location.*;import android.os.*;import java.io.*;import java.nio.charset.StandardCharsets;import java.util.UUID;
/** User-started location FGS. Local recording with explicit group sharing. No boot restart, network or mock positions. */
public final class RideService extends Service implements LocationListener {
 static final String STOP="com.motohub.next.STOP_RIDE";static volatile boolean running=false;static volatile String status="기록 꺼짐",active="";static volatile int count=0;static volatile long startedAt=0;static volatile double liveDistanceKm=0,liveSpeedKmh=0;
 static volatile RideService instance;
 @Override public void onCreate(){super.onCreate();instance=this;}
 static boolean shareAllowed(){RideService s=instance;return s!=null&&running&&s.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED&&(Build.VERSION.SDK_INT<33||s.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED)&&s.getSystemService(NotificationManager.class).areNotificationsEnabled();}
 static void refreshNotification(){RideService s=instance;if(s!=null)s.handler.post(()->{if(running)s.getSystemService(NotificationManager.class).notify(404,s.notification(count+"개 좌표 · "+(Sharing.enabled?"그룹 공유 켜짐":"공유 꺼짐")));});}
 LocationManager manager;FileOutputStream output;File track;long lastTime;Location lastGood;final Handler handler=new Handler(Looper.getMainLooper());
 final Runnable permissionCheck=new Runnable(){public void run(){if(!running)return;if(checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){stopWith("위치 권한이 해제되어 기록을 중지했습니다.",false);return;}if(Sharing.enabled&&((Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)||!getSystemService(NotificationManager.class).areNotificationsEnabled()))Sharing.stop();handler.postDelayed(this,5000);}};
 @Override public IBinder onBind(Intent i){return null;}
 @Override public int onStartCommand(Intent intent,int flags,int id){
  if(intent==null){stopWith("GPS 서비스가 중단됐습니다.",false);return START_NOT_STICKY;}if(STOP.equals(intent.getAction())){stopWith("기록 종료",true);return START_NOT_STICKY;}if(running)return START_NOT_STICKY;
  try{
   if(checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)throw new SecurityException("정확한 위치 권한이 필요합니다.");
   manager=getSystemService(LocationManager.class);if(manager==null||!manager.isProviderEnabled(LocationManager.GPS_PROVIDER))throw new IllegalStateException("기기의 GPS를 켜 주세요.");
   NotificationManager nm=getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel("ride","라이딩 GPS 기록",NotificationManager.IMPORTANCE_LOW));
   Notification n=notification("GPS 신호 대기 · 위치 공유 꺼짐");if(Build.VERSION.SDK_INT>=29)startForeground(404,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);else startForeground(404,n);
   track=new File(TrackFile.directory(getFilesDir()),System.currentTimeMillis()+"-"+UUID.randomUUID()+".track");output=new FileOutputStream(track);count=0;lastTime=0;lastGood=null;startedAt=System.currentTimeMillis();liveDistanceKm=0;liveSpeedKmh=0;active=track.getName();running=true;status="GPS 신호 대기";
   manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,5000,0,this,Looper.getMainLooper());handler.postDelayed(permissionCheck,5000);
  }catch(Exception e){stopWith(e.getMessage()==null?"GPS 시작 실패":e.getMessage(),false);}return START_NOT_STICKY;
 }
 Notification notification(String text){PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,RideService.class).setAction(STOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);return new Notification.Builder(this,"ride").setSmallIcon(R.drawable.ic_launcher).setContentTitle("MOTO HUB · GPS 기록 중").setContentText(text).setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true).addAction(new Notification.Action.Builder(null,"기록 종료",stop).build()).build();}
 @Override public void onLocationChanged(Location location){
  if(!running||output==null)return;if(checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){stopWith("위치 권한이 해제되어 기록을 중지했습니다.",false);return;}long age=SystemClock.elapsedRealtimeNanos()-location.getElapsedRealtimeNanos();
  if(location.isFromMockProvider()||age<0||age>30_000_000_000L||!location.hasAccuracy()||!TrackFile.valid(location.getLatitude(),location.getLongitude(),location.getAccuracy(),location.getTime())||location.getTime()<=lastTime)return;
  try{if(track.length()>TrackFile.MAX_BYTES-512)throw new IOException("경로 최대 크기에 도달했습니다.");output.write(TrackFile.point(location.getLatitude(),location.getLongitude(),location.getAccuracy(),location.getTime()).getBytes(StandardCharsets.UTF_8));output.flush();output.getFD().sync();lastTime=location.getTime();count++;
   if(lastGood!=null){long dt=location.getTime()-lastGood.getTime();float metres=location.distanceTo(lastGood);
    if(dt>0&&dt<=600000){double measuredKmh=metres*3600.0/dt;
     if(measuredKmh<=250){liveDistanceKm+=metres/1000.0;liveSpeedKmh=measuredKmh;}
     else liveSpeedKmh=0;
    }else liveSpeedKmh=0;
   }
   lastGood=new Location(location);Sharing.offer(location);status=count+"개 좌표 · 마지막 "+java.time.Instant.ofEpochMilli(lastTime);getSystemService(NotificationManager.class).notify(404,notification(count+"개 좌표 저장 · "+(Sharing.enabled?"그룹 위치 공유 켜짐":"위치 공유 꺼짐")));}catch(Exception e){stopWith("저장 오류로 기록 중지: "+e.getMessage(),false);}
 }
 @Override public void onProviderDisabled(String provider){if(LocationManager.GPS_PROVIDER.equals(provider))stopWith("GPS가 꺼져 기록을 중지했습니다.",false);}
 @Override public void onProviderEnabled(String p){} @Override public void onStatusChanged(String p,int s,Bundle b){}
 void stopWith(String message,boolean completed){status=message;cleanup(completed);stopSelf();}
 void cleanup(boolean completed){Sharing.stop();running=false;handler.removeCallbacksAndMessages(null);if(manager!=null)try{manager.removeUpdates(this);}catch(Exception ignored){}if(output!=null){try{output.close();if(completed&&track!=null)new File(track.getPath()+".done").createNewFile();}catch(IOException ignored){}output=null;}active="";stopForeground(STOP_FOREGROUND_REMOVE);}
 @Override public void onDestroy(){if(running)status="GPS 서비스가 중단됐습니다. 저장된 좌표는 복구할 수 있어요.";cleanup(false);instance=null;super.onDestroy();}
}
