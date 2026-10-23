package com.motohub.next;
import android.app.*;import android.content.*;import android.content.pm.PackageManager;import android.content.pm.ServiceInfo;import android.location.*;import android.os.*;import java.io.*;import java.nio.charset.StandardCharsets;import java.util.UUID;import android.bluetooth.*;
/** User-started location FGS. Local recording with explicit group sharing. No boot restart, network or mock positions. */
public final class RideService extends Service implements LocationListener {
 static final String STOP="com.motohub.next.STOP_RIDE",AUTO_ARM="com.motohub.next.AUTO_RIDE_ARM",AUTO_DISARM="com.motohub.next.AUTO_RIDE_DISARM",EXTRA_DEVICE="device_address",EXTRA_BIKE_ID="ride_bike_id";static volatile boolean autoArmed=false;static volatile boolean running=false;static volatile String status="기록 꺼짐",active="";static volatile int count=0;static volatile long startedAt=0,startedElapsed=0;static volatile double liveDistanceKm=0,liveSpeedKmh=0,livePeakKmh=0;
 // Volatile display data only: neither ECU samples nor Bluetooth address are uploaded/stored.
 static volatile int obdSpeedKmh=-1,obdReconnects=0;
 static volatile long obdSpeedSamples=0; // only current, explicitly enabled connection session
 static volatile double obdRpm=Double.NaN,obdCoolantC=Double.NaN,obdVoltage=Double.NaN;
 static volatile String obdLink="연결 안 됨";
 static volatile long obdLastSampleElapsed=0;
 static volatile long obdRpmAt=0,obdCoolantAt=0,obdVoltageAt=0;
 static volatile String obdLastError="없음";
 static volatile String obdPidSupport="지원 PID 확인 전 · ODO는 미검증";
 static volatile String obdAuxDiagnostics="센서 수신 기록 없음";
 static volatile long obdStationaryRemainingMs=-1;
 static String engineEstimate(){return ObdEngineState.label(obdRpm,obdRpmAt,SystemClock.elapsedRealtime());}
 static volatile boolean gpsOnlyAfterObd=false;
 static volatile RideService instance;
 @Override public void onCreate(){super.onCreate();instance=this;
  // GPS disk sync and callbacks live off the UI looper, preserving location callback order.
  gpsIoThread=new HandlerThread("moto-gps-disk",android.os.Process.THREAD_PRIORITY_BACKGROUND);
  gpsIoThread.start();
 }
 /** Read current service-owned finalization state without leaking it across service instances. */
 static boolean saving(){RideService s=instance;return s!=null&&(s.finishPending||s.autoFinishPending);}
 static boolean shareAllowed(){RideService s=instance;return s!=null&&running&&s.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED&&(Build.VERSION.SDK_INT<33||s.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED)&&s.getSystemService(NotificationManager.class).areNotificationsEnabled();}
 static void refreshNotification(){RideService s=instance;if(s!=null)s.handler.post(()->{if(running)s.getSystemService(NotificationManager.class).notify(404,s.notification(String.format(java.util.Locale.KOREA,"%.0f km/h · %.2f km · ",liveSpeedKmh,liveDistanceKm)+(Sharing.enabled?"그룹 공유 켜짐":"공유 꺼짐")));});}
 final RideNotificationRate notificationRate=new RideNotificationRate();
 final RideNotificationRate autoNotificationRate=new RideNotificationRate();
 String pendingBikeId="";ObdAutoSpeedMonitor obdMonitor;final ObdRideAutoGate autoGate=new ObdRideAutoGate();LocationManager manager;FileOutputStream output;File track;long lastTime;volatile long lastFixElapsed;volatile Location lastGood;final Handler handler=new Handler(Looper.getMainLooper());
 HandlerThread gpsIoThread;
 volatile boolean finishPending=false,autoFinishPending=false;
 final Runnable permissionCheck=new Runnable(){public void run(){if(!running&&!autoArmed)return;if(autoArmed&&!getSharedPreferences("developer_experiments",0).getBoolean("enabled",false)){
    // Turning off developer experiments revokes OBD permission, not consent to a
    // separately running GPS ride. The GPS track must stay recoverable/active.
    ObdAutoSpeedMonitor monitor=obdMonitor;
    if(monitor!=null)monitor.stop();
    onObdDisconnected("개발자 모드가 종료되어 자동 감지를 중지했습니다.");
    if(!running)return;
   }if(checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){stopWith("위치 권한이 해제되어 기록을 중지했습니다.",false);return;}if(autoArmed&&(manager==null||!manager.isProviderEnabled(LocationManager.GPS_PROVIDER))){
     // A GPS toggle must never close a track already being written by an OBD-
     // armed session. Stop only automatic detection; preserve manual GPS intent.
     if(GpsProviderLossPolicy.onLoss(running,autoArmed)==GpsProviderLossPolicy.Action.KEEP_ACTIVE_TRACK){
      ObdAutoSpeedMonitor monitor=obdMonitor;
      if(monitor!=null)monitor.stop();
      onObdDisconnected("GPS가 꺼져 자동 감지를 중지했습니다.");
      status="GPS 꺼짐 · 기존 라이딩 보존 · 다시 켜면 기록 재개";
      NotificationManager notifications=getSystemService(NotificationManager.class);
      if(notifications!=null)notifications.notify(404,notification(status));
     }else{stopWith("GPS가 꺼져 자동 출발 감지를 중지했습니다.",false);return;}
    }long now=SystemClock.elapsedRealtime();
   if(autoArmed){
    if(!ObdTelemetryFreshness.speedFresh(obdLastSampleElapsed,now))obdSpeedKmh=-1;
    if(!ObdTelemetryFreshness.fresh(obdRpmAt,now))obdRpm=Double.NaN;
    if(!ObdTelemetryFreshness.fresh(obdCoolantAt,now))obdCoolantC=Double.NaN;
    if(!ObdTelemetryFreshness.fresh(obdVoltageAt,now))obdVoltage=Double.NaN;
   }
   // The watchdog and GPS I/O worker must not race when resetting the last valid fix.
   boolean gpsLost=false;
   synchronized(RideService.this){
    if(lastFixElapsed>0&&now-lastFixElapsed>35_000){
     liveSpeedKmh=0;lastGood=null;lastFixElapsed=0;gpsLost=true;
    }
   }
   if(gpsLost){
    if(Sharing.enabled)Sharing.stop();
    status="GPS 신호 대기 · 기록 유지 · 위치 공유 중지";
    getSystemService(NotificationManager.class).notify(404,notification(status));
   }
   if(Sharing.enabled&&((Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)||!getSystemService(NotificationManager.class).areNotificationsEnabled()))Sharing.stop();handler.postDelayed(this,5000);}};
 @Override public IBinder onBind(Intent i){return null;}
 @Override public int onStartCommand(Intent intent,int flags,int id){
  if(intent==null){stopWith("GPS 서비스가 중단됐습니다.",false);return START_NOT_STICKY;}
  String action=intent.getAction();
  if(STOP.equals(action)){
   finishUserRideAsync();return START_NOT_STICKY;
  }
  if(AUTO_DISARM.equals(action)){
   // Disarming the experimental reader must NOT silently discard an active GPS ride.
   if(autoArmed){
    ObdAutoSpeedMonitor monitor=obdMonitor;
    if(monitor!=null)monitor.stop();
    onObdDisconnected("사용자가 자동 감지를 중지했습니다.");
   }else if(!RideSessionInterlock.keepExistingSession(running,autoArmed))stopSelf();
   return START_NOT_STICKY;
  }
  if(!RestoreRideGate.beginRideStart()){
   // A rejected duplicate start must never stop an already-running foreground
   // ride: Android can deliver a second start intent while a restore is pending.
   if(!RideSessionInterlock.keepExistingSession(running,autoArmed)){
    status="백업 또는 복원 중에는 라이딩을 시작할 수 없습니다.";
    stopSelf();
   }
   return START_NOT_STICKY;
  }
  try{
   if(AUTO_ARM.equals(action)){
    if(running||autoArmed)return START_NOT_STICKY;
    pendingBikeId=intent.getStringExtra(EXTRA_BIKE_ID);
    armAutomaticRide(intent.getStringExtra(EXTRA_DEVICE));return START_NOT_STICKY;
   }
   if(running||autoArmed)return START_NOT_STICKY;
   pendingBikeId=intent.getStringExtra(EXTRA_BIKE_ID);
   try{
    prepareForeground(false);
    openRideTrack();
    requestGpsUpdatesChecked();
    handler.postDelayed(permissionCheck,5000);
   }catch(Exception e){stopWith(e.getMessage()==null?"GPS 시작 실패":e.getMessage(),false);}
   return START_NOT_STICKY;
  }finally{RestoreRideGate.endRideStart();}
 }
 /** Permission can be revoked after foreground-start; check immediately before registration. */
 private void requestGpsUpdatesChecked(){
  if(checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)
   throw new SecurityException("위치 권한이 해제되어 GPS를 시작할 수 없습니다.");
  if(manager==null)throw new IllegalStateException("GPS 서비스가 준비되지 않았습니다.");
  if(gpsIoThread==null||!gpsIoThread.isAlive())throw new IllegalStateException("GPS 기록 스레드를 시작할 수 없습니다.");
  manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,5000,0,this,gpsIoThread.getLooper());
 }
 private void prepareForeground(boolean automatic){
  if(checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)throw new SecurityException("정확한 위치 권한이 필요합니다.");
  if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)throw new SecurityException("알림 권한이 필요합니다.");
  manager=getSystemService(LocationManager.class);
  if(manager==null||!manager.isProviderEnabled(LocationManager.GPS_PROVIDER))throw new IllegalStateException("기기의 GPS를 켜 주세요.");
  NotificationManager nm=getSystemService(NotificationManager.class);
  if(!nm.areNotificationsEnabled())throw new IllegalStateException("앱 알림을 허용해 주세요.");
  nm.createNotificationChannel(new NotificationChannel("ride","라이딩 GPS 기록",NotificationManager.IMPORTANCE_LOW));
  Notification n=notification(automatic?"OBD2 자동 기록 대기 · GPS 아직 저장 안 함":"GPS 신호 대기 · 위치 공유 꺼짐");
  if(Build.VERSION.SDK_INT>=34&&automatic)startForeground(404,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION|ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
  else if(Build.VERSION.SDK_INT>=29)startForeground(404,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
  else startForeground(404,n);
 }
 private synchronized void openRideTrack()throws Exception{
  track=new File(TrackFile.directory(getFilesDir()),System.currentTimeMillis()+"-"+UUID.randomUUID()+".track");
  output=new FileOutputStream(track);
  try{if(RideBikeBinding.valid(pendingBikeId))RideBikeBinding.write(track,pendingBikeId);}
  catch(IOException error){output.close();output=null;track.delete();throw error;}
  count=0;lastTime=0;lastFixElapsed=0;lastGood=null;
  startedAt=System.currentTimeMillis();startedElapsed=SystemClock.elapsedRealtime();liveDistanceKm=0;liveSpeedKmh=0;livePeakKmh=0;gpsOnlyAfterObd=false;
  active=track.getName();running=true;notificationRate.reset();status="GPS 신호 대기";
 }
 private void armAutomaticRide(String address){
  try{
   if(!getSharedPreferences("developer_experiments",0).getBoolean("enabled",false))throw new SecurityException("개발자 모드를 활성화해 주세요.");
   if(address==null||!BluetoothAdapter.checkBluetoothAddress(address))throw new IllegalArgumentException("OBD2 어댑터를 다시 선택해 주세요.");
   if(Build.VERSION.SDK_INT>=31&&checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)throw new SecurityException("Bluetooth 연결 권한이 필요합니다.");
   BluetoothManager bluetooth=getSystemService(BluetoothManager.class);
   BluetoothAdapter adapter=bluetooth==null?null:bluetooth.getAdapter();
   if(adapter==null||!adapter.isEnabled())throw new IllegalStateException("Bluetooth를 켜 주세요.");
   BluetoothDevice paired=null;
   for(BluetoothDevice d:adapter.getBondedDevices())if(address.equals(d.getAddress())){paired=d;break;}
   if(paired==null)throw new SecurityException("이미 페어링된 OBD2 장치만 사용할 수 있습니다.");
   Sharing.stop(); // Automatic local rides must never inherit a group sharing session.
   prepareForeground(true);autoGate.reset();autoNotificationRate.reset();
   autoGate.setStopAfterMs(getSharedPreferences("developer_experiments",0).getLong("obd_stop_after_ms",ObdRideAutoGate.STOP_AFTER_MS));
   autoArmed=true;status="OBD2 연결 대기 · 자동 라이딩 감지";
   obdSpeedKmh=-1;obdRpm=Double.NaN;obdCoolantC=Double.NaN;obdVoltage=Double.NaN;
   obdReconnects=0;obdSpeedSamples=0;obdLink="OBD2 연결 중";obdLastSampleElapsed=0;
   obdPidSupport="표준 지원 PID 확인 중 · ODO 미검증";
   obdAuxDiagnostics="센서 수신 기록 없음";obdStationaryRemainingMs=-1;
   obdRpmAt=0;obdCoolantAt=0;obdVoltageAt=0;obdLastError="없음";
   final ObdAutoSpeedMonitor[] ref=new ObdAutoSpeedMonitor[1];
   ObdAutoSpeedMonitor reader=new ObdAutoSpeedMonitor(paired,new ObdAutoSpeedMonitor.Listener(){
    @Override public void speed(int kmh){handler.post(()->{if(autoArmed&&obdMonitor==ref[0]){
     obdSpeedKmh=kmh;obdSpeedSamples++;obdLastSampleElapsed=SystemClock.elapsedRealtime();handleAutoSpeed(kmh);
    }});}
    @Override public void telemetry(String pid,double value){handler.post(()->{if(autoArmed&&obdMonitor==ref[0]){
     long now=SystemClock.elapsedRealtime();
     if("010C".equals(pid)){obdRpm=value;obdRpmAt=now;}
     else if("0105".equals(pid)){obdCoolantC=value;obdCoolantAt=now;}
     else if("0142".equals(pid)){obdVoltage=value;obdVoltageAt=now;}
    }});}
    @Override public void capabilities(ObdPidCapabilities capability){handler.post(()->{
     if(autoArmed&&obdMonitor==ref[0])obdPidSupport=capability.summary();
    });}
    @Override public void auxiliaryDiagnostics(String summary){handler.post(()->{
     if(autoArmed&&obdMonitor==ref[0])obdAuxDiagnostics=summary;
    });}
    @Override public void connected(){handler.post(()->{if(autoArmed&&obdMonitor==ref[0]){
     obdLink="연결됨 · 차량 데이터 읽기 전용";
     if(!running)status="OBD2 연결됨 · 출발 대기";
    }});}
    @Override public void reconnecting(int attempt,int max){handler.post(()->{if(autoArmed&&obdMonitor==ref[0]){
     obdLink="연결 재시도 "+attempt+"/"+max;
     obdReconnects=attempt;obdSpeedKmh=-1;obdLastSampleElapsed=0;
     obdRpmAt=0;obdCoolantAt=0;obdVoltageAt=0;
     obdRpm=Double.NaN;obdCoolantC=Double.NaN;obdVoltage=Double.NaN;
     autoGate.connectionLost();obdStationaryRemainingMs=-1;liveSpeedKmh=0;
     status="OBD2 신호 손실 · "+obdLink+(running?" · GPS 기록 유지":"");
     getSystemService(NotificationManager.class).notify(404,notification(status));
    }});}
    @Override public void disconnected(String reason){handler.post(()->{if(autoArmed&&obdMonitor==ref[0])onObdDisconnected(reason);});}
   });
   ref[0]=reader;obdMonitor=reader;
   reader.start();handler.postDelayed(permissionCheck,5000);
  }catch(Exception e){stopWith("자동 기록 시작 실패: "+(e.getMessage()==null?"연결을 확인해 주세요.":e.getMessage()),false);}
 }
 private void handleAutoSpeed(int speedKmh){
  if(!autoArmed||autoFinishPending||finishPending)return;
  long now=SystemClock.elapsedRealtime();
  ObdRideAutoGate.Event event=autoGate.accept(speedKmh,now);
  obdStationaryRemainingMs=autoGate.stationaryRemainingMs(now);
  if(event==ObdRideAutoGate.Event.START){
   try{openRideTrack();requestGpsUpdatesChecked();
    status="OBD2 출발 감지 · GPS 기록 중";}
   catch(Exception e){stopWith("GPS 자동 기록 시작 실패: "+e.getMessage(),false);return;}
  }else if(event==ObdRideAutoGate.Event.FINISH){finishAutoTrack();}
  // Speed samples arrive faster than the user needs notification updates. Avoid
  // rebuilding the notification every second while KakaoNavi is in foreground.
  // START and FINISH remain immediate; GPS samples and files are never throttled.
  boolean notificationDue=autoNotificationRate.shouldNotify(SystemClock.elapsedRealtime());
  if(autoArmed&&(notificationDue||event!=ObdRideAutoGate.Event.NONE)){
   String text=running?(obdStationaryRemainingMs>=0
    ?ObdRestStatus.label(true,true,obdStationaryRemainingMs)+" · GPS 기록 유지"
    :"자동 기록 중 · OBD2 "+speedKmh+" km/h · GPS "+String.format(java.util.Locale.KOREA,"%.2f km",liveDistanceKm))
    :"OBD2 연결됨 · 출발 대기 · 위치 공유 꺼짐";
   getSystemService(NotificationManager.class).notify(404,notification(text));
  }
 }
 /** A full-track readback can scan 25MB: keep STOP responsive on Android main.
  *  Location callbacks and this close operation run FIFO on the same GPS worker.
  *  Never stop the service until the verified marker or recoverable original is settled.
  */
 private void finishUserRideAsync(){
  if(finishPending)return;
  if(autoFinishPending){
   // A user STOP during automatic file verification means stop the OBD session too.
   finishPending=true;Sharing.stop();return;
  }
  if(!running){stopWith("라이딩 기록이 종료됐습니다.",false);return;}
  finishPending=true;
  Sharing.stop();
  if(manager!=null)try{manager.removeUpdates(this);}catch(Exception ignored){}
  status="GPS 기록 저장·검증 중…";
  HandlerThread worker=gpsIoThread;
  if(worker==null||!worker.isAlive()||!new Handler(worker.getLooper()).post(()->{
   boolean verified=closeTrack(true);
   handler.post(()->{
    if(!finishPending)return;finishPending=false;
    stopWith(verified?"사용자가 라이딩 기록을 저장하고 종료했습니다.":"GPS 기록 검증 실패 · 복구 화면에서 원본을 확인하세요.",false);
    if(verified)DailyMaintenanceJob.checkSoon(this);
   });
  })){
   finishPending=false;
   stopWith("사용자가 라이딩 기록을 종료했습니다.",true);
  }
 }
 /** Auto rides use the same ordered GPS worker for full-file verification.
  *  Keep OBD autoGate on the main thread but do not allow a new start mid-finalize.
  */
 private void finishAutoTrack(){
  if(!running||autoFinishPending)return;
  autoFinishPending=true;
  Sharing.stop();
  if(manager!=null)try{manager.removeUpdates(this);}catch(Exception ignored){}
  final File savedTrack=track;
  status="자동 라이딩 GPS 기록 저장·검증 중…";
  HandlerThread worker=gpsIoThread;
  if(worker==null||!worker.isAlive()||!new Handler(worker.getLooper()).post(()->{
   boolean verified=closeTrack(true);
   handler.post(()->{if(autoFinishPending)completeAutoTrack(savedTrack,verified);});
  })){
   completeAutoTrack(savedTrack,closeTrack(true));
  }
 }
 private void completeAutoTrack(File savedTrack,boolean verified){
  autoFinishPending=false;running=false;
  if(finishPending){
   finishPending=false;
   stopWith(verified?"자동 라이딩을 저장하고 종료했습니다.":"GPS 저장 검증 실패 · 원본 복구가 필요합니다.",false);
   if(verified)DailyMaintenanceJob.checkSoon(this);
   return;
  }
  // OBD can disconnect or developer access can be revoked while the GPS worker
  // verifies the final track. A disarmed session must not leave a foreground
  // service running forever with neither GPS recording nor an OBD reader.
  if(!autoArmed){
   stopWith(verified?"OBD2 연결 종료 · GPS 기록 저장 완료":"OBD2 연결 종료 · GPS 기록 원본 복구 필요",false);
   if(verified)DailyMaintenanceJob.checkSoon(this);
   return;
  }
  status=verified?"자동 라이딩 저장 완료 · 다음 출발 감지 대기":"자동 라이딩 저장 검증 실패 · 기록을 복구 화면에서 확인해 주세요";
  if(verified&&savedTrack!=null&&savedTrack.isFile()&&count>=2){
   new Thread(()->{try{RideInsights.Result summary=RideInsights.analyze(savedTrack);
    handler.post(()->{if(autoArmed&&!running){status=String.format(java.util.Locale.KOREA,
     "라이딩 저장 · %.1f km · %.0f km/h 최고 · 다시 출발 대기",summary.distanceKm,summary.peakKmh);
     getSystemService(NotificationManager.class).notify(404,notification(status));}});
   }catch(Exception ignored){}},"moto-auto-ride-summary").start();
  }
  if(verified)DailyMaintenanceJob.checkSoon(this);
 }
 private void onObdDisconnected(String reason){
  // No guessing: do not finish or split a moving ride when Bluetooth fails.
  // Keep an already-running GPS session in the same foreground service until its
  // user explicitly ends it. Never auto-arm again or enable group sharing.
  obdLastError="OBD2 · "+reason;
  obdLink="연결 종료 · "+reason;obdSpeedKmh=-1;obdLastSampleElapsed=0;
  obdRpm=Double.NaN;obdCoolantC=Double.NaN;obdVoltage=Double.NaN;
  obdRpmAt=0;obdCoolantAt=0;obdVoltageAt=0;
  autoArmed=false;autoGate.reset();obdStationaryRemainingMs=-1;obdMonitor=null;
  Sharing.stop();
  if(autoFinishPending){
   // Completion owns the same GPS worker. Let its callback stop the service
   // after the existing .track is verified, rather than closing it mid-write.
   status="OBD2 연결 종료 · GPS 저장 검증 중…";
   return;
  }
  if(running){
   gpsOnlyAfterObd=true;
   status="OBD2 연결 종료 · GPS 단독 기록 유지 · 알림에서 종료 가능";
   getSystemService(NotificationManager.class).notify(404,notification(status));
  }else stopWith("OBD2 자동 감지 종료 · "+reason,false);
 }
 Notification notification(String text){
  PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,RideService.class).setAction(STOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification.Builder builder=new Notification.Builder(this,"ride").setSmallIcon(R.drawable.ic_launcher)
    .setContentTitle(autoArmed?"MOTO HUB · OBD2 자동 라이딩":"MOTO HUB · GPS 기록 중")
    .setContentText(text).setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true).setVisibility(Notification.VISIBILITY_PRIVATE)
    .addAction(new Notification.Action.Builder(null,"기록 종료",stop).build());
  if(autoArmed){
   PendingIntent disarm=PendingIntent.getService(this,3,new Intent(this,RideService.class).setAction(AUTO_DISARM),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   builder.addAction(new Notification.Action.Builder(null,"OBD 감지만 중지",disarm).build());
  }
  Intent navi=getPackageManager().getLaunchIntentForPackage(KakaoNaviLaunch.PACKAGE);
  if(navi!=null){
   PendingIntent nav=PendingIntent.getActivity(this,2,navi,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   builder.addAction(new Notification.Action.Builder(null,"카카오내비",nav).build());
  }
  return builder.build();
 }
 @Override public synchronized void onLocationChanged(Location location){
  if(!running||output==null)return;if(checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){stopWith("위치 권한이 해제되어 기록을 중지했습니다.",false);return;}long age=SystemClock.elapsedRealtimeNanos()-location.getElapsedRealtimeNanos();
  if(location.isFromMockProvider()||age<0||age>30_000_000_000L||!location.hasAccuracy()||!TrackFile.valid(location.getLatitude(),location.getLongitude(),location.getAccuracy(),location.getTime())||!GpsFixPolicy.acceptTime(location.getTime(),System.currentTimeMillis(),lastTime))return;
  if(lastGood!=null){long delta=location.getTime()-lastGood.getTime();if(delta>0&&delta<=RideMotionGate.MAX_CONTINUOUS_FIX_MS&&location.distanceTo(lastGood)*3600.0/delta>250)return;} // Discard implausible continuous GPS spikes; keep first point after a gap.
  try{if(track.length()>TrackFile.MAX_BYTES-512)throw new IOException("경로 최대 크기에 도달했습니다.");output.write(TrackFile.point(location.getLatitude(),location.getLongitude(),location.getAccuracy(),location.getTime()).getBytes(StandardCharsets.UTF_8));output.flush();output.getFD().sync();lastTime=location.getTime();lastFixElapsed=SystemClock.elapsedRealtime();count++;
   if(lastGood!=null){long dt=location.getTime()-lastGood.getTime();float metres=location.distanceTo(lastGood);
    if(RideMotionGate.countDistance(dt,metres,lastGood.getAccuracy(),location.getAccuracy())){
     double measuredKmh=metres*3600.0/dt;
     liveDistanceKm+=metres/1000.0;liveSpeedKmh=measuredKmh;
     if(autoArmed&&location.getAccuracy()<=20){final double speedForGate=measuredKmh;
      // OBD connection callbacks own the autoGate on the main looper. Serialize GPS hints there.
      handler.post(()->{if(autoArmed&&running){autoGate.gpsMovement(speedForGate,SystemClock.elapsedRealtime());obdStationaryRemainingMs=autoGate.stationaryRemainingMs(SystemClock.elapsedRealtime());}});
     }
     livePeakKmh=Math.max(livePeakKmh,measuredKmh);
    }else liveSpeedKmh=0; // A long GPS gap must never be treated as travelled distance.
   }
   lastGood=new Location(location);
   status=count+"개 좌표 · 마지막 "+java.time.Instant.ofEpochMilli(lastTime)+(location.getAccuracy()>40?" · 정확도 낮음":"");
  }catch(Exception e){
   // Only a real GPS file/track calculation failure can stop local recording.
   stopWith("저장 오류로 기록 중지: "+e.getMessage(),false);
   return;
  }
  // Neither optional group transmission nor the notification manager may
  // invalidate an already synced local track or stop this ride.
  RidePostCommit.afterWrite(
   ()->{if(!autoArmed&&location.getAccuracy()<=40)Sharing.offer(location);},
   ()->{Sharing.stop();status="그룹 공유 실패 · 로컬 GPS 기록 유지";},
   ()->{
    if(notificationRate.shouldNotify(SystemClock.elapsedRealtime())){
     NotificationManager notifications=getSystemService(NotificationManager.class);
     if(notifications!=null)notifications.notify(404,notification(
      String.format(java.util.Locale.KOREA,"%.0f km/h · %.2f km · ",liveSpeedKmh,liveDistanceKm)+
      (Sharing.enabled?"그룹 위치 공유 켜짐":(gpsOnlyAfterObd?"OBD2 종료 · GPS 단독 기록":"위치 공유 꺼짐"))));
    }
   }
  );
 }
 @Override public void onProviderDisabled(String provider){if(LocationManager.GPS_PROVIDER.equals(provider)){
  // Security-critical: disable remote sharing synchronously, before any UI message is queued.
  Sharing.stop();
  handler.post(()->{if(!running)return;
   synchronized(RideService.this){liveSpeedKmh=0;lastGood=null;lastFixElapsed=0;}
   status="GPS 일시 중단 · 켜면 기록 재개 (그룹 공유 중지)";getSystemService(NotificationManager.class).notify(404,notification(status));
  });
 }}
 @Override public void onProviderEnabled(String p){if(LocationManager.GPS_PROVIDER.equals(p))handler.post(()->{if(!running)return;status="GPS 복귀 · 좌표 대기";getSystemService(NotificationManager.class).notify(404,notification(status));});} @Override public void onStatusChanged(String p,int s,Bundle b){}
 void stopWith(String message,boolean completed){status=message;cleanup(completed);stopSelf();}
 private synchronized boolean closeTrack(boolean completed){
  boolean verified=false;
  if(output!=null){
   try{
    // A successful close alone does not imply a valid, complete GPS track.
    output.getFD().sync();output.close();output=null;
    if(completed&&count>0&&track!=null){TrackCompletion.mark(track,count);verified=true;}
    else if(count==0&&track!=null)TrackFile.discardEmptySession(track);
   }catch(Exception e){
    // Keep any nonempty original .track without a .done marker for recovery.
    status="GPS 저장 검증 실패 · 원본 기록을 복구 화면에서 확인해 주세요";
    try{output.close();}catch(Exception ignored){}
   }finally{output=null;}
  }
  active="";lastGood=null;lastFixElapsed=0;gpsOnlyAfterObd=false;
  return verified;
 }
 synchronized void cleanup(boolean completed){
  // Invalidate queued main-thread finalization callbacks after failures or process teardown.
  finishPending=false;autoFinishPending=false;
  Sharing.stop();autoArmed=false;obdSpeedKmh=-1;obdLastSampleElapsed=0;obdLink="연결 안 됨";
  obdStationaryRemainingMs=-1;obdAuxDiagnostics="센서 수신 기록 없음";
  obdRpm=Double.NaN;obdCoolantC=Double.NaN;obdVoltage=Double.NaN;
  obdRpmAt=0;obdCoolantAt=0;obdVoltageAt=0;
  if(obdMonitor!=null){obdMonitor.stop();obdMonitor=null;}
  autoGate.reset();running=false;handler.removeCallbacksAndMessages(null);
  if(manager!=null)try{manager.removeUpdates(this);}catch(Exception ignored){}
  boolean verified=closeTrack(completed);
  if(completed&&!verified&&count>0)status="GPS 기록 검증 실패 · 복구 화면에서 원본 기록을 확인해 주세요";
  stopForeground(STOP_FOREGROUND_REMOVE);
  if(completed&&verified)DailyMaintenanceJob.checkSoon(this);
 }
 @Override public void onDestroy(){if(running||autoArmed)status="라이딩 서비스가 중단됐습니다. 저장된 GPS 좌표는 복구할 수 있어요.";
  cleanup(false);
  if(gpsIoThread!=null){gpsIoThread.quitSafely();gpsIoThread=null;}
  instance=null;super.onDestroy();}
}
