package com.motohub.next;
import android.app.*;import android.content.*;import android.content.pm.PackageManager;import android.content.pm.ServiceInfo;import android.location.*;import android.os.*;import java.io.*;import java.nio.charset.StandardCharsets;import java.util.UUID;import android.bluetooth.*;
/** User-started location FGS. Local recording with explicit group sharing. No boot restart, network or mock positions. */
public final class RideService extends Service implements LocationListener {
 static final String STOP="com.motohub.next.STOP_RIDE",AUTO_ARM="com.motohub.next.AUTO_RIDE_ARM",AUTO_DISARM="com.motohub.next.AUTO_RIDE_DISARM",EXTRA_DEVICE="device_address",EXTRA_BIKE_ID="ride_bike_id",EXTRA_CARDO_TRIGGER="cardo_trigger",CARDO_DISCONNECTED="com.motohub.next.CARDO_DISCONNECTED",RESUME_INTERRUPTED="com.motohub.next.RESUME_INTERRUPTED",EXTRA_INTERRUPTED_NAME="interrupted_track_name";static volatile boolean autoArmed=false;static volatile boolean running=false;static volatile String status="기록 꺼짐",active="";static volatile int count=0;static volatile long startedAt=0,startedElapsed=0,lastMotionElapsed=0;static volatile boolean stopSuggestionShown=false;static volatile double liveDistanceKm=0,liveSpeedKmh=0,livePeakKmh=0;static volatile long liveMovingMillis=0,liveStationaryMillis=0,lastWidgetRefreshElapsed=0;
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
 /** Bound once per ride; switching the garage never changes the GPS owner. */
 static volatile String activeRideBikeId="";
 static volatile RideService instance;
 @Override public void onCreate(){super.onCreate();instance=this;
  // GPS disk sync and callbacks live off the UI looper, preserving location callback order.
  gpsIoThread=new HandlerThread("moto-gps-disk",android.os.Process.THREAD_PRIORITY_BACKGROUND);
  gpsIoThread.start();
  gpsWriteHandler=new Handler(gpsIoThread.getLooper());
 }
 /** Read current service-owned finalization state without leaking it across service instances. */
 static boolean saving(){RideService s=instance;return s!=null&&(s.finishPending||s.autoFinishPending);}
 /** An already running foreground service receives helmet presence updates
  * directly. This avoids Android background startService restrictions and
  * allows a brief disconnect/reconnect without misclassifying a parked trip. */
 static boolean cardoPresenceChanged(String bikeId,boolean connected){
  RideService s=instance;
  if(s==null||!s.cardoSession||!autoArmed||!bikeId.equals(activeRideBikeId))return false;
  s.handler.post(()->{
   if(instance!=s||!s.cardoSession||!autoArmed||!bikeId.equals(activeRideBikeId))return;
   s.cardoConnected=connected;
   s.handler.removeCallbacks(s.cardoDisconnectGrace);
   if(connected&&s.cardoGpsFallbackRecording){
    s.handler.removeCallbacks(s.cardoGpsStopWatch);
    s.handler.postDelayed(s.cardoGpsStopWatch,30_000L);
   }
   if(!connected){
    s.handler.postDelayed(s.cardoDisconnectGrace,10_000L);
    if(!running){
     s.status="카르도 연결 해제 · 10초 동안 재연결 확인";
     s.notifySafely(s.status);
    }
   }else{
    s.status=running?"카르도 다시 연결됨 · GPS 기록 유지":
      "카르도 다시 연결됨 · OBD 출발 감지 대기";
    s.notifySafely(s.status);
   }
  });
  return true;
 }

 static boolean gpsFresh(){RideService s=instance;
  return s!=null&&running&&GpsSignalStatus.fresh(s.lastFixElapsed,SystemClock.elapsedRealtime());
 }
 static long gpsFixElapsed(){RideService s=instance;return s==null?0:s.lastFixElapsed;}
 static boolean gpsParkingHeld(){RideService s=instance;return s!=null&&running&&s.gpsObdParkingHold;}
 static float gpsAccuracyMeters(){
  RideService s=instance;
  Location fix=s==null?null:s.lastGood;
  return fix!=null&&fix.hasAccuracy()&&gpsFresh()?fix.getAccuracy():Float.NaN;
 }
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
 String pendingBikeId="",activeAutoAdapterAddress="";ObdAutoSpeedMonitor obdMonitor;
 boolean cardoSession=false,cardoConnected=false,requestedCardoStart=false;
 String pendingCardoToken="";
 boolean cardoGpsFallbackRecording=false,cardoGpsProbeRegistered=false;
 int cardoGpsProbeFailures=0;
 long cardoGpsRetryAt=0;
 final CardoGpsFallbackGate cardoGpsGate=new CardoGpsFallbackGate();
 final GpsStationaryEvidence gpsStationaryEvidence=new GpsStationaryEvidence();
 final java.util.concurrent.ConcurrentLinkedDeque<Location> cardoGpsPreroll=new java.util.concurrent.ConcurrentLinkedDeque<>();
 boolean prerollReplaying=false;
 Location cardoGpsPrevious=null;
 // Do not power GPS during a healthy OBD wait. A lightweight 12-second
 // handler checks whether ECU speed has gone stale before opening GPS.
 final Runnable cardoGpsProbeWake=new Runnable(){public void run(){
  if(!cardoSession||!autoArmed||running||autoFinishPending||
     !BikeObdPreferences.gpsFallbackEnabled(RideService.this,pendingBikeId))return;
  long now=SystemClock.elapsedRealtime();
  if(!ObdTelemetryFreshness.speedFresh(obdLastSampleElapsed,now)
     &&!cardoGpsProbeRegistered&&now>=cardoGpsRetryAt){
   try{
    requestGpsUpdatesChecked();
    cardoGpsProbeRegistered=true;
    cardoGpsProbeFailures=0;cardoGpsRetryAt=0;
    status="OBD 속도 미수신 · 5분 한정 GPS 안전망 이동 감지";
    notifySafely(status);
   }catch(Exception error){
    ConnectionEventLog.record(RideService.this,pendingBikeId,ConnectionEventLog.Event.GPS_PERMISSION_BLOCKED);
    cardoGpsProbeFailures=Math.min(8,cardoGpsProbeFailures+1);
    long delay=Math.min(60_000L,5_000L*(1L<<Math.min(4,cardoGpsProbeFailures-1)));
    cardoGpsRetryAt=now+delay;
    status="GPS 안전망 위치 제한 · "+(delay/1000)+"초 뒤 재시도 · OBD 대기 유지";
    notifySafely(status);
   }
  }
  handler.postDelayed(this,12_000L);
 }};
 /** GPS fallback never fabricates mileage and auto-stops only after a fresh
  * GPS fix confirms a sustained stationary interval. */
 final Runnable cardoGpsStopWatch=new Runnable(){public void run(){
  if(!cardoGpsFallbackRecording||!running||autoFinishPending)return;
  long now=SystemClock.elapsedRealtime();
  long idleLimit=cardoConnected?600_000L:300_000L;
  boolean stationaryGps=gpsStationaryEvidence.confirmed(now);
  boolean stationaryEcu=obdSpeedKmh>=0&&obdSpeedKmh<=2&&
    ObdTelemetryFreshness.speedFresh(obdLastSampleElapsed,now)&&
    parkingIsolation.observedParking(now);
  if((stationaryGps||stationaryEcu)&&lastMotionElapsed>0&&
     now>=lastMotionElapsed&&now-lastMotionElapsed>=idleLimit){
   status="GPS 안전망 정차 확인 · 파일 저장 검증 중";
   finishAutoTrack();
  }else handler.postDelayed(this,30_000L);
 }};
 final Runnable cardoIdleStop=new Runnable(){public void run(){
  if(cardoSession&&autoArmed&&!running&&!autoFinishPending)
   stopWith("카르도 연결 후 5분 대기 만료 · 배터리 절약",false);
 }};
 // A single Bluetooth ACL disconnect can be transient while the headset
 // swaps audio profiles. Reconnect during grace must restore the session.
 final Runnable cardoDisconnectGrace=new Runnable(){public void run(){
  if(!cardoSession||!autoArmed||cardoConnected)return;
  if(!running){
   stopWith("카르도 연결 종료 확인 · OBD 대기 종료",false);
  }else if(autoGate.stoppedBeforeDisconnect(SystemClock.elapsedRealtime())){
   status="카르도 연결 종료 확인 · 정차 후 GPS 저장 중";
   finishAutoTrack();
  }else{
   status=cardoGpsFallbackRecording?
    "카르도 해제 · GPS 안전망 이동 추적 중 · 정차 확인 후 종료":
    "카르도 연결 끊김 · 주행 중일 수 있어 GPS 기록 유지";
   notifySafely(status);
  }
 }};
 final ObdParkingIsolation parkingIsolation=new ObdParkingIsolation();
 /** The phone may walk after the motorcycle is parked. */
 volatile boolean gpsObdParkingHold=false,gpsObdResumeSplit=false;
 long gpsObdHoldStarted=-1;
 final Runnable finishParkedObdLoss=new Runnable(){@Override public void run(){
  if(!running||autoFinishPending||finishPending||!gpsObdParkingHold)return;
  if(parkingIsolation.dueAfterLost(SystemClock.elapsedRealtime())){
   // Complete the parked bike ride while leaving the explicitly armed
   // Bluetooth foreground watcher ready for the next ignition ON.
   if(!BikeObdPreferences.autoRideEnabled(RideService.this,pendingBikeId)&&!cardoSession){
    // A one-shot reader must not remain connected indefinitely.
    ObdAutoSpeedMonitor reader=obdMonitor;
    if(reader!=null)reader.stop();
    obdMonitor=null;autoArmed=false;
   }
   autoGate.reset();obdStationaryRemainingMs=-1;
   obdLink=autoArmed?"주차 추정 · 다음 시동 자동 재연결 대기":"주차 추정 · 자동 감지 종료";
   status=autoArmed?"GPS 저장 확인 · 다음 시동 OBD 연결 대기":"주차 후 GPS 저장 확인";
   notifySafely(status);
   finishAutoTrack();
  }
 }};final ObdRideAutoGate autoGate=new ObdRideAutoGate();final ObdSpeedSanityPolicy speedSanity=new ObdSpeedSanityPolicy();LocationManager manager;FileOutputStream output;File track;
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
 final Runnable permissionCheck=new Runnable(){public void run(){if(!running&&!autoArmed)return;
  if(autoArmed)RidePowerAudit.sample(RideService.this,pendingBikeId);
  if(autoArmed&&!getSharedPreferences("developer_experiments",0).getBoolean("enabled",false)
   &&!(cardoSession&&CardoAutoRidePrefs.forBike(RideService.this,pendingBikeId))
   &&(!BikeObdPreferences.autoRideEnabled(RideService.this,pendingBikeId)
       ||!BikeObdPreferences.address(RideService.this,pendingBikeId).equals(activeAutoAdapterAddress))){
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
      notifySafely(status);
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
    notifySafely(status);
   }
   if(Sharing.enabled&&((Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)||!notificationsAvailable()))Sharing.stop();
   maybeNotifyStationary(now);
   handler.postDelayed(this,5000);}};
 @Override public IBinder onBind(Intent i){return null;}
 @Override public int onStartCommand(Intent intent,int flags,int id){
  if(intent==null){stopWith("GPS 서비스가 중단됐습니다.",false);return START_NOT_STICKY;}
  String action=intent.getAction();
  if(STOP.equals(action)){
   finishUserRideAsync();return START_NOT_STICKY;
  }
  if(AUTO_DISARM.equals(action)){
   // Explicit opt-out revokes both foreground readiness modes, never the GPS file.
   if(cardoSession&&CardoAutoRidePrefs.forBike(this,pendingBikeId)){
    CardoCompanionPairing.stopObserving(this);
    CardoAutoRidePrefs.disable(this);
    CardoRideStarter.cancelTap(this);
   }
   // Explicit stop revokes the always-ready opt-in. Existing GPS stays intact.
   if(RideBikeBinding.valid(pendingBikeId))
    BikeObdPreferences.setAutoRideEnabled(this,pendingBikeId,false);
   // Disarming the reader must NOT silently discard an active GPS ride.
   if(autoArmed){
    ObdAutoSpeedMonitor monitor=obdMonitor;
    if(monitor!=null)monitor.stop();
    onObdDisconnected("사용자가 자동 감지를 중지했습니다.");
   }else if(!RideSessionInterlock.keepExistingSession(running,autoArmed))stopSelf();
   return START_NOT_STICKY;
  }
  if(CARDO_DISCONNECTED.equals(action)){
   // Explicit UI disable also exits this Cardo session; preserve active GPS.
   if(!cardoSession||!autoArmed)return START_NOT_STICKY;
   cardoConnected=false;
   handler.removeCallbacks(cardoDisconnectGrace);
   handler.post(cardoDisconnectGrace);
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
   if(RESUME_INTERRUPTED.equals(action)){
    if(running||autoArmed)return START_NOT_STICKY;
    try{
     File old=RideResumePolicy.source(getFilesDir(),intent.getStringExtra(EXTRA_INTERRUPTED_NAME),System.currentTimeMillis());
     String uuid=RideActiveSessionPolicy.session(old.getName());
     pendingBikeId=RideBikeBinding.readStrict(old);
     activeRideBikeId=RideBikeBinding.valid(pendingBikeId)?pendingBikeId:"";
     prepareForeground(false);openRideTrack(uuid);requestGpsUpdatesChecked();
     handler.postDelayed(permissionCheck,5000L);
     status="중단 이전 원본 보존 · 같은 세션으로 새 GPS 구간 기록";
     notifySafely(status);
    }catch(Exception error){stopWith("GPS 이어 기록 실패 · 원본 보존: "+
     (error.getMessage()==null?"확인 필요":error.getMessage()),false);}
    return START_NOT_STICKY;
   }
   if(AUTO_ARM.equals(action)){
    if(running||autoArmed)return START_NOT_STICKY;
    pendingBikeId=intent.getStringExtra(EXTRA_BIKE_ID);
    if(!RideBikeBinding.valid(pendingBikeId)){stopWith("등록된 바이크를 선택한 뒤 OBD 기록을 켜 주세요.",false);return START_NOT_STICKY;}
    activeRideBikeId=pendingBikeId;
    requestedCardoStart=intent.getBooleanExtra(EXTRA_CARDO_TRIGGER,false);
    pendingCardoToken=requestedCardoStart?
     intent.getStringExtra(CardoStartHandshake.EXTRA_TOKEN):"";
    armAutomaticRide(intent.getStringExtra(EXTRA_DEVICE));return START_NOT_STICKY;
   }
   if(running||autoArmed)return START_NOT_STICKY;
   pendingBikeId=intent.getStringExtra(EXTRA_BIKE_ID);
   activeRideBikeId=RideBikeBinding.valid(pendingBikeId)?pendingBikeId:"";
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
 private synchronized void openRideTrack()throws Exception{openRideTrack(null);}
 private synchronized void openRideTrack(String priorSession)throws Exception{
  // Must be checked here as well as the UI: OBD auto-start has no foreground confirmation screen.
  GpsStorageSpacePolicy.require(getFilesDir());
  activeRideSession=RideResumePolicy.validSession(priorSession)?priorSession:UUID.randomUUID().toString();
  track=new File(TrackFile.directory(getFilesDir()),System.currentTimeMillis()+"-"+activeRideSession+".track");
  if(!track.createNewFile())throw new IOException("이미 같은 이름의 GPS 기록이 있습니다 · 원본 보호");
  output=new FileOutputStream(track);
  try{if(RideBikeBinding.valid(pendingBikeId))RideBikeBinding.write(track,pendingBikeId);}
  catch(IOException error){output.close();output=null;track.delete();throw error;}
  count=0;segmentPoints=0;completedSegments=0;lastTime=0;lastFixElapsed=0;lastGood=null;gpsSync.reset();
  gpsStationaryEvidence.reset();
  lastStorageCheckElapsed=-60_000L;lowStorageWarned=false;
  if(gpsWriteHandler!=null)gpsWriteHandler.removeCallbacks(syncPendingGps);gpsSyncScheduled=false;
  startedAt=System.currentTimeMillis();startedElapsed=SystemClock.elapsedRealtime();lastMotionElapsed=startedElapsed;stopSuggestionShown=false;liveDistanceKm=0;liveSpeedKmh=0;livePeakKmh=0;liveMovingMillis=0;liveStationaryMillis=0;lastWidgetRefreshElapsed=0;gpsOnlyAfterObd=false;
  active=track.getName();running=true;notificationRate.reset();status="GPS 신호 대기";
  RideCaptureHealth.started(this);
  if(gpsWriteHandler!=null){
   gpsWriteHandler.removeCallbacks(storageWatchdog);
   gpsWriteHandler.postDelayed(storageWatchdog,60_000L);
  }
  MotoWidgetProvider.refresh(this);
 }
 private void armAutomaticRide(String address){
  try{
   boolean developerApproved=getSharedPreferences("developer_experiments",0).getBoolean("enabled",false);
   boolean bikeApproved=BikeObdPreferences.autoRideEnabled(this,pendingBikeId)
     &&address!=null&&address.equalsIgnoreCase(BikeObdPreferences.address(this,pendingBikeId));
   boolean cardoApproved=requestedCardoStart&&CardoAutoRidePrefs.forBike(this,pendingBikeId)
     &&((BikeObdPreferences.validAddress(address)&&address.equalsIgnoreCase(BikeObdPreferences.address(this,pendingBikeId)))
       ||(!BikeObdPreferences.validAddress(address)&&BikeObdPreferences.gpsFallbackEnabled(this,pendingBikeId)));
   if(!developerApproved&&!bikeApproved&&!cardoApproved)
    throw new SecurityException("바이크 설정에서 자동 라이딩을 먼저 동의해 주세요.");
   boolean cardoGpsOnly=cardoApproved&&!BikeObdPreferences.validAddress(address)&&
     BikeObdPreferences.gpsFallbackEnabled(this,pendingBikeId);
   if(!cardoGpsOnly&&(address==null||!BluetoothAdapter.checkBluetoothAddress(address)))
    throw new IllegalArgumentException("OBD2 어댑터를 다시 선택해 주세요.");
   if(Build.VERSION.SDK_INT>=31&&checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)throw new SecurityException("Bluetooth 연결 권한이 필요합니다.");
   BluetoothManager bluetooth=getSystemService(BluetoothManager.class);
   BluetoothAdapter adapter=bluetooth==null?null:bluetooth.getAdapter();
   if(adapter==null||!adapter.isEnabled())throw new IllegalStateException("Bluetooth를 켜 주세요.");
   if(cardoGpsOnly){
    // Cardo + GPS fallback was explicitly permitted; no invented ECU data.
    Sharing.stop();cardoSession=true;cardoConnected=true;
    cardoGpsFallbackRecording=false;cardoGpsProbeRegistered=false;
    cardoGpsProbeFailures=0;cardoGpsRetryAt=0;
    cardoGpsPrevious=null;cardoGpsGate.reset();cardoGpsPreroll.clear();prerollReplaying=false;gpsStationaryEvidence.reset();parkingIsolation.reset();autoGate.reset();
    prepareForeground(true);autoArmed=true;obdMonitor=null;activeAutoAdapterAddress="";
    RidePowerAudit.begin(this,pendingBikeId);
    obdLink="OBD 미등록 · GPS 단독 안전망";obdSpeedKmh=-1;obdLastSampleElapsed=0;
    ObdAutoRideProgress.stage(this,pendingBikeId,ObdAutoRideProgress.Stage.CARDO_GPS);
    handler.postDelayed(permissionCheck,5000L);
    handler.postDelayed(cardoGpsProbeWake,1000L);
    handler.postDelayed(cardoIdleStop,CardoStandbyPolicy.idleWindowMs(true));
    status="카르도 연결 · GPS 이동 검증 대기 (OBD 없이)";notifySafely(status);
    CardoStartHandshake.armed(this,pendingBikeId,pendingCardoToken,true);
    return;
   }
   BluetoothDevice paired=null;
   for(BluetoothDevice d:adapter.getBondedDevices())if(address.equals(d.getAddress())){paired=d;break;}
   if(paired==null)throw new SecurityException("이미 페어링된 OBD2 장치만 사용할 수 있습니다.");
   activeAutoAdapterAddress=address.toUpperCase(java.util.Locale.ROOT);
   Sharing.stop(); // Automatic local rides must never inherit a group sharing session.
   cardoSession=cardoApproved;cardoConnected=cardoApproved;
   cardoGpsFallbackRecording=false;cardoGpsProbeRegistered=false;
   cardoGpsProbeFailures=0;cardoGpsRetryAt=0;
   cardoGpsPrevious=null;cardoGpsGate.reset();cardoGpsPreroll.clear();gpsStationaryEvidence.reset();
   prepareForeground(true);autoGate.reset();speedSanity.reset();autoNotificationRate.reset();
   autoGate.setStopAfterMs(getSharedPreferences("developer_experiments",0).getLong("obd_stop_after_ms",ObdRideAutoGate.USER_DEFAULT_STOP_AFTER_MS));
   parkingIsolation.reset();gpsObdParkingHold=false;gpsObdResumeSplit=false;gpsObdHoldStarted=-1;
   autoArmed=true;status="OBD2 연결 대기 · 자동 라이딩 감지";
   RidePowerAudit.begin(this,pendingBikeId);
   ObdAutoRideProgress.stage(this,pendingBikeId,ObdAutoRideProgress.Stage.CONNECTING);
   obdSpeedKmh=-1;obdRpm=Double.NaN;obdCoolantC=Double.NaN;obdVoltage=Double.NaN;obdFuelPercent=Double.NaN;obdFuelAt=0;
   obdReconnects=0;obdSpeedSamples=0;obdAverageLatencyMs=-1;obdQueryFailures=0;obdConnectionAttempts=1;obdConnectionSuccesses=0;obdLink="OBD2 연결 중";obdLastSampleElapsed=0;
   obdPidSupport="표준 지원 PID 확인 중 · ODO 미검증";
   obdAuxDiagnostics="센서 수신 기록 없음";obdStationaryRemainingMs=-1;
   obdRpmAt=0;obdCoolantAt=0;obdVoltageAt=0;obdFuelAt=0;obdFuelPercent=Double.NaN;obdLastError="없음";
   final ObdAutoSpeedMonitor[] ref=new ObdAutoSpeedMonitor[1];
   ObdAutoSpeedMonitor reader=new ObdAutoSpeedMonitor(paired,new ObdAutoSpeedMonitor.Listener(){
    @Override public boolean allowOdometer(){return OdometerAutoSyncInbox.enabled(RideService.this,pendingBikeId);}
    @Override public boolean allowDtcDistance(){
     return DtcOdoBridge.enabled(RideService.this,pendingBikeId);
    }
    @Override public void dtcDistance(int km){
     if(autoArmed&&pendingBikeId.equals(activeRideBikeId))
      DtcOdoBridge.offer(RideService.this,pendingBikeId,km,System.currentTimeMillis());
    }
    @Override public void dtcDistanceStatus(String reason){
     handler.post(()->{if(autoArmed)obdLastError=reason;});
    }
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
    @Override public void speed(int kmh){
     // Preserve the worker's receipt time; a busy UI looper must not make old
     // speed data look fresh or count delayed callbacks as a new acceleration.
     final long receivedAt=SystemClock.elapsedRealtime();
     handler.post(()->{if(autoArmed&&obdMonitor==ref[0]){
     if(!ObdTelemetryFreshness.speedFresh(receivedAt,SystemClock.elapsedRealtime()))return;
     long elapsed=receivedAt;
      if(!speedSanity.accept(kmh,elapsed)){
       autoGate.connectionLost();obdLastError="OBD2 속도 급변값 무시 · 자동 출발 보류";
       return;
      }
      obdSpeedKmh=kmh;obdSpeedSamples++;obdLastSampleElapsed=elapsed;
      // A real ECU speed report means GPS safety-net sampling is unnecessary.
      // Do not touch GPS if a real track is being recorded.
      if(cardoSession&&cardoGpsProbeRegistered&&!running){
       if(manager!=null)try{manager.removeUpdates(RideService.this);}catch(Exception ignored){}
       cardoGpsProbeRegistered=false;cardoGpsPrevious=null;cardoGpsGate.reset();
       // Keep a bounded recent prefix until a verified ECU-speed start.
       // It is not permitted to become motorcycle ODO or maintenance km.
      }
       boolean heldBefore=gpsObdParkingHold;
       parkingIsolation.onSpeed(kmh,elapsed);
       // A valid ECU speed sample, not Bluetooth socket establishment, ends loss grace.
       handler.removeCallbacks(finishParkedObdLoss);
       synchronized(RideService.this){
        gpsObdParkingHold=running&&parkingIsolation.holdGps();
        if(gpsObdParkingHold&&!heldBefore){gpsObdHoldStarted=elapsed;liveSpeedKmh=0;}
        if(heldBefore&&!gpsObdParkingHold){
         if(gpsObdHoldStarted>0&&elapsed-gpsObdHoldStarted>=15_000)gpsObdResumeSplit=true;
         gpsObdHoldStarted=-1;
        }
       }
      if(obdSpeedSamples==1){
       ObdLocalDiagnosticLog.record(RideService.this,"SENSOR_OK");
       ObdAutoRideProgress.stage(RideService.this,pendingBikeId,ObdAutoRideProgress.Stage.SPEED);
      }
      handleAutoSpeed(kmh,elapsed);
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
     parkingIsolation.onLinkConnected();
     obdLink="연결됨 · 차량 데이터 읽기 전용";MotoWidgetProvider.refresh(RideService.this);
      ObdLocalDiagnosticLog.record(RideService.this,"BT_OK");obdConnectionSuccesses++;
      ObdAutoRideProgress.stage(RideService.this,pendingBikeId,ObdAutoRideProgress.Stage.CONNECTED);
     if(!running)status="OBD2 연결됨 · 출발 대기";
    }});}
    @Override public void reconnecting(int attempt,int max){handler.post(()->{if(autoArmed&&obdMonitor==ref[0]){
     obdLink=max==0?(cardoSession?"카르도 감지 · OBD 연결 재시도":"OBD 재연결 상시 대기 · 약 60초 간격"):
      "연결 재시도 "+attempt+"/"+max;
     obdConnectionAttempts++;MotoWidgetProvider.refresh(RideService.this);
     boolean parkedBeforeLoss=running&&parkingIsolation.onLinkLoss(SystemClock.elapsedRealtime());
     if(parkedBeforeLoss){
      synchronized(RideService.this){
       gpsObdParkingHold=true;
       if(gpsObdHoldStarted<0)gpsObdHoldStarted=SystemClock.elapsedRealtime();
      }
      long remaining=parkingIsolation.remainingAfterLost(SystemClock.elapsedRealtime());
      handler.removeCallbacks(finishParkedObdLoss);
      handler.postDelayed(finishParkedObdLoss,Math.max(1,remaining));
     }
     ObdAutoRideProgress.stage(RideService.this,pendingBikeId,ObdAutoRideProgress.Stage.RETRYING);
     obdReconnects=attempt;speedSanity.reset();ObdLocalDiagnosticLog.record(RideService.this,"RECONNECT");obdSpeedKmh=-1;obdLastSampleElapsed=0;
     obdRpmAt=0;obdCoolantAt=0;obdVoltageAt=0;obdFuelAt=0;obdFuelPercent=Double.NaN;
     obdRpm=Double.NaN;obdCoolantC=Double.NaN;obdVoltage=Double.NaN;
     autoGate.connectionLost();obdStationaryRemainingMs=-1;liveSpeedKmh=0;
     status="OBD2 신호 손실 · "+obdLink+(running?
       (gpsObdParkingHold?" · 주차 후보, 휴대폰 도보 GPS 제외":" · GPS 기록 유지"):"");
     notifySafely(status);
    }});}
    @Override public void disconnected(String reason){handler.post(()->{if(autoArmed&&obdMonitor==ref[0])onObdDisconnected(reason);});}
   },bikeApproved||cardoSession); // Cardo retries stop at five-minute idle deadline
   ref[0]=reader;obdMonitor=reader;
   ObdLocalDiagnosticLog.record(this,"AUTO_ON");
   reader.start();handler.postDelayed(permissionCheck,5000);
   if(cardoSession)CardoStartHandshake.armed(this,pendingBikeId,pendingCardoToken,false);
   if(cardoSession){handler.removeCallbacks(cardoIdleStop);handler.postDelayed(cardoIdleStop,CardoStandbyPolicy.idleWindowMs(
    BikeObdPreferences.gpsFallbackEnabled(this,pendingBikeId)));}
   if(cardoSession&&BikeObdPreferences.gpsFallbackEnabled(this,pendingBikeId)){
    handler.removeCallbacks(cardoGpsProbeWake);
    handler.postDelayed(cardoGpsProbeWake,5_000L);
    status="OBD 속도 우선 대기 · 미수신 시만 GPS 안전망 준비";
    notifySafely(status);
   }
  }catch(Exception e){
   CardoStartHandshake.rejected(this,pendingBikeId,pendingCardoToken);
   stopWith("자동 기록 시작 실패: "+(e.getMessage()==null?"연결을 확인해 주세요.":e.getMessage()),false);
  }
 }
 private void startCardoGpsFallbackRide(){
  if(!cardoSession||!autoArmed||running||autoFinishPending||finishPending||
     !BikeObdPreferences.gpsFallbackEnabled(this,pendingBikeId)||
     ObdTelemetryFreshness.speedFresh(obdLastSampleElapsed,SystemClock.elapsedRealtime()))return;
  if(!RestoreRideGate.beginRideStart()){
   cardoGpsGate.reset();
   status="GPS 안전망 출발 감지 · 백업/복원이 끝나면 다시 확인";
   notifySafely(status);return;
  }
  try{
   if(!autoArmed||running||autoFinishPending)return;
   openRideTrack();
   if(!cardoGpsProbeRegistered)requestGpsUpdatesChecked();
   cardoGpsFallbackRecording=true;
   flushActualGpsPreroll(45_000L);
   handler.removeCallbacks(cardoGpsProbeWake);
   handler.removeCallbacks(cardoIdleStop);
   handler.removeCallbacks(cardoGpsStopWatch);
   handler.postDelayed(cardoGpsStopWatch,30_000L);
   status="OBD 속도 미수신 · GPS 이동 검증 후 로컬 기록 중";
   ObdLocalDiagnosticLog.record(this,"GPS_STARTED");
   ObdAutoRideProgress.stage(this,pendingBikeId,ObdAutoRideProgress.Stage.GPS);
   notifySafely(status);
  }catch(Exception e){
   stopWith("GPS 안전망 기록 시작 실패 · 원본 보존: "+e.getMessage(),false);
  }finally{RestoreRideGate.endRideStart();}
 }
 /** Replays real, timestamped GPS fixes on the ordered disk thread.
  * No interpolated coordinates and no old phone-walking history. */
 private void flushActualGpsPreroll(long maxAgeMs){
  if(gpsWriteHandler==null)return;
  long now=SystemClock.elapsedRealtimeNanos()/1_000_000L;
  final java.util.ArrayList<Location> real=new java.util.ArrayList<>();
  for(Location fix:cardoGpsPreroll){
   long age=now-fix.getElapsedRealtimeNanos()/1_000_000L;
   if(age>=0&&age<=maxAgeMs)real.add(new Location(fix));
  }
  cardoGpsPreroll.clear();
  if(real.isEmpty())return;
  gpsWriteHandler.postAtFrontOfQueue(()->{
   prerollReplaying=true;
   try{for(Location fix:real)if(running)onLocationChanged(fix);}
   finally{prerollReplaying=false;}
  });
 }
 private void handleAutoSpeed(int speedKmh,long receivedAt){
  if(!autoArmed||autoFinishPending||finishPending)return;
  long now=receivedAt;
  ObdRideAutoGate.Event event=autoGate.accept(speedKmh,now);
  obdStationaryRemainingMs=autoGate.stationaryRemainingMs(now);
  if(event==ObdRideAutoGate.Event.START){
   // Keep Cardo idle deadline until GPS file has successfully started.
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
    // The GPS probe may already have measured the initial road segment
    // before the ECU delivered three independent speed samples.
    if(cardoSession)flushActualGpsPreroll(15_000L);
    if(cardoSession)handler.removeCallbacks(cardoIdleStop);
    status="OBD2 출발 감지 · GPS 기록 중";ObdLocalDiagnosticLog.record(this,"GPS_STARTED");
    ObdAutoRideProgress.stage(this,pendingBikeId,ObdAutoRideProgress.Stage.GPS);
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
   RideCaptureHealth.finished(getApplicationContext(),verified,count);
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
   RideSessionVerification.Result whole=verified?
    RideSessionVerification.verify(getFilesDir(),savedTrack,completedSegments,count,liveDistanceKm):
    RideSessionVerification.fail("마지막 GPS 구간 검증 실패 · 원본 보존");
   RideCaptureHealth.finished(getApplicationContext(),verified,count);
   if(verified)RideCompletionInbox.record(getApplicationContext(),savedTrack);
   // Reconcile previous sections in the derived GPS ledger, never the vehicle ODO.
   if(verified&&completedSegments>0)new Thread(()->{
    try{RideMileageEstimate.rebuild(getApplicationContext());}catch(IOException ignored){}
   },"moto-rollover-distance-reconcile").start();
   handler.post(()->{if(autoFinishPending)completeAutoTrack(savedTrack,verified,whole);});
  })){
   // If the GPS disk worker is unavailable, preserve the original and don't
   // assert the session is proven without a full readback.
   completeAutoTrack(savedTrack,closeTrack(true),
    RideSessionVerification.fail("GPS 검증 스레드 이용 불가 · 전체 구간 미확인"));
  }
 }
 private void completeAutoTrack(File savedTrack,boolean verified,RideSessionVerification.Result whole){
  // Mark setup verified only after all parts of one session were read back.
  FirstRideSetupProof.reportSessionCompletion(this,pendingBikeId,savedTrack,verified,count,liveDistanceKm,whole);
  autoFinishPending=false;running=false;
  // Reset recording after the stored ride; new movement can start a new track.
  autoGate.reset();parkingIsolation.reset();
  cardoGpsFallbackRecording=false;cardoGpsPrevious=null;cardoGpsGate.reset();cardoGpsPreroll.clear();gpsStationaryEvidence.reset();
  handler.removeCallbacks(cardoGpsStopWatch);
  synchronized(this){gpsObdParkingHold=false;gpsObdHoldStarted=-1;gpsObdResumeSplit=false;}
  if(cardoSession){
   cardoGpsProbeRegistered=false;
   if(cardoConnected&&BikeObdPreferences.gpsFallbackEnabled(this,pendingBikeId)){
    handler.removeCallbacks(cardoGpsProbeWake);
    handler.postDelayed(cardoGpsProbeWake,5_000L);
   }
   if(!cardoConnected){
    stopWith(verified?
      "카르도 OFF · GPS 기록 검증 완료 · OBD 대기 종료":
      "카르도 OFF · GPS 파일 검증 실패 · 복구 화면에서 원본 확인",false);
    if(verified)DailyMaintenanceJob.checkSoon(this);
    return;
   }
   handler.removeCallbacks(cardoIdleStop);
   handler.postDelayed(cardoIdleStop,CardoStandbyPolicy.idleWindowMs(
    BikeObdPreferences.gpsFallbackEnabled(this,pendingBikeId)));
  }
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
  ObdAutoRideProgress.stage(this,pendingBikeId,verified?ObdAutoRideProgress.Stage.SAVED:ObdAutoRideProgress.Stage.FAILED);
  if(verified&&savedTrack!=null&&savedTrack.isFile()&&count>=2){
   new Thread(()->{try{RideInsights.Result summary=RideInsights.analyze(savedTrack);
    handler.post(()->{if(autoArmed&&!running){status=String.format(java.util.Locale.KOREA,
     "라이딩 저장 · %.1f km · %.0f km/h 최고 · 다시 출발 대기\n%s",summary.distanceKm,summary.peakKmh,RideDistanceReviewPolicy.suggestion(summary.distanceKm,true));
     notifySafely(status);}});
   }catch(Exception ignored){}},"moto-auto-ride-summary").start();
  }
  if(verified)DailyMaintenanceJob.checkSoon(this);
 }
 private void onObdDisconnected(String reason){
  // A user explicitly choosing GPS-only recording overrides the parked-ECU pause.
  // Start a fresh GPS segment so any intervening phone movement is not bridged.
  if("사용자가 자동 감지를 중지했습니다.".equals(reason)){
   synchronized(this){gpsObdParkingHold=false;gpsObdResumeSplit=true;gpsObdHoldStarted=-1;}
   parkingIsolation.reset();
  }
  // A permanent failure may arrive without a reconnect callback (no retry budget).
  // Check the last fresh, stationary ECU samples; never infer parking from phone GPS.
  if(running&&gpsObdParkingHold)
   parkingIsolation.onLinkLoss(SystemClock.elapsedRealtime());
  // No guessing: do not finish or split a moving ride when Bluetooth fails.
  // Keep an already-running GPS session in the same foreground service until its
  // user explicitly ends it. Never auto-arm again or enable group sharing.
  ObdAutoRideProgress.stage(this,pendingBikeId,ObdAutoRideProgress.Stage.FAILED);
  ConnectionEventLog.record(this,pendingBikeId,ConnectionEventLog.Event.OBD_LOST);
  ObdLocalDiagnosticLog.automaticSession(this,obdConnectionAttempts,
   obdConnectionSuccesses,obdReconnects,obdQueryFailures,obdSpeedSamples,reason);
  obdLastError="OBD2 · "+reason;
  obdLink="연결 종료 · "+ObdFailureGuide.classify(reason);MotoWidgetProvider.refresh(this);speedSanity.reset();ObdLocalDiagnosticLog.record(this,"AUTO_OFF");obdSpeedKmh=-1;obdLastSampleElapsed=0;
  obdRpm=Double.NaN;obdCoolantC=Double.NaN;obdVoltage=Double.NaN;
  obdRpmAt=0;obdCoolantAt=0;obdVoltageAt=0;obdFuelAt=0;obdFuelPercent=Double.NaN;
  // The user separately opted in to GPS safety net for a missing 010D PID.
  // Leave the 5-minute foreground sampler alive, but never invent ECU ODO.
  if(cardoSession&&autoArmed&&BikeObdPreferences.gpsFallbackEnabled(this,pendingBikeId)){
   if(obdMonitor!=null)obdMonitor.stop();
   obdMonitor=null;autoGate.reset();obdStationaryRemainingMs=-1;
   if(!running){
    handler.removeCallbacks(cardoGpsProbeWake);
    handler.post(cardoGpsProbeWake);
   }
   if(running&&gpsObdParkingHold&&
      parkingIsolation.remainingAfterLost(SystemClock.elapsedRealtime())>=0){
    long remaining=parkingIsolation.remainingAfterLost(SystemClock.elapsedRealtime());
    handler.removeCallbacks(finishParkedObdLoss);
    handler.postDelayed(finishParkedObdLoss,Math.max(1,remaining));
   }
   status=running?"OBD 통신 종료 · GPS 로컬 기록 유지":
     "OBD 속도 미지원/종료 · 카르도 GPS 안전망 감지 대기";
   notifySafely(status);return;
  }
  autoArmed=false;autoGate.reset();obdStationaryRemainingMs=-1;obdMonitor=null;
  Sharing.stop();
  if(autoFinishPending){
   // Completion owns the same GPS worker. Let its callback stop the service
   // after the existing .track is verified, rather than closing it mid-write.
   status="OBD2 연결 종료 · GPS 저장 검증 중…";
   return;
  }
  if(running){
   if(gpsObdParkingHold&&parkingIsolation.remainingAfterLost(SystemClock.elapsedRealtime())>=0
     &&!"사용자가 자동 감지를 중지했습니다.".equals(reason)){
    long remaining=parkingIsolation.remainingAfterLost(SystemClock.elapsedRealtime());
    status="주차 후 OBD 끊김 추정 · 도보 GPS 제외 · "+Math.max(0,(remaining+999)/1000)+"초 후 종료 판정";
    handler.removeCallbacks(finishParkedObdLoss);
    handler.postDelayed(finishParkedObdLoss,Math.max(1,remaining));
   }else{
    gpsOnlyAfterObd=true;
    status=gpsObdParkingHold?
      "OBD 종료 · 주차 후보, 휴대폰 도보 GPS 제외 · 알림에서 종료 확인":
      "OBD 연결 종료 · 주행 중 신호 손실 가능 · GPS 단독 기록 유지 · 알림에서 종료 가능";
   }
   notifySafely(status);
  }else stopWith("OBD2 자동 감지 종료 · "+reason,false);
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
 private synchronized void rotateRideSection()throws Exception{rotateRideSection(true);}
 /**
  * Normal size-based rotation repeats one fix to preserve a continuous ride.
  * OBD parked-resume rotation must NOT repeat it: the phone may have walked
  * hundreds of metres and the old bike position is not a valid new origin.
  */
 private synchronized void rotateRideSection(boolean bridgeWithLastFix)throws Exception{
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
   // Only storage-size rotation preserves the continuous boundary point.
   // A parked OBD gap is a discontinuity; never manufacture its travelled distance.
   if(bridgeWithLastFix&&lastGood!=null){
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
  // The phone belongs to the rider, not the parked motorcycle.
  if(location==null)return;
  if(gpsObdParkingHold)return;
  if(!running&&autoArmed&&cardoSession&&cardoGpsProbeRegistered&&
     BikeObdPreferences.gpsFallbackEnabled(this,pendingBikeId)){
   long age=SystemClock.elapsedRealtimeNanos()-location.getElapsedRealtimeNanos();
   if(location.isFromMockProvider()||age<0||age>10_000_000_000L||
      !location.hasAccuracy()||location.getAccuracy()>20||location.getAccuracy()<0||
      !TrackFile.valid(location.getLatitude(),location.getLongitude(),location.getAccuracy(),location.getTime())||
      !GpsFixPolicy.acceptTime(location.getTime(),System.currentTimeMillis(),0)){
    cardoGpsGate.reset();cardoGpsPrevious=null;return;
   }
   Location old=cardoGpsPrevious;cardoGpsPrevious=new Location(location);
   cardoGpsPreroll.addLast(new Location(location));
   long newest=location.getElapsedRealtimeNanos()/1_000_000L;
   while(!cardoGpsPreroll.isEmpty()&&(cardoGpsPreroll.size()>20||
     newest-cardoGpsPreroll.peekFirst().getElapsedRealtimeNanos()/1_000_000L>45_000L))
    cardoGpsPreroll.removeFirst();
   if(old!=null){
    long gapMs=(location.getElapsedRealtimeNanos()-old.getElapsedRealtimeNanos())/1_000_000L;
    float metres=location.distanceTo(old);
    boolean obdFresh=ObdTelemetryFreshness.speedFresh(obdLastSampleElapsed,SystemClock.elapsedRealtime());
    if(cardoGpsGate.accept(gapMs,metres,old.getAccuracy(),location.getAccuracy(),obdFresh))
     handler.post(this::startCardoGpsFallbackRide);
   }
   return;
  }
  if(!running||output==null||!checkRuntimeStorage())return;if(checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){stopWith("위치 권한이 해제되어 기록을 중지했습니다.",false);return;}long age=SystemClock.elapsedRealtimeNanos()-location.getElapsedRealtimeNanos();
  if(location.isFromMockProvider()||age<0||age>(prerollReplaying?50_000_000_000L:30_000_000_000L)||!location.hasAccuracy()||!TrackFile.valid(location.getLatitude(),location.getLongitude(),location.getAccuracy(),location.getTime())||!GpsFixPolicy.acceptTime(location.getTime(),System.currentTimeMillis(),lastTime))return;
  if(lastGood!=null){long delta=location.getTime()-lastGood.getTime();if(delta>0&&delta<=RideMotionGate.MAX_CONTINUOUS_FIX_MS&&location.distanceTo(lastGood)*3600.0/delta>250)return;} // Discard implausible continuous GPS spikes; keep first point after a gap.
  gpsStationaryEvidence.accept(location.getElapsedRealtimeNanos()/1_000_000L,
   location.getLatitude(),location.getLongitude(),location.getAccuracy());
  try{
   // Never bridge an OBD-stationary gap (or walking) as traveled motorcycle km.
   if(gpsObdResumeSplit){
    if(segmentPoints>0)rotateRideSection(false);
    lastGood=null;lastTime=0;
    gpsObdResumeSplit=false;
   }
   byte[] point=TrackFile.point(location.getLatitude(),location.getLongitude(),
    location.getAccuracy(),location.getTime()).getBytes(StandardCharsets.UTF_8);
   if(point.length>TrackFile.MAX_BYTES-track.length())rotateRideSection();
   output.write(point);output.flush();
   long writeAt=SystemClock.elapsedRealtime();
   if(gpsSync.shouldSync(writeAt)){
    output.getFD().sync();
    gpsSync.synced(writeAt);
    if(gpsWriteHandler!=null)gpsWriteHandler.removeCallbacks(syncPendingGps);
    gpsSyncScheduled=false;
   }else if(!gpsSyncScheduled&&gpsWriteHandler!=null){
    gpsSyncScheduled=true;
    gpsWriteHandler.postDelayed(syncPendingGps,GpsSyncPolicy.MAX_DIRTY_MS);
   }
   lastTime=location.getTime();lastFixElapsed=SystemClock.elapsedRealtime();count++;segmentPoints++;
   if(count==1)RideCaptureHealth.firstFix(this);
   if(lastGood!=null){long dt=location.getTime()-lastGood.getTime();float metres=location.distanceTo(lastGood);
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
     if(autoArmed&&!gpsObdParkingHold&&obdSpeedKmh>=ObdParkingIsolation.RESUME_MIN_KMH
        &&ObdTelemetryFreshness.speedFresh(obdLastSampleElapsed,SystemClock.elapsedRealtime())
        &&location.getAccuracy()<=20){final double speedForGate=measuredKmh;
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
  // Security-critical: disable remote sharing synchronously, before any UI message is queued.
  Sharing.stop();
  handler.post(()->{if(!running)return;
   synchronized(RideService.this){liveSpeedKmh=0;lastGood=null;lastFixElapsed=0;}
   status="GPS 일시 중단 · 켜면 기록 재개 (그룹 공유 중지)";notifySafely(status);
  });
 }}
 @Override public void onProviderEnabled(String p){if(LocationManager.GPS_PROVIDER.equals(p))handler.post(()->{if(!running)return;status="GPS 복귀 · 좌표 대기";notifySafely(status);});} @Override public void onStatusChanged(String p,int s,Bundle b){}
 void stopWith(String message,boolean completed){
  if((running||autoArmed)&&RideBikeBinding.valid(pendingBikeId))
   ConnectionEventLog.categorizeStop(this,pendingBikeId,message);
  RideCaptureHealth.failedIfActive(this,message);
  status=message;cleanup(completed);stopSelf();
 }
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
  return verified;
 }
 synchronized void cleanup(boolean completed){
  CardoStartHandshake.rejected(this,pendingBikeId,pendingCardoToken);
  CardoStartHandshake.stopped(this,pendingBikeId,pendingCardoToken);
  pendingCardoToken="";
  RidePowerAudit.finish(this,pendingBikeId);
  // Stop the independent space watchdog before closing any GPS writer.
  if(gpsWriteHandler!=null)gpsWriteHandler.removeCallbacks(storageWatchdog);
  // Invalidate queued main-thread finalization callbacks after failures or process teardown.
  finishPending=false;autoFinishPending=false;
  handler.removeCallbacks(finishParkedObdLoss);handler.removeCallbacks(cardoIdleStop);
  handler.removeCallbacks(cardoDisconnectGrace);handler.removeCallbacks(cardoGpsStopWatch);
  handler.removeCallbacks(cardoGpsProbeWake);
  cardoSession=false;cardoConnected=false;requestedCardoStart=false;
  cardoGpsFallbackRecording=false;cardoGpsProbeRegistered=false;
  cardoGpsPrevious=null;cardoGpsGate.reset();cardoGpsPreroll.clear();gpsStationaryEvidence.reset();parkingIsolation.reset();
  gpsObdParkingHold=false;gpsObdResumeSplit=false;gpsObdHoldStarted=-1;
  Sharing.stop();autoArmed=false;activeRideBikeId="";activeAutoAdapterAddress="";obdSpeedKmh=-1;obdLastSampleElapsed=0;obdLink="연결 안 됨";
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
 @Override public void onDestroy(){
  if(running)RideCaptureHealth.failedIfActive(this,"비정상 종료 · GPS 원본 복구 확인 필요");
  if(running||autoArmed)status="라이딩 서비스가 중단됐습니다. 저장된 GPS 좌표는 복구할 수 있어요.";
  cleanup(false);
  if(gpsIoThread!=null){gpsIoThread.quitSafely();gpsIoThread=null;}
  instance=null;super.onDestroy();}
}
