package com.motohub.next;
import android.app.*;import android.content.*;import android.content.pm.PackageManager;import android.content.pm.ServiceInfo;import android.location.*;import android.os.*;import java.io.*;import java.nio.charset.StandardCharsets;import java.util.UUID;import android.bluetooth.*;
/** User-started location FGS. Local recording with explicit group sharing. No boot restart, network or mock positions. */
public final class RideService extends Service implements LocationListener {
 static final String STOP="com.motohub.next.STOP_RIDE",AUTO_ARM="com.motohub.next.AUTO_RIDE_ARM",AUTO_DISARM="com.motohub.next.AUTO_RIDE_DISARM",EXTRA_DEVICE="device_address",EXTRA_BIKE_ID="ride_bike_id";static volatile boolean autoArmed=false;static volatile boolean running=false;static volatile String status="기록 꺼짐",active="";static volatile int count=0;static volatile long startedAt=0,startedElapsed=0,lastMotionElapsed=0;static volatile boolean stopSuggestionShown=false;static volatile double liveDistanceKm=0,liveSpeedKmh=0,livePeakKmh=0;static volatile long liveMovingMillis=0,liveStationaryMillis=0,lastWidgetRefreshElapsed=0;
 // Volatile display data only: neither ECU samples nor Bluetooth address are uploaded/stored.
 static volatile int obdSpeedKmh=-1,obdReconnects=0;
 static volatile long obdAverageLatencyMs=-1;
 static volatile int obdQueryFailures=0,obdConnectionAttempts=0,obdConnectionSuccesses=0;
 static volatile long obdSpeedSamples=0; // only current, explicitly enabled connection session
 static volatile double obdRpm=Double.NaN,obdCoolantC=Double.NaN,obdVoltage=Double.NaN,obdFuelPercent=Double.NaN;
 static volatile String obdLink="연결 안 됨";
 static volatile long obdLastSampleElapsed=0;
 static volatile long obdRpmAt=0,obdCoolantAt=0,obdVoltageAt=0,obdFuelAt=0;
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
  gpsWriteHandler=new Handler(gpsIoThread.getLooper());
 }
 /** Read current service-owned finalization state without leaking it across service instances. */
 static boolean saving(){RideService s=instance;return s!=null&&(s.finishPending||s.autoFinishPending);}
 static boolean gpsFresh(){RideService s=instance;
  return s!=null&&running&&GpsSignalStatus.fresh(s.lastFixElapsed,SystemClock.elapsedRealtime());
 }
 static long gpsFixElapsed(){RideService s=instance;return s==null?0:s.lastFixElapsed;}
 static boolean shareAllowed(){
  RideService s=instance;
  return s!=null&&running&&s.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED&&
   (Build.VERSION.SDK_INT<33||s.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED)&&
   s.notificationsAvailable();
 }
 boolean notificationsAvailable(){
  try{
   NotificationManager notifications=getSystemService(NotificationManager.class);
   return notifications!=null&&notifications.areNotificationsEnabled();
  }catch(RuntimeException unavailable){return false;}
 }
 /** Noncritical notification failures must not stop a valid local GPS track. */
 void notifySafely(String message){
  try{
   NotificationManager notifications=getSystemService(NotificationManager.class);
   if(notifications!=null)notifications.notify(404,notification(message));
  }catch(RuntimeException ignored){}
 }
 /** All long-idle checks also run while TMAP or NAVER is foregrounded. */
 void maybeNotifyStationary(long now){
  if(!running||stopSuggestionShown||!notificationsAvailable())return;
  if(!RideStopSuggestion.shouldSuggest(now,startedElapsed,lastMotionElapsed,gpsFresh(),
      liveDistanceKm,liveSpeedKmh,stopSuggestionShown))return;
  NotificationManager manager=getSystemService(NotificationManager.class);
  if(manager==null)return;
  try{
   manager.createNotificationChannel(new NotificationChannel("ride_hints","라이딩 종료 확인",NotificationManager.IMPORTANCE_DEFAULT));
   Intent viewRide=new Intent(this,MainActivity.class);
   viewRide.setAction("com.motohub.next.STATIONARY_REVIEW");
   viewRide.putExtra(MotoWidgetProvider.EXTRA_TAB,"라이딩");
   viewRide.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
   PendingIntent open=PendingIntent.getActivity(this,405,viewRide,
        PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   Notification note=new Notification.Builder(this,"ride_hints")
    .setSmallIcon(R.drawable.ic_notification)
    .setContentTitle("라이딩이 잠시 멈춘 것 같아요")
    .setContentText("12분 이상 정차 중 · 기록을 계속할지 확인해 주세요.")
    .setContentIntent(open).setAutoCancel(true)
    .setVisibility(Notification.VISIBILITY_PRIVATE).build();
   manager.notify(405,note);
   stopSuggestionShown=true; // Offer only once per ride, never silently stop.
  }catch(RuntimeException ignored){ /* A notification failure must not stop GPS. */ }
 }
 static void refreshNotification(){
  RideService service=instance;
  if(service!=null)service.handler.post(()->{
   if(running||autoArmed)service.notifySafely(running?
    String.format(java.util.Locale.KOREA,"%.0f km/h · %.2f km · ",liveSpeedKmh,liveDistanceKm)+
     (Sharing.enabled?"그룹 공유 켜짐":"공유 꺼짐"):
    "OBD2 출발 감지 대기 · GPS 기록 시작 전");
  });
 }
 final RideNotificationRate notificationRate=new RideNotificationRate();
 final RideNotificationRate autoNotificationRate=new RideNotificationRate();
 String pendingBikeId="";ObdAutoSpeedMonitor obdMonitor;BluetoothDevice selectedAutoDevice;
 int obdReconnectRounds=0,obdValidSamplesThisConnection=0;
 boolean autoGpsPaused=false; // Foreground monitor is suspended, not permanently disarmed.
 final Runnable reconnectObd=new Runnable(){@Override public void run(){
  if(!autoArmed||autoGpsPaused||selectedAutoDevice==null||obdMonitor!=null)return;
  if(manager==null||!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
   pauseObdForGpsOff();return;
  }
  if(saving()){handler.postDelayed(this,15_000L);return;}
  BluetoothManager manager=getSystemService(BluetoothManager.class);
  BluetoothAdapter adapter=manager==null?null:manager.getAdapter();
  if(adapter==null||!adapter.isEnabled()){
   obdLink="Bluetooth 꺼짐 · 자동 재연결 대기";
   // Checking radio state is cheap; do not leave a just-reenabled phone asleep for five minutes.
   handler.postDelayed(this,30_000L);
   notifySafely(obdLink+" · 켜지면 자동 재연결");
   return;
  }
  startAutoMonitor(selectedAutoDevice);
 }};
 final ObdRideAutoGate autoGate=new ObdRideAutoGate();final ObdSpeedSanityPolicy speedSanity=new ObdSpeedSanityPolicy();LocationManager manager;FileOutputStream output;File track;
 /** Original UUID is reused for sequential 25MB sections, so legacy ZIP formats still work. */
 String activeRideSession="";
 int segmentPoints=0,completedSegments=0;
 long lastTime;volatile long lastFixElapsed;volatile Location lastGood;final Handler handler=new Handler(Looper.getMainLooper());
 HandlerThread gpsIoThread;
 Handler gpsWriteHandler;
 boolean gpsSyncScheduled=false;
 long lastStorageCheckElapsed=-60_000L;
 boolean lowStorageWarned=false;
 final GpsSyncPolicy gpsSync=new GpsSyncPolicy();
 final GpsParkedEvidence gpsParkedEvidence=new GpsParkedEvidence();
 final Runnable syncPendingGps=new Runnable(){@Override public void run(){
  synchronized(RideService.this){
   gpsSyncScheduled=false;
   if(output==null||!running||!gpsSync.hasDirty())return;
   try{output.getFD().sync();gpsSync.synced(SystemClock.elapsedRealtime());}
   catch(IOException e){stopWith("GPS 저장 오류 · 경로를 복구 화면에서 확인하세요.",false);}
  }
 }};
 /** Independent of location callbacks: runs every 60s even during GPS signal loss. */
 final Runnable storageWatchdog=new Runnable(){@Override public void run(){
  synchronized(RideService.this){
   if(!running||output==null)return;
   checkRuntimeStorage();
   if(running&&output!=null&&gpsWriteHandler!=null)
    gpsWriteHandler.postDelayed(this,60_000L);
  }
 }};
 volatile boolean finishPending=false,autoFinishPending=false;
 final Runnable permissionCheck=new Runnable(){public void run(){
   if(!running&&!autoArmed)return;
   // OBD auto detection was explicitly armed from the selected bike. It no longer
   // depends on an unrelated developer-mode toggle while the FGS is active.
   if(checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
    // Revocation is a fatal permission loss. Retain recoverable GPS tracks.
    ObdHandsFreeSession.disable(this);
    stopWith("위치 권한이 해제되어 기록을 중지했습니다.",false);return;
   }
   boolean gpsEnabled=manager!=null&&manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
   if(autoArmed&&!gpsEnabled)pauseObdForGpsOff();
   else if(autoArmed&&gpsEnabled&&autoGpsPaused)resumeObdAfterGpsOn();
   long now=SystemClock.elapsedRealtime();
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
    gpsParkedEvidence.reset();
    if(Sharing.enabled)Sharing.stop();
    status="GPS 신호 대기 · 기록 유지 · 위치 공유 중지";
    notifySafely(status);
   }
   if(Sharing.enabled&&((Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)||!notificationsAvailable()))Sharing.stop();
   maybeNotifyStationary(now);
   Location latest=lastGood;
   boolean cleanGps=latest!=null&&latest.hasAccuracy()&&latest.getAccuracy()<=25
     &&GpsSignalStatus.fresh(lastFixElapsed,now);
   if(ObdHandsFreePolicy.finishAfterObdLoss(autoArmed,running,cleanGps,
       ObdTelemetryFreshness.speedFresh(obdLastSampleElapsed,now),count,liveDistanceKm,
       liveSpeedKmh,startedElapsed,lastMotionElapsed,now,autoGate.stopAfterMs())&&
       gpsParkedEvidence.sustained(now,autoGate.stopAfterMs())&&
       !autoFinishPending&&!finishPending){
    autoGate.reset();
    status="OBD 신호 없음 · GPS로 정차 확인 · 자동 저장 중";
    notifySafely(status);
    finishAutoTrack();
   }
   handler.postDelayed(this,5000);}};
 @Override public IBinder onBind(Intent i){return null;}
 @Override public int onStartCommand(Intent intent,int flags,int id){
  if(intent==null){stopWith("GPS 서비스가 중단됐습니다.",false);return START_NOT_STICKY;}
  String action=intent.getAction();
  if(STOP.equals(action)){
   // Explicit STOP also revokes persistent consent even if the service restarted
   // without an active monitor after process death.
   if(autoArmed)disarmAutoByUser("사용자가 자동 라이딩을 종료했습니다.");
   else ObdHandsFreeSession.disable(this);
   if(running||autoFinishPending)finishUserRideAsync();
   else if(!autoArmed)stopSelf();
   return START_NOT_STICKY;
  }
  if(AUTO_DISARM.equals(action)){
   // Explicit disconnect must never rearm the OBD reader behind the user's back.
   if(autoArmed)disarmAutoByUser("사용자가 자동 감지를 중지했습니다.");
   else{
    ObdHandsFreeSession.disable(this);
    if(!RideSessionInterlock.keepExistingSession(running,autoArmed))stopSelf();
   }
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
  // Must be checked here as well as the UI: OBD auto-start has no foreground confirmation screen.
  GpsStorageSpacePolicy.require(getFilesDir());
  activeRideSession=UUID.randomUUID().toString();
  track=new File(TrackFile.directory(getFilesDir()),System.currentTimeMillis()+"-"+activeRideSession+".track");
  output=new FileOutputStream(track);
  try{if(RideBikeBinding.valid(pendingBikeId))RideBikeBinding.write(track,pendingBikeId);}
  catch(IOException error){output.close();output=null;track.delete();throw error;}
  count=0;segmentPoints=0;completedSegments=0;lastTime=0;lastFixElapsed=0;lastGood=null;gpsSync.reset();
  gpsParkedEvidence.reset();
  lastStorageCheckElapsed=-60_000L;lowStorageWarned=false;
  if(gpsWriteHandler!=null)gpsWriteHandler.removeCallbacks(syncPendingGps);gpsSyncScheduled=false;
  startedAt=System.currentTimeMillis();startedElapsed=SystemClock.elapsedRealtime();lastMotionElapsed=startedElapsed;stopSuggestionShown=false;liveDistanceKm=0;liveSpeedKmh=0;livePeakKmh=0;liveMovingMillis=0;liveStationaryMillis=0;lastWidgetRefreshElapsed=0;gpsOnlyAfterObd=false;
  active=track.getName();running=true;notificationRate.reset();status="GPS 신호 대기";
  if(gpsWriteHandler!=null){
   gpsWriteHandler.removeCallbacks(storageWatchdog);
   gpsWriteHandler.postDelayed(storageWatchdog,60_000L);
  }
  MotoWidgetProvider.refresh(this);
 }
 /** Do not call onObdDisconnected repeatedly while Location is switched off:
  * that previously reset the reconnect timer every five seconds forever. */
 private void pauseObdForGpsOff(){
  if(!autoArmed||autoGpsPaused)return;
  autoGpsPaused=true;
  handler.removeCallbacks(reconnectObd);
  ObdAutoSpeedMonitor monitor=obdMonitor;obdMonitor=null;
  if(monitor!=null)monitor.stop(); // Stale callbacks cannot affect the new reader.
  obdSpeedKmh=-1;obdLastSampleElapsed=0;
  autoGate.connectionLost();speedSanity.reset();
  obdLink="GPS 꺼짐 · OBD 연결 일시 중단";
  status=running?"GPS 꺼짐 · 기존 기록 보존 · 다시 켜면 OBD 복구":
    "GPS 꺼짐 · 자동 감지 일시 중단 · 켜면 재개";
  notifySafely(status);
 }
 private void resumeObdAfterGpsOn(){
  if(!autoArmed||!autoGpsPaused)return;
  autoGpsPaused=false;
  status="GPS 켜짐 · OBD 자동 연결 복구 중";
  obdLink="OBD 복구 대기";
  handler.removeCallbacks(reconnectObd);
  handler.post(reconnectObd);
  notifySafely(status);
 }
 private void armAutomaticRide(String address){
  try{
   if(pendingBikeId==null||pendingBikeId.trim().isEmpty())throw new SecurityException("먼저 기록할 바이크를 선택해 주세요.");
   if(address==null||!BluetoothAdapter.checkBluetoothAddress(address))throw new IllegalArgumentException("OBD2 어댑터를 다시 선택해 주세요.");
   if(Build.VERSION.SDK_INT>=31&&checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)throw new SecurityException("Bluetooth 연결 권한이 필요합니다.");
   BluetoothManager bluetooth=getSystemService(BluetoothManager.class);
   BluetoothAdapter adapter=bluetooth==null?null:bluetooth.getAdapter();
   if(adapter==null)throw new IllegalStateException("휴대폰 Bluetooth를 사용할 수 없습니다.");
   BluetoothDevice paired=null;
   if(adapter.isEnabled()){
    for(BluetoothDevice d:adapter.getBondedDevices())if(address.equals(d.getAddress())){paired=d;break;}
   }else if(ObdHandsFreeSession.enabled(this)&&
       pendingBikeId.equals(ObdHandsFreeSession.bikeId(this))&&
       address.equals(ObdHandsFreeSession.deviceAddress(this))){
    // Only previously confirmed paired adapters can resume when phone BT is off.
    paired=adapter.getRemoteDevice(address);
   }
   if(paired==null)throw new SecurityException("해당 바이크에 페어링해 둔 OBD 장치를 확인할 수 없습니다.");
   Sharing.stop(); // Automatic local rides must never inherit a group sharing session.
   prepareForeground(true);autoGate.reset();speedSanity.reset();autoNotificationRate.reset();
   selectedAutoDevice=paired;obdReconnectRounds=0;autoGpsPaused=false;handler.removeCallbacks(reconnectObd);
   autoGate.setStopAfterMs(getSharedPreferences("developer_experiments",0).getLong(
    BikeObdPreferences.stopAfterKey(pendingBikeId),ObdRideAutoGate.USER_DEFAULT_STOP_AFTER_MS));
   if(!ObdHandsFreeSession.enable(this,pendingBikeId,address))
    throw new IOException("자동 감지 설정을 보관하지 못했습니다.");
   autoArmed=true;status="OBD2 연결 대기 · 자동 라이딩 감지";
   obdSpeedKmh=-1;obdRpm=Double.NaN;obdCoolantC=Double.NaN;obdVoltage=Double.NaN;obdFuelPercent=Double.NaN;obdFuelAt=0;
   obdReconnects=0;obdSpeedSamples=0;obdAverageLatencyMs=-1;obdQueryFailures=0;obdConnectionAttempts=0;obdConnectionSuccesses=0;obdLink="OBD2 연결 중";obdLastSampleElapsed=0;
   obdPidSupport="표준 지원 PID 확인 중 · ODO 미검증";
   obdAuxDiagnostics="센서 수신 기록 없음";obdStationaryRemainingMs=-1;
   obdRpmAt=0;obdCoolantAt=0;obdVoltageAt=0;obdFuelAt=0;obdFuelPercent=Double.NaN;obdLastError="없음";
   if(adapter.isEnabled())startAutoMonitor(paired);
   else{
    obdLink="Bluetooth 꺼짐 · 켜지면 자동 연결";
    status="Bluetooth 켜짐 대기 · 다음 주행 자동 감지 유지";
    handler.postDelayed(reconnectObd,30_000L);notifySafely(status);
   }
   handler.postDelayed(permissionCheck,5000);
  }catch(Exception e){stopWith("자동 기록 시작 실패: "+(e.getMessage()==null?"연결을 확인해 주세요.":e.getMessage()),false);}
 }
 private void startAutoMonitor(BluetoothDevice paired){
  if(!autoArmed||autoGpsPaused||obdMonitor!=null)return;
  obdLink="OBD 동글 자동 연결 시도";
  obdConnectionAttempts++;
  final ObdAutoSpeedMonitor[] ref=new ObdAutoSpeedMonitor[1];
   ObdAutoSpeedMonitor reader=new ObdAutoSpeedMonitor(paired,new ObdAutoSpeedMonitor.Listener(){
    @Override public boolean allowOdometer(){return OdometerAutoSyncInbox.enabled(RideService.this,pendingBikeId);}
    @Override public void odometer(double km){
     if(autoArmed&&OdometerAutoSyncInbox.enabled(RideService.this,pendingBikeId))
      OdometerAutoSyncInbox.offer(RideService.this,pendingBikeId,km,km,System.currentTimeMillis());
    }
    @Override public void odometerStatus(String reason){
     handler.post(()->{if(autoArmed)obdLastError=reason;});
    }
    @Override public void linkQuality(long averageMs,int samples,int failures){
     handler.post(()->{if(autoArmed&&obdMonitor==ref[0]){
      obdAverageLatencyMs=averageMs;obdQueryFailures=failures;
     }});
    }
    @Override public void speed(int kmh){handler.post(()->{if(autoArmed&&obdMonitor==ref[0]){
     long elapsed=SystemClock.elapsedRealtime();
      if(!speedSanity.accept(kmh,elapsed)){
       autoGate.connectionLost();obdLastError="OBD2 속도 급변값 무시 · 자동 출발 보류";
       return;
      }
      obdSpeedKmh=kmh;obdSpeedSamples++;obdLastSampleElapsed=elapsed;
      obdValidSamplesThisConnection=ObdReconnectHealthPolicy.acceptValidSpeed(obdValidSamplesThisConnection);
      if(ObdReconnectHealthPolicy.stable(obdValidSamplesThisConnection))obdReconnectRounds=0;
      gpsOnlyAfterObd=false;
      if(obdSpeedSamples==1)ObdLocalDiagnosticLog.record(RideService.this,"SENSOR_OK");
      handleAutoSpeed(kmh);
    }});}
    @Override public void telemetry(String pid,double value){handler.post(()->{if(autoArmed&&obdMonitor==ref[0]){
     long now=SystemClock.elapsedRealtime();
     if("010C".equals(pid)){obdRpm=value;obdRpmAt=now;}
     else if("0105".equals(pid)){obdCoolantC=value;obdCoolantAt=now;}
     else if("0142".equals(pid)){obdVoltage=value;obdVoltageAt=now;}
     else if("012F".equals(pid)){obdFuelPercent=value;obdFuelAt=now;}
    }});}
    @Override public void capabilities(ObdPidCapabilities capability){handler.post(()->{
     if(autoArmed&&obdMonitor==ref[0]){
       obdPidSupport=capability.summary();
       ObdLocalDiagnosticLog.capability(RideService.this,capability);
       if(capability.forPid("010D")==ObdPidCapabilities.Support.NO)
        ObdLocalDiagnosticLog.record(RideService.this,"NO_SPEED");
      }
    });}
    @Override public void auxiliaryDiagnostics(String summary){handler.post(()->{
     if(autoArmed&&obdMonitor==ref[0])obdAuxDiagnostics=summary;
    });}
    @Override public void connected(){handler.post(()->{if(autoArmed&&obdMonitor==ref[0]){
     obdLink="연결됨 · 차량 데이터 읽기 전용";
     obdValidSamplesThisConnection=0;
     MotoWidgetProvider.refresh(RideService.this);
      ObdLocalDiagnosticLog.record(RideService.this,"BT_OK");obdConnectionSuccesses++;
     if(!running)status="OBD2 연결됨 · 출발 대기";
    }});}
    @Override public void reconnecting(int attempt,int max){handler.post(()->{if(autoArmed&&obdMonitor==ref[0]){
     obdLink="연결 재시도 "+attempt+"/"+max;obdConnectionAttempts++;MotoWidgetProvider.refresh(RideService.this);
     obdReconnects=attempt;obdValidSamplesThisConnection=0;
     speedSanity.reset();ObdLocalDiagnosticLog.record(RideService.this,"RECONNECT");obdSpeedKmh=-1;obdLastSampleElapsed=0;
     obdRpmAt=0;obdCoolantAt=0;obdVoltageAt=0;obdFuelAt=0;obdFuelPercent=Double.NaN;
     obdRpm=Double.NaN;obdCoolantC=Double.NaN;obdVoltage=Double.NaN;
     // OBD loss must never overwrite GPS-measured speed: false zeroes can trigger
     // a parked-ride finish during an active trip.
     autoGate.connectionLost();obdStationaryRemainingMs=-1;
     status="OBD2 신호 손실 · "+obdLink+(running?" · GPS 기록 유지":"");
     notifySafely(status);
    }});}
    @Override public void disconnected(String reason){handler.post(()->{if(autoArmed&&obdMonitor==ref[0])onObdDisconnected(reason);});}
   });
   ref[0]=reader;obdMonitor=reader;
   ObdLocalDiagnosticLog.record(this,"AUTO_ON");
   reader.start();
 }
 private void handleAutoSpeed(int speedKmh){
  if(!autoArmed||autoFinishPending||finishPending)return;
  long now=SystemClock.elapsedRealtime();
  ObdRideAutoGate.Event event=autoGate.accept(speedKmh,now);
  obdStationaryRemainingMs=autoGate.stationaryRemainingMs(now);
  if(event==ObdRideAutoGate.Event.START){
   // The externally requested GPS start is interlocked; auto-start must use
   // the same reservation or a concurrent form save/restore could begin.
   if(!RestoreRideGate.beginRideStart()){
    autoGate.reset(); // No GPS file was opened. Retry after fresh speed samples.
    obdStationaryRemainingMs=-1;
    status="기록 저장·백업 중 · OBD 출발 감지 대기";
    notifySafely(status);
    return;
   }
   try{
    // Re-check after obtaining the process-wide write/start reservation.
    if(!autoArmed||running||autoFinishPending)return;
    openRideTrack();requestGpsUpdatesChecked();
    status="OBD2 출발 감지 · GPS 기록 중";ObdLocalDiagnosticLog.record(this,"GPS_STARTED");
   }catch(Exception e){stopWith("GPS 자동 기록 시작 실패: "+e.getMessage(),false);return;}
   finally{RestoreRideGate.endRideStart();}
  }else if(event==ObdRideAutoGate.Event.FINISH){finishAutoTrack();}
  // Speed samples arrive faster than the user needs notification updates. Avoid
  // rebuilding the notification every second while an independent navigation app is in foreground.
  // START and FINISH remain immediate; GPS samples and files are never throttled.
  boolean notificationDue=autoNotificationRate.shouldNotify(SystemClock.elapsedRealtime());
  if(autoArmed&&(notificationDue||event!=ObdRideAutoGate.Event.NONE)){
   String text=running?(obdStationaryRemainingMs>=0
    ?ObdRestStatus.label(true,true,obdStationaryRemainingMs)+" · GPS 기록 유지"
    :"자동 기록 중 · OBD2 "+speedKmh+" km/h · GPS "+String.format(java.util.Locale.KOREA,"%.2f km",liveDistanceKm))
    :"OBD2 연결됨 · 출발 대기 · 위치 공유 꺼짐";
   notifySafely(text);
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
  final File savedTrack=track;
  HandlerThread worker=gpsIoThread;
  if(worker==null||!worker.isAlive()||!new Handler(worker.getLooper()).post(()->{
   boolean verified=closeTrack(true);
   if(verified)RideCompletionInbox.record(getApplicationContext(),savedTrack);
   // Reconcile previous sections in the derived GPS ledger, never the vehicle ODO.
   if(verified&&completedSegments>0)new Thread(()->{
    try{RideMileageEstimate.rebuild(getApplicationContext());}catch(IOException ignored){}
   },"moto-rollover-distance-reconcile").start();
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
   if(verified)RideCompletionInbox.record(getApplicationContext(),savedTrack);
   // Reconcile previous sections in the derived GPS ledger, never the vehicle ODO.
   if(verified&&completedSegments>0)new Thread(()->{
    try{RideMileageEstimate.rebuild(getApplicationContext());}catch(IOException ignored){}
   },"moto-rollover-distance-reconcile").start();
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
  if(!verified){
   // Do not silently open another auto track after a failed verified write.
   // An unfinished nonempty .track stays recoverable, and the rider must review it.
   ObdHandsFreeSession.disable(this);
   stopWith("자동 저장 검증 실패 · 복구 화면에서 기록을 확인하고 자동 감지를 다시 켜세요",false);
   return;
  }
  status="자동 라이딩 저장 완료 · 다음 출발 감지 대기";
  if(savedTrack!=null&&savedTrack.isFile()&&count>=2){
   new Thread(()->{try{RideInsights.Result summary=RideInsights.analyze(savedTrack);
    handler.post(()->{if(autoArmed&&!running){status=String.format(java.util.Locale.KOREA,
     "라이딩 저장 · %.1f km · %.0f km/h 최고 · 다시 출발 대기\n%s",summary.distanceKm,summary.peakKmh,RideDistanceReviewPolicy.suggestion(summary.distanceKm,true));
     notifySafely(status);}});
   }catch(Exception ignored){}},"moto-auto-ride-summary").start();
  }
  if(verified)DailyMaintenanceJob.checkSoon(this);
 }
 private void disarmAutoByUser(String reason){
  ObdHandsFreeSession.disable(this);
  handler.removeCallbacks(reconnectObd);
  autoArmed=false;autoGpsPaused=false;selectedAutoDevice=null;obdReconnectRounds=0;
  ObdAutoSpeedMonitor old=obdMonitor;obdMonitor=null;
  if(old!=null)old.stop();
  autoGate.reset();speedSanity.reset();
  obdLink="자동 감지 꺼짐";obdSpeedKmh=-1;obdLastSampleElapsed=0;
  obdStationaryRemainingMs=-1;
  ObdLocalDiagnosticLog.record(this,"AUTO_OFF");
  if(autoFinishPending){
   finishPending=true;status="OBD 중지 · GPS 저장 완료 확인 중";
   notifySafely(status);return;
  }
  if(running){
   gpsOnlyAfterObd=true;
   status="자동 감지 꺼짐 · 현재 GPS 기록은 계속됨";
   notifySafely(status);
  }else stopWith(reason,false);
 }
 private void onObdDisconnected(String reason){
  // The owner has opted into continuing monitoring, so a dead engine must
  // never turn this into an unarmed session. Preserve any in-flight GPS track.
  ObdLocalDiagnosticLog.automaticSession(this,obdConnectionAttempts,
   obdConnectionSuccesses,obdReconnects,obdQueryFailures,obdSpeedSamples,reason);
  obdLastError="OBD2 · "+reason;
  obdValidSamplesThisConnection=0;
  obdLink="재연결 대기 · "+ObdFailureGuide.classify(reason);
  speedSanity.reset();obdSpeedKmh=-1;obdLastSampleElapsed=0;
  obdRpm=Double.NaN;obdCoolantC=Double.NaN;obdVoltage=Double.NaN;
  obdRpmAt=0;obdCoolantAt=0;obdVoltageAt=0;obdFuelAt=0;obdFuelPercent=Double.NaN;
  autoGate.connectionLost();obdStationaryRemainingMs=-1;obdMonitor=null;
  Sharing.stop();MotoWidgetProvider.refresh(this);
  if(!autoArmed)return;
  if(ObdSpeedSupportFailure.confirmedUnsupported(reason)){
   // An explicit ECU "no PID 010D" cannot be solved by polling Bluetooth forever.
   // Stop the auto mode, preserve any GPS track, and keep a diagnostic breadcrumb.
   ObdHandsFreeSession.disable(this);
   autoArmed=false;selectedAutoDevice=null;
   handler.removeCallbacks(reconnectObd);
   String alert="OBD 속도 PID 010D 미지원 · 자동 출발 불가 · 수동 GPS 기록 이용";
   if(running){
    gpsOnlyAfterObd=true;status=alert+" · 기존 GPS 기록 유지";
    notifySafely(status);
   }else stopWith(alert,false);
   return;
  }
  if(running){
   // Preserve the already-open track; only GPS-verified stationary fallback may save it.
   gpsOnlyAfterObd=true;
  }
  long backoff=ObdHandsFreePolicy.reconnectCooldownMs(++obdReconnectRounds);
  handler.removeCallbacks(reconnectObd);handler.postDelayed(reconnectObd,backoff);
  status=autoFinishPending?"GPS 자동 저장 중 · OBD 재연결 예약":
   running?"OBD 연결 끊김 · GPS 계속 기록 · 정차 확인 후 자동 저장":
   "OBD 대기 · 다음 시동 자동 재연결 ("+(backoff/1000)+"초 후 시도)";
  notifySafely(status);
 }
 Notification notification(String text){
  PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Intent confirmStop=new Intent(this,MainActivity.class).setAction("com.motohub.next.CONFIRM_RIDE_STOP")
    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
  confirmStop.putExtra(MotoWidgetProvider.EXTRA_TAB,"라이딩");
  PendingIntent stop=PendingIntent.getActivity(this,1,confirmStop,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification.Builder builder=new Notification.Builder(this,"ride").setSmallIcon(R.drawable.ic_notification)
    .setContentTitle(autoArmed?"MOTO HUB · OBD2 자동 라이딩":"MOTO HUB · GPS 기록 중")
    .setContentText(text).setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true).setVisibility(Notification.VISIBILITY_PRIVATE)
    .addAction(new Notification.Action.Builder(null,"기록 종료",stop).build());
  if(autoArmed){
   PendingIntent disarm=PendingIntent.getService(this,3,new Intent(this,RideService.class).setAction(AUTO_DISARM),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   builder.addAction(new Notification.Action.Builder(null,"OBD 감지만 중지",disarm).build());
  }
  NavigationChoice choice=NavigationApps.preferred(this);
  Intent navi=NavigationApps.installedLaunchIntent(this,choice);
  if(navi!=null){
   PendingIntent nav=PendingIntent.getActivity(this,2,navi,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   builder.addAction(new Notification.Action.Builder(null,choice.label,nav).build());
  }
  return builder.build();
 }
 /** Runs on the GPS worker, not UI; the first check is immediate, then at most once a minute. */
 private synchronized boolean checkRuntimeStorage(){
  long now=SystemClock.elapsedRealtime();
  if(now-lastStorageCheckElapsed<60_000L)return running;
  lastStorageCheckElapsed=now;
  long free=GpsStorageSpacePolicy.available(getFilesDir());
  if(GpsStorageSpacePolicy.critical(free)){
   stopWith("GPS 저장공간 8MB 미만 · 기록 중지 · 원본은 복구 화면에 보존",false);
   return false;
  }
  if(GpsStorageSpacePolicy.warn(free)&&!lowStorageWarned){
   lowStorageWarned=true;
   notifySafely("GPS 저장공간 부족 · 남은 공간 "+(free/(1024L*1024L))+"MB · 저장공간 확보 필요");
  }else if(!GpsStorageSpacePolicy.warn(free))lowStorageWarned=false;
  return running;
 }
 /**
  * Atomically settle an existing full part before opening the next part.
  * The same session UUID ties together legacy-compatible track files.
  * If rotation fails, the previous committed .done remains intact.
  */
 private synchronized void rotateRideSection()throws Exception{
  if(output==null||track==null||segmentPoints<1||activeRideSession.isEmpty())
   throw new IOException("연속 GPS 구간 상태가 유효하지 않습니다.");
  GpsStorageSpacePolicy.require(getFilesDir());
  File previous=track;
  output.getFD().sync();output.close();output=null;
  TrackCompletion.mark(previous,segmentPoints);
  long timestamp=lastTime>0?lastTime:System.currentTimeMillis();
  File next=new File(previous.getParentFile(),timestamp+"-"+activeRideSession+".track");
  if(!next.createNewFile())throw new IOException("다음 GPS 구간 파일 이름 충돌");
  FileOutputStream nextOutput=null;
  int nextPoints=0;
  try{
   nextOutput=new FileOutputStream(next);
   if(RideBikeBinding.valid(pendingBikeId))RideBikeBinding.write(next,pendingBikeId);
   // Repeat exactly one boundary fix to include its next travel interval.
   // The prior segment and next one have no duplicated distance interval.
   if(lastGood!=null){
    byte[] boundary=TrackFile.point(lastGood.getLatitude(),lastGood.getLongitude(),
      lastGood.getAccuracy(),lastGood.getTime()).getBytes(StandardCharsets.UTF_8);
    nextOutput.write(boundary);nextOutput.flush();nextOutput.getFD().sync();
    nextPoints=1;
   }
  }catch(Exception error){
   if(nextOutput!=null)try{nextOutput.close();}catch(Exception ignored){}
   new File(next.getPath()+".bike").delete();
   if(next.length()==0)next.delete();
   throw error;
  }
  track=next;output=nextOutput;active=next.getName();
  segmentPoints=nextPoints;completedSegments++;
  gpsSync.reset();
  if(gpsWriteHandler!=null)gpsWriteHandler.removeCallbacks(syncPendingGps);
  gpsSyncScheduled=false;
  status="GPS "+(completedSegments+1)+"번째 구간 기록 중 · 이전 구간 저장 완료";
 }
 @Override public synchronized void onLocationChanged(Location location){
  if(!running||output==null||!checkRuntimeStorage())return;if(checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){stopWith("위치 권한이 해제되어 기록을 중지했습니다.",false);return;}long age=SystemClock.elapsedRealtimeNanos()-location.getElapsedRealtimeNanos();
  if(location.isFromMockProvider()||age<0||age>30_000_000_000L||!location.hasAccuracy()||!TrackFile.valid(location.getLatitude(),location.getLongitude(),location.getAccuracy(),location.getTime())||!GpsFixPolicy.acceptTime(location.getTime(),System.currentTimeMillis(),lastTime))return;
  if(lastGood!=null){long delta=location.getTime()-lastGood.getTime();if(delta>0&&delta<=RideMotionGate.MAX_CONTINUOUS_FIX_MS&&location.distanceTo(lastGood)*3600.0/delta>250)return;} // Discard implausible continuous GPS spikes; keep first point after a gap.
  try{
   byte[] point=TrackFile.point(location.getLatitude(),location.getLongitude(),
    location.getAccuracy(),location.getTime()).getBytes(StandardCharsets.UTF_8);
   if(point.length>TrackFile.MAX_BYTES-track.length())rotateRideSection();
   output.write(point);output.flush();
   long writeAt=SystemClock.elapsedRealtime();
   if(gpsSync.shouldSync(writeAt)){
    output.getFD().sync();
    if(gpsWriteHandler!=null)gpsWriteHandler.removeCallbacks(syncPendingGps);
    gpsSyncScheduled=false;
   }else if(!gpsSyncScheduled&&gpsWriteHandler!=null){
    gpsSyncScheduled=true;
    gpsWriteHandler.postDelayed(syncPendingGps,GpsSyncPolicy.MAX_DIRTY_MS);
   }
   lastTime=location.getTime();lastFixElapsed=SystemClock.elapsedRealtime();count++;segmentPoints++;
   if(lastGood!=null){long dt=location.getTime()-lastGood.getTime();float metres=location.distanceTo(lastGood);
    // A GPS blackout followed by a single position must not close a moving ride.
    if(autoArmed)gpsParkedEvidence.observe(lastFixElapsed,dt,metres,
      lastGood.getAccuracy(),location.getAccuracy());
    else gpsParkedEvidence.reset();
    // Estimate only GPS-observed time; long gaps, poor accuracy and spikes
    // are not silently counted as either moving or stopped.
    if(RideTimePolicy.eligible(dt,metres,lastGood.getAccuracy(),location.getAccuracy())){
     if(RideTimePolicy.moving(metres,dt))liveMovingMillis+=dt;
     else liveStationaryMillis+=dt;
    }
    if(RideMotionGate.countDistance(dt,metres,lastGood.getAccuracy(),location.getAccuracy())){
     double measuredKmh=metres*3600.0/dt;
     liveDistanceKm+=metres/1000.0;liveSpeedKmh=measuredKmh;
     if(location.getAccuracy()<=25&&RideStopSuggestion.moving(measuredKmh))lastMotionElapsed=SystemClock.elapsedRealtime();
     if(autoArmed&&location.getAccuracy()<=20){final double speedForGate=measuredKmh;
      // OBD connection callbacks own the autoGate on the main looper. Serialize GPS hints there.
      handler.post(()->{if(autoArmed&&running){autoGate.gpsMovement(speedForGate,SystemClock.elapsedRealtime());obdStationaryRemainingMs=autoGate.stationaryRemainingMs(SystemClock.elapsedRealtime());}});
     }
     livePeakKmh=Math.max(livePeakKmh,measuredKmh);
    }else liveSpeedKmh=0; // A long GPS gap must never be treated as travelled distance.
   }else gpsParkedEvidence.reset();
   lastGood=new Location(location);
   status=count+"개 좌표 · 마지막 "+java.time.Instant.ofEpochMilli(lastTime)+(location.getAccuracy()>40?" · 정확도 낮음":"");
  }catch(Exception e){
   // Only a real GPS file/track calculation failure can stop local recording.
   stopWith("저장 오류로 기록 중지: "+e.getMessage(),false);
   return;
  }
  // Neither optional group transmission nor the notification manager may
  // invalidate an already synced local track or stop this ride.
  long nowForWidget=SystemClock.elapsedRealtime();
  if(lastWidgetRefreshElapsed==0||nowForWidget<lastWidgetRefreshElapsed||
     nowForWidget-lastWidgetRefreshElapsed>=30_000){
   lastWidgetRefreshElapsed=nowForWidget;
   MotoWidgetProvider.refresh(this);
  }
  RidePostCommit.afterWrite(
   ()->{if(!autoArmed&&location.getAccuracy()<=40)Sharing.offer(location);},
   ()->{Sharing.stop();status="그룹 공유 실패 · 로컬 GPS 기록 유지";},
   ()->{
    if(notificationRate.shouldNotify(SystemClock.elapsedRealtime())){
     notifySafely(String.format(java.util.Locale.KOREA,
      "%.0f km/h · %.2f km · ",liveSpeedKmh,liveDistanceKm)+
      (Sharing.enabled?"그룹 위치 공유 켜짐":(gpsOnlyAfterObd?"OBD2 종료 · GPS 단독 기록":"위치 공유 꺼짐")));
    }
   }
  );
 }
 @Override public void onProviderDisabled(String provider){if(LocationManager.GPS_PROVIDER.equals(provider)){
  gpsParkedEvidence.reset();
  // Security-critical: disable remote sharing synchronously, before any UI message is queued.
  Sharing.stop();
  handler.post(()->{if(!running)return;
   synchronized(RideService.this){liveSpeedKmh=0;lastGood=null;lastFixElapsed=0;}
   status="GPS 일시 중단 · 켜면 기록 재개 (그룹 공유 중지)";notifySafely(status);
  });
 }}
 @Override public void onProviderEnabled(String p){if(LocationManager.GPS_PROVIDER.equals(p))handler.post(()->{
  if(autoArmed&&autoGpsPaused)resumeObdAfterGpsOn();
  if(running){status="GPS 복귀 · 좌표 대기";notifySafely(status);}
 });} @Override public void onStatusChanged(String p,int s,Bundle b){}
 void stopWith(String message,boolean completed){status=message;cleanup(completed);stopSelf();}
 private synchronized boolean closeTrack(boolean completed){
  if(gpsWriteHandler!=null)gpsWriteHandler.removeCallbacks(syncPendingGps);
  gpsSyncScheduled=false;gpsSync.reset();
  boolean verified=false;
  if(output!=null){
   try{
    // A successful close alone does not imply a valid, complete GPS track.
    output.getFD().sync();output.close();output=null;
    if(completed&&segmentPoints>0&&track!=null){TrackCompletion.mark(track,segmentPoints);verified=true;}
    else if(segmentPoints==0&&track!=null)TrackFile.discardEmptySession(track);
   }catch(Exception e){
    // Keep any nonempty original .track without a .done marker for recovery.
    status="GPS 저장 검증 실패 · 원본 기록을 복구 화면에서 확인해 주세요";
    try{output.close();}catch(Exception ignored){}
   }finally{output=null;}
  }
  active="";lastGood=null;lastFixElapsed=0;gpsOnlyAfterObd=false;
  gpsParkedEvidence.reset();
  return verified;
 }
 synchronized void cleanup(boolean completed){
  // Stop the independent space watchdog before closing any GPS writer.
  if(gpsWriteHandler!=null)gpsWriteHandler.removeCallbacks(storageWatchdog);
  // Invalidate queued main-thread finalization callbacks after failures or process teardown.
  finishPending=false;autoFinishPending=false;
  handler.removeCallbacks(reconnectObd);selectedAutoDevice=null;obdReconnectRounds=0;autoGpsPaused=false;
  Sharing.stop();autoArmed=false;obdSpeedKmh=-1;obdLastSampleElapsed=0;obdLink="연결 안 됨";
  obdStationaryRemainingMs=-1;obdAuxDiagnostics="센서 수신 기록 없음";
  obdRpm=Double.NaN;obdCoolantC=Double.NaN;obdVoltage=Double.NaN;
  obdRpmAt=0;obdCoolantAt=0;obdVoltageAt=0;obdFuelAt=0;obdFuelPercent=Double.NaN;
  if(obdMonitor!=null){obdMonitor.stop();obdMonitor=null;}
  autoGate.reset();running=false;handler.removeCallbacksAndMessages(null);
  if(manager!=null)try{manager.removeUpdates(this);}catch(Exception ignored){}
  boolean verified=closeTrack(completed);
  if(completed&&!verified&&segmentPoints>0)status="GPS 기록 검증 실패 · 복구 화면에서 원본 기록을 확인해 주세요";
  stopForeground(STOP_FOREGROUND_REMOVE);
  try{NotificationManager nm=getSystemService(NotificationManager.class);if(nm!=null)nm.cancel(405);}catch(Exception ignored){}
  MotoWidgetProvider.refresh(this);
  if(completed&&verified)DailyMaintenanceJob.checkSoon(this);
 }
 @Override public void onDestroy(){if(running||autoArmed)status="라이딩 서비스가 중단됐습니다. 저장된 GPS 좌표는 복구할 수 있어요.";
  cleanup(false);
  if(gpsIoThread!=null){gpsIoThread.quitSafely();gpsIoThread=null;}
  instance=null;super.onDestroy();}
}
