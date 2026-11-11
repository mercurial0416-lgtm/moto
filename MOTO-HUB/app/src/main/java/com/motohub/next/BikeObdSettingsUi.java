package com.motohub.next;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * The per-bike OBD entrypoint lives under Settings > Bike management.
 * Storing an adapter never silently connects it, arms tracking, or changes the ECU.
 */
final class BikeObdSettingsUi {
 private final MainActivity a;
 BikeObdSettingsUi(MainActivity a){this.a=a;}

 void open(String bikeId){
  if(!RideBikeBinding.valid(bikeId)){a.toast("먼저 차고에서 바이크를 선택해 주세요.");return;}
  org.json.JSONObject bike=null;
  for(org.json.JSONObject item:a.items("vehicles"))
   if(bikeId.equals(item.optString("id"))){bike=item;break;}
  if(bike==null){a.toast("등록된 바이크가 없습니다. 차고에서 먼저 등록해 주세요.");return;}
  boolean obd=!BikeObdPreferences.address(a,bikeId).isEmpty();
  boolean cardo=CardoAutoRidePrefs.forBike(a,bikeId);
  boolean fallback=BikeObdPreferences.gpsFallbackEnabled(a,bikeId);
  boolean obdOnly=BikeObdPreferences.autoRideEnabled(a,bikeId);
  boolean sameBike=bikeId.equals(RideService.activeRideBikeId);
  boolean liveObd=sameBike&&RideService.autoArmed&&RideService.obdSpeedKmh>=0&&
   ObdTelemetryFreshness.speedFresh(RideService.obdLastSampleElapsed,android.os.SystemClock.elapsedRealtime());
  boolean active=sameBike&&(RideService.autoArmed||RideService.running);
  boolean anotherBikeActive=!sameBike&&(RideService.autoArmed||RideService.running);
  boolean gpsRecording=sameBike&&RideService.running&&RideService.gpsFresh();
  boolean armed=cardo&&(obd||fallback);
  ConnectionSetupMode.Mode mode=ConnectionSetupMode.get(a,bikeId);
  boolean wantsObd=mode.needsObd(),wantsCardo=mode.needsCardo();
  boolean permissions=setupPermissionsOk();
  LinearLayout p=a.page("라이딩 연결 도우미");
  a.label(p,bike.optString("name","내 바이크"),23,a.fg,true);
  a.label(p,"처음 한 번만 설정하면 돼. 아래에 지금 필요한 일만 표시해.",13,a.muted,false);
  a.gap(p,10);
  String state=gpsRecording?"GPS 경로 기록 중":sameBike&&RideService.running?
   "GPS 기록 시작됨 · 위치 수신 대기":active&&liveObd?
   "OBD 데이터 수신 중 · 출발 감지 활성":active?
   "자동 감지 대기 · 실제 연결 확인 중":armed?
   "카르도 연결을 기다리는 중":obdOnly?
   "OBD 상시 대기 설정됨 · 앱 실행 시 다시 확인":"설정 전 · 아직 자동기록은 꺼져 있어";
  panel(p,"현재 상태",state,
   active?"GPS가 기록 중인지 라이딩 화면과 알림에서 확인해.":
   "블루투스에 기기가 등록돼 있어도 ECU에 연결됐다는 뜻은 아니야.",true);
  // Live, read-only telemetry. Never treat paired devices as active ECU data.
  LinearLayout livePanel=a.col();
  livePanel.setPadding(a.dp(14),a.dp(11),a.dp(14),a.dp(11));
  livePanel.setBackground(a.shape(a.panel,a.line,14));
  a.label(livePanel,"실시간 연결 상태 · 화면 열려 있을 때만 확인",13,a.fg,true);
  android.widget.TextView obdLive=a.text("",13,a.muted,false);
  android.widget.TextView cardoLive=a.text("",13,a.muted,false);
  android.widget.TextView gpsLive=a.text("",13,a.muted,false);
  livePanel.addView(obdLive);livePanel.addView(cardoLive);livePanel.addView(gpsLive);
  a.gap(p,8);p.addView(livePanel);

  a.gap(p,10);
  a.action(p,mode==ConnectionSetupMode.Mode.NONE?"자동기록 방식 먼저 선택":
   "기록 방식: "+mode.label+" · 변경하기",mode==ConnectionSetupMode.Mode.NONE,
   ()->chooseMode(bikeId));
  a.gap(p,14);
  a.label(p,"지금 할 일",17,a.fg,true);
  if(anotherBikeActive){
   a.label(p,"다른 바이크의 기록이 진행 중이야. 기록 종료 후 연결을 바꿔.",13,a.muted,false);
   a.action(p,"진행 중인 기록 상태 보기",true,()->showConnectionStatus(RideService.activeRideBikeId));
  }else if(mode==ConnectionSetupMode.Mode.NONE){
   a.label(p,"어떤 방식으로 자동기록할지 먼저 골라. 필요한 단계만 안내할게.",13,a.muted,false);
   a.action(p,"자동기록 방식 선택",true,()->chooseMode(bikeId));
  }else if(wantsObd&&!obd){
   a.label(p,"OBD 동글이 필요해. 바이크 키 ON → 갤럭시 Bluetooth 페어링 → 동글 등록.",13,a.muted,false);
   a.action(p,"OBD 동글 등록",true,()->pick(bikeId));
  }else if(wantsCardo&&!cardo){
   a.label(p,"Freecom 2X를 켜고 휴대폰과 페어링한 뒤 등록해.",13,a.muted,false);
   a.action(p,"카르도 등록",true,()->pickCardo(bikeId));
  }else if(mode==ConnectionSetupMode.Mode.CARDO_GPS&&!fallback){
   a.label(p,"GPS로 출발을 감지하려면 위치 사용과 오인 가능성에 별도로 동의해야 해.",13,a.muted,false);
   a.action(p,"GPS 안전망 동의하기",true,()->changeGpsFallback(bikeId,false));
  }else if(!permissions){
   a.label(p,"기기 설정은 끝났어. 자동기록 시작에 필요한 권한만 확인하면 돼.",13,a.muted,false);
   a.action(p,"자동기록 권한 확인하기",true,this::showPermissionHelp);
  }else if(active){
   a.label(p,gpsRecording?"GPS 경로가 저장 중이야. 주행 중 설정은 변경하지 마.":
    "현재 출발 감지 상태를 확인할 수 있어. ECU 데이터가 실제 수신 중인지 구별해.",13,a.muted,false);
   a.action(p,"현재 연결 상태 확인",true,()->showConnectionStatus(bikeId));
  }else if(mode==ConnectionSetupMode.Mode.OBD_ONLY){
   a.label(p,"OBD를 상시 감시하고 다음 출발을 기다리는 방식이야. 지속 알림과 배터리를 사용해.",13,a.muted,false);
   a.action(p,"OBD 자동기록 대기 시작",true,()->beginAuto(bikeId));
  }else if(mode==ConnectionSetupMode.Mode.CARDO_OBD){
   a.label(p,"카르도를 휴대폰에 연결한 뒤 출발하면 OBD 속도로 시작을 감지해. 연결이 막히면 알림을 확인해.",13,a.muted,false);
   a.action(p,"OBD 실제 센서 검사 (정차)",true,()->DeveloperObdScreen.launchQuick(a));
   a.action(p,"현재 카르도 연결 감지 시작 시험",false,()->CardoRideStarter.startFromTap(a,false));
  }else{
   a.label(p,"Freecom 2X 연결을 감지한 뒤 GPS 이동으로 출발을 판정해. Android가 막으면 알림에서 시작해.",13,a.muted,false);
   a.action(p,"정차 상태에서 자동 감지 시험",true,()->CardoRideStarter.startFromTap(a,false));
   a.action(p,"연결 상태 다시 확인",false,()->open(bikeId));
  }

  a.gap(p,12);
  a.label(p,"설정 순서",16,a.fg,true);
  step(p,"01","바이크",bike.optString("name","내 바이크")+" 선택됨",
   "이 화면에서 설정한 연결은 이 바이크에만 적용돼.",true);
  step(p,"02","OBD 동글",!wantsObd?"이번 방식에 필요 없음 · 건너뛰기":obd?
   liveObd?"실제 ECU 속도 수신 확인":"등록 완료 · 실제 ECU 연결은 미확인":
   "미등록 · 카르도+GPS만 쓴다면 건너뛰어도 돼",
   obd?"키 ON 상태에서 읽기 전용 연결 검사로 확인할 수 있어.":
   "동글 장착 → 바이크 키 ON → 휴대폰 Bluetooth 페어링 → 앱에 등록.",!wantsObd||obd);
  step(p,"03","카르도",!wantsCardo?"이번 방식에 필요 없음 · 건너뛰기":cardo?
   CardoAutoRidePrefs.recentlyConnected(a)?"최근 연결 감지 · 자동기록 장치 등록됨":
   "등록됨 · 다음 Bluetooth 연결 대기":
   "미등록 · OBD 상시 대기만 사용한다면 생략 가능",
   "Freecom 2X 전원 ON → 갤럭시에서 연결 → 앱에 기기 등록.",!wantsCardo||cardo);
  step(p,"04","자동기록",mode==ConnectionSetupMode.Mode.NONE?"방식을 먼저 선택":gpsRecording?"GPS 기록 중":active?"출발 감시 작동 중":
   armed?"카르도 연결 감지 사용 · 실제 자동시작은 미확인":
   obdOnly?"OBD 상시 대기 동의 저장됨":"아직 자동기록 방법을 선택하지 않았어",
   cardo?(fallback?"GPS 안전망도 켜짐 · OBD 응답이 없으면 GPS 이동을 검증.":
     "GPS 안전망은 꺼져 있어. OBD가 응답하지 않으면 자동시작이 안 될 수 있어."):
    "카르도 방식은 배터리 부담이 적어. OBD 상시 대기는 지속 알림·전력을 사용해.",
   active||(mode==ConnectionSetupMode.Mode.OBD_ONLY?obdOnly:
    mode==ConnectionSetupMode.Mode.CARDO_GPS?cardo&&fallback:cardo&&obd));

  a.gap(p,12);
  a.label(p,FirstRideSetupProof.summary(a,bikeId),13,a.muted,false);
  a.action(p,"블루투스 페어링 설정 열기",false,()->openBluetoothSettings());
  a.action(p,"연결 문제 진단 · 지금 해결하기",false,()->showConnectionHelp(bikeId));
  a.divider(p);
  a.action(p,"고급 설정 · ODO / 센서 / 로그 / 해제",false,()->openAdvanced(bikeId));
  a.show();
  startLiveStatus(bikeId,a.dialog,obdLive,cardoLive,gpsLive);
 }

 private void startLiveStatus(String bikeId,android.app.Dialog owner,
   android.widget.TextView obdLive,android.widget.TextView cardoLive,android.widget.TextView gpsLive){
  android.os.Handler main=new android.os.Handler(android.os.Looper.getMainLooper());
  main.post(new Runnable(){@Override public void run(){
   if(a.isFinishing()||owner==null||!owner.isShowing()||a.dialog!=owner)return;
   boolean current=bikeId.equals(RideService.activeRideBikeId);
   boolean ecu=current&&RideService.autoArmed&&RideService.obdSpeedKmh>=0&&
     ObdTelemetryFreshness.speedFresh(RideService.obdLastSampleElapsed,android.os.SystemClock.elapsedRealtime());
   boolean hasObd=!BikeObdPreferences.address(a,bikeId).isEmpty();
   obdLive.setText(!hasObd?"OBD: 이 바이크에 등록되지 않음":
     ecu?"OBD: 실제 ECU 속도 수신 중":
     current&&RideService.autoArmed?"OBD: 연결 대기/ECU 속도 미수신":
     "OBD: 동글 등록됨 · 현재 ECU 응답 확인 전");
   gpsLive.setText(current&&RideService.running?
     RideService.gpsFresh()?"GPS: 실제 좌표 수신 및 기록 중":
       "GPS: 기록 서비스 작동 · 최신 좌표 미수신":
     "GPS: 라이딩 좌표 기록 시작 전");
   if(!CardoAutoRidePrefs.forBike(a,bikeId)){
    cardoLive.setText("카르도: 미등록 · OBD 전용이면 정상");
   }else{
    final String target=CardoAutoRidePrefs.address(a);
    cardoLive.setText("카르도: 현재 Bluetooth 오디오 연결 확인 중…");
    CardoConnectionProbe.probe(a,target,result->a.runOnUiThread(()->{
     if(!owner.isShowing()||a.dialog!=owner)return;
     switch(result){
      case CONNECTED:cardoLive.setText("카르도: 현재 연결 확인됨 (Android 오디오 프로필)");break;
      case DISCONNECTED:cardoLive.setText("카르도: 현재 오디오 연결 확인 안 됨 · 페어링 상태와 구별");break;
      case PERMISSION:cardoLive.setText("카르도: 주변 기기 권한 필요");break;
      case BLUETOOTH_OFF:cardoLive.setText("카르도: 휴대폰 Bluetooth 꺼짐");break;
      default:cardoLive.setText("카르도: Android 연결 상태 확인 불가 · 연결 감지 알림 확인");
     }
    }));
   }
   // Screen-scoped and bounded. Closing this dialog stops future queries.
   main.postDelayed(this,8_000L);
  }});
 }

 private boolean setupPermissionsOk(){
  if(a.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)return false;
  if(Build.VERSION.SDK_INT>=31&&a.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)return false;
  if(Build.VERSION.SDK_INT>=33&&a.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return false;
  LocationManager lm=a.getSystemService(LocationManager.class);
  return lm!=null&&lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
 }
 private void step(LinearLayout parent,String number,String title,String state,String hint,boolean ready){
  LinearLayout panel=a.col();
  panel.setPadding(a.dp(15),a.dp(13),a.dp(15),a.dp(12));
  panel.setBackground(a.shape(a.panel,a.line,16));
  a.label(panel,number+"   "+title+(ready?"   ✓":""),16,a.fg,true);
  a.label(panel,state,14,ready?a.fg:a.muted,true);
  a.label(panel,hint,12,a.muted,false);
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
  lp.bottomMargin=a.dp(9);parent.addView(panel,lp);
 }
 private void panel(LinearLayout parent,String title,String message,String hint,boolean highlight){
  LinearLayout box=a.col();box.setPadding(a.dp(16),a.dp(15),a.dp(16),a.dp(14));
  box.setBackground(a.shape(a.panel,highlight?a.accent:a.line,16));
  a.label(box,title,12,a.muted,true);
  a.label(box,message,17,a.fg,true);
  a.label(box,hint,12,a.muted,false);
  parent.addView(box,new LinearLayout.LayoutParams(-1,-2));
 }
 private void chooseMode(String bikeId){
  if(RideService.running||RideService.autoArmed||RideService.saving()||
     RestoreRideGate.isRestoring()||RestoreRideGate.isFormWriting()){
   a.toast("라이딩과 자동 감지를 종료한 뒤 방식 변경을 할 수 있어.");return;
  }
  ConnectionSetupMode.Mode[] all={
   ConnectionSetupMode.Mode.CARDO_OBD,
   ConnectionSetupMode.Mode.OBD_ONLY,
   ConnectionSetupMode.Mode.CARDO_GPS};
  String[] choices={
   "카르도 + OBD · 추천\n헬멧 연결 → 실제 ECU 속도로 출발 확인",
   "OBD만 사용\n동글 연결을 계속 감시 · 배터리 추가 사용",
   "카르도 + GPS\nOBD 없이 GPS 이동 검증 · 배터리/오인 주의"};
  new AlertDialog.Builder(a).setTitle("라이딩을 어떻게 자동 기록할까?")
   .setItems(choices,(d,index)->{
    ConnectionSetupMode.Mode selected=all[index];
    ConnectionSetupMode.Mode previous=ConnectionSetupMode.get(a,bikeId);
    boolean revokeCardo=selected==ConnectionSetupMode.Mode.OBD_ONLY&&CardoAutoRidePrefs.forBike(a,bikeId);
    boolean revokeObd=selected!=ConnectionSetupMode.Mode.OBD_ONLY&&BikeObdPreferences.autoRideEnabled(a,bikeId);
    boolean revokeGps=selected==ConnectionSetupMode.Mode.CARDO_OBD&&BikeObdPreferences.gpsFallbackEnabled(a,bikeId);
    String warning=(revokeCardo?"\n• 기존 카르도 자동감지와 GPS 안전망이 해제돼.":"")+
      (revokeObd?"\n• OBD 상시 대기 설정은 꺼져.":"")+
      (revokeGps?"\n• 기존 GPS 안전망은 꺼져.":"");
    if(warning.isEmpty()){ConnectionSetupMode.set(a,bikeId,selected);open(bikeId);return;}
    new AlertDialog.Builder(a).setTitle("방식 변경 시 설정 확인")
     .setMessage("기록 방식: "+previous.label+" → "+selected.label+warning+
      "\n\n기존 GPS 파일·정비·계기판 ODO는 변경되지 않아.")
     .setNegativeButton("취소",null)
     .setPositiveButton("설정 변경",(confirm,which)->{
      if(RideService.running||RideService.autoArmed)return;
      if(revokeCardo)CardoAutoRidePrefs.disable(a);
      if(revokeObd)BikeObdPreferences.setAutoRideEnabled(a,bikeId,false);
      if(revokeGps)BikeObdPreferences.setGpsFallbackEnabled(a,bikeId,false);
      ConnectionSetupMode.set(a,bikeId,selected);open(bikeId);
     }).show();
   }).setNegativeButton("취소",null).show();
 }

 void confirmDeviceRegistration(String bikeId,String mac,String name,boolean ecuVerified){
  if(a.readOnly||RideService.running||RideService.autoArmed||RideService.saving()||
     RestoreRideGate.isRestoring()||RestoreRideGate.isFormWriting()){
   a.toast("연결 감지와 주행·복원 종료 후 동글을 바꿀 수 있어.");return;
  }
  if(!RideBikeBinding.valid(bikeId)||!BikeObdPreferences.validAddress(mac)||
     a.bike()==null||!bikeId.equals(a.bike().optString("id"))){
   a.toast("현재 바이크를 다시 선택한 뒤 등록해.");return;
  }
  String old=BikeObdPreferences.address(a,bikeId);
  boolean changing=!mac.equalsIgnoreCase(old);
  boolean hadCardo=changing&&CardoAutoRidePrefs.forBike(a,bikeId);
  boolean hadAuto=changing&&BikeObdPreferences.autoRideEnabled(a,bikeId);
  boolean hadGps=changing&&BikeObdPreferences.gpsFallbackEnabled(a,bikeId);
  String warning=(hadCardo?"\n• 카르도 자동감지를 다시 등록해야 해.":"")+
    (hadGps?"\n• GPS 안전망도 다시 동의해야 해.":"")+
    (hadAuto?"\n• OBD 상시 대기 설정이 해제돼.":"");
  new AlertDialog.Builder(a).setTitle(ecuVerified?"ECU 확인한 동글 등록":"이 바이크에 OBD 동글 등록")
   .setMessage((name==null?"OBD 동글":name)+
     "\n\n"+(ecuVerified?"실제 ECU 센서값 수신 확인됨.":"Bluetooth 장치만 선택함 · ECU 연결은 아직 미확인.")+
     (changing&&old.isEmpty()?"\n처음 등록하는 동글이야.":"\n기존 동글을 변경하는 중이야.")+
     warning+"\n\nGPS 기록·정비 내역·ODO는 그대로 보존돼.")
   .setNegativeButton("취소",null)
   .setPositiveButton(warning.isEmpty()?"이 동글 등록":"해제 내용 확인 · 등록",(d,w)->{
    if(RideService.running||RideService.autoArmed||a.bike()==null||
       !bikeId.equals(a.bike().optString("id")))return;
    BikeObdPreferences.setDevice(a,bikeId,mac,name);
    open(bikeId);
    if(hadCardo)new AlertDialog.Builder(a).setTitle("카르도 설정을 다시 연결해")
      .setMessage("새 OBD 동글 등록으로 이전 카르도 자동감지와 GPS 안전망 동의가 해제됐어. Freecom 2X를 다시 등록하면 출발 감지를 재개할 수 있어.")
      .setNegativeButton("나중에",null)
      .setPositiveButton("카르도 다시 등록",(dialog,which)->pickCardo(bikeId)).show();
   }).show();
 }

 private void openBluetoothSettings(){
  try{a.startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));}
  catch(Exception e){a.toast("갤럭시 설정 → 연결 → Bluetooth에서 OBD 또는 Freecom 2X를 페어링해.");}
 }
 private void showPermissionHelp(){
  new AlertDialog.Builder(a).setTitle("자동기록 권한 확인")
   .setMessage("1. 갤럭시 설정 → 앱 → MOTO HUB → 권한\n2. 위치: 정확한 위치 허용\n3. 주변 기기와 알림 허용\n4. 휴대폰 위치 기능 켜기\n\n화면이 꺼져도 카르도로 시작하려면 위치 '항상 허용'이 필요할 수 있어. Android가 허용하지 않으면 알림으로 직접 시작할 수 있어.")
   .setNegativeButton("닫기",null)
   .setPositiveButton("앱 권한 설정 열기",(d,w)->a.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
     android.net.Uri.parse("package:"+a.getPackageName())))).show();
 }
 private void showConnectionStatus(String bikeId){
  new AlertDialog.Builder(a).setTitle("연결 상태 · 실제 수신 기준")
   .setMessage("바이크: "+a.bikeName(bikeId)+"\nOBD: "+RideService.obdLink+
    "\nECU 속도 수신: "+(RideService.autoArmed&&bikeId.equals(RideService.activeRideBikeId)&&
      ObdTelemetryFreshness.speedFresh(RideService.obdLastSampleElapsed,android.os.SystemClock.elapsedRealtime())?
       "최근 응답 있음":"최근 응답 확인 안 됨")+
    "\nGPS 기록: "+(RideService.running?"서비스 작동 중":"시작 안 됨")+
    "\nGPS 좌표: "+(RideService.gpsFresh()?"최근 수신":"대기 또는 오래된 신호")+
    "\n\n센서 미수신을 연결 성공으로 표시하지 않아.")
   .setPositiveButton("닫기",null).show();
 }
 private void showConnectionHelp(String bikeId){
  new AlertDialog.Builder(a).setTitle("연결이 안 될 때")
   .setMessage("① OBD 동글 목록에 안 보임\n바이크 키 ON → 갤럭시 Bluetooth에서 먼저 페어링 → 앱에 돌아와 동글 선택.\n\n② 동글은 등록했는데 속도/RPM 안 나옴\n바이크 정차 후 시동 ON → 'OBD 실제 연결 검사'. 동글은 Bluetooth Classic SPP를 지원해야 해. ECU가 PID를 지원하지 않을 수도 있어.\n\n③ 카르도 연결해도 자동기록 안 됨\nFreecom 2X를 휴대폰과 연결했는지, 카르도 등록·위치/알림 권한을 확인해. Android 백그라운드 제한 시 시작 알림을 눌러.\n\n④ OBD 없이 쓰려면\n카르도 등록 후 GPS 안전망을 별도 활성화해. 보행/다른 차량 이동도 오인할 수 있어.\n\n⑤ 자동기록은 켜졌는데 경로가 없음\n라이딩 화면에서 GPS 좌표 수신을 확인해. 계기판 누적거리(ODO)와 GPS 기록 거리는 별개야.")
   .setNegativeButton("닫기",null)
   .setPositiveButton("OBD 검사",(d,w)->DeveloperObdScreen.launchQuick(a))
   .show();
 }
 private void changeGpsFallback(String bikeId,boolean enabled){
  if(!CardoAutoRidePrefs.forBike(a,bikeId)){
   a.toast("먼저 카르도를 등록해.");return;
  }
  if(RideService.autoArmed||RideService.running||RideService.saving()){
   a.toast("주행과 연결 감지를 종료한 뒤 설정을 바꿀 수 있어.");return;
  }
  new AlertDialog.Builder(a).setTitle(enabled?"GPS 안전망 끄기":"GPS 안전망 켜기")
   .setMessage(enabled?"GPS로 출발을 감지하는 기능을 끌까? 기존 기록은 그대로 보존돼.":
    "OBD 속도가 수신되지 않을 때 카르도 연결 후 GPS 이동으로 출발을 판정해. 배터리를 더 쓰고, 도보·다른 차량 이동을 오인할 수 있어. 계기판 ODO에는 자동 반영하지 않아.")
   .setNegativeButton("취소",null)
   .setPositiveButton(enabled?"끄기":"동의하고 켜기",(d,w)->{
    BikeObdPreferences.setGpsFallbackEnabled(a,bikeId,!enabled);open(bikeId);
   }).show();
 }

 void openAdvanced(String bikeId){
  if(!RideBikeBinding.valid(bikeId)){a.toast("바이크를 먼저 등록해 주세요.");return;}
  org.json.JSONObject bike=null;
  for(org.json.JSONObject entry:a.items("vehicles"))if(bikeId.equals(entry.optString("id"))){bike=entry;break;}
  if(bike==null){a.toast("등록된 바이크를 찾을 수 없습니다.");return;}
  String name=bike.optString("name","내 바이크");
  String address=BikeObdPreferences.address(a,bikeId);
  String adapter=BikeObdPreferences.deviceName(a,bikeId);
  List<String> selected=BikeObdPreferences.selectedPids(a,bikeId);
  LinearLayout p=a.page("고급 연결 · OBD / 카르도");
  a.action(p,"← 쉬운 연결 화면으로 돌아가기",false,()->open(bikeId));
  a.label(p,name,23,a.fg,true);
  a.label(p,"이 바이크에서 사용할 진단 동글과 표시 센서를 설정합니다. 다른 바이크의 설정과 섞이지 않습니다.",14,a.muted,false);
  a.gap(p,12);
  a.label(p,"연결할 OBD 동글",16,a.fg,true);
  a.label(p,address.isEmpty()?"미등록 · Bluetooth에서 페어링 후 선택":
    "등록됨 · "+(adapter.isEmpty()?"OBD2 동글":adapter)+"\nBluetooth "+address,14,a.fg,false);
  a.action(p,"갤럭시 Bluetooth 페어링 설정",false,()->{
   try{a.startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));}
   catch(Exception unavailable){a.toast("갤럭시 설정 → 연결 → Bluetooth에서 직접 페어링해 주세요.");}
  });
  a.action(p,address.isEmpty()?"이 바이크의 OBD 동글 등록":"이 바이크의 OBD 동글 변경",true,()->pick(bikeId));
  a.gap(p,12);
  a.label(p,"마지막 자동기록 진행: "+ObdAutoRideProgress.summary(a,bikeId),13,a.muted,false);
  a.label(p,RidePowerAudit.summary(a,bikeId),13,a.muted,false);
  a.label(p,"OBD 진단은 바이크별·동글별로 분리되며 MAC 주소는 진단 로그에 남기지 않습니다.",12,a.muted,false);
  a.label(p,"라이딩 기록 자동 연동",16,a.fg,true);
  boolean autoEnabled=BikeObdPreferences.autoRideEnabled(a,bikeId);
  a.label(p,autoEnabled?(RideService.autoArmed&&bikeId.equals(RideService.activeRideBikeId)?
    "상시 대기 중 · 시동 OFF 뒤에도 다음 연결을 감시합니다":
    "상시 대기 동의 저장됨 · 앱을 다시 열면 연결 감지 재개"):
    "상시 대기 꺼짐 · 한 번 켜면 주차 뒤에도 다음 시동을 감지",14,a.muted,false);
  if(RideService.autoArmed&&bikeId.equals(RideService.activeRideBikeId))
   a.label(p,"지금 이 바이크의 OBD 자동 출발 감지 진행 중 · "+RideService.obdLink,14,a.fg,false);
  else if(RideService.autoArmed||RideService.running)
   a.label(p,"다른 라이딩이 진행 중입니다. 해당 기록을 종료한 뒤 설정하세요.",13,a.muted,false);
  if(address.isEmpty())a.label(p,"바이크별 동글을 먼저 등록하면 자동 라이딩을 켤 수 있습니다.",13,a.muted,false);
  else{
   a.action(p,autoEnabled?"OBD 상시 대기 다시 시작":"OBD 시동 자동 연결 · 상시 대기 켜기",true,()->beginAuto(bikeId));
   if(autoEnabled)a.action(p,"이 바이크 OBD 자동 기록 해제",false,()->disableAuto(bikeId));
  }
  if(RideService.autoArmed&&bikeId.equals(RideService.activeRideBikeId))
   a.action(p,"현재 OBD 감지만 중지 · GPS 기록 보존",false,()->{
    a.startService(new Intent(a,RideService.class).setAction(RideService.AUTO_DISARM));
    a.toast("OBD 감지 중지 요청 · 이미 시작된 GPS 기록은 유지됩니다.");
   });

  a.gap(p,12);
  a.action(p,"OBD 읽기 전용 실시간 계기판",false,()->{
   if(!RideService.autoArmed||!bikeId.equals(RideService.activeRideBikeId)){
    a.toast("이 바이크의 OBD 자동 감지를 먼저 시작해 주세요.");return;
   }
   a.startActivity(new Intent(a,ObdDashboardActivity.class));
  });
  a.label(p,"카르도 자동 라이딩 · 저전력",16,a.fg,true);
  boolean cardoHere=CardoAutoRidePrefs.forBike(a,bikeId);
  a.label(p,cardoHere?
   "켜짐 · "+CardoAutoRidePrefs.name(a)+" · "+CardoAutoRidePrefs.status(a):
   "꺼짐 · 카르도 연결 후 OBD 또는 GPS 안전망으로 출발 감지",14,a.muted,false);
  a.action(p,cardoHere?"카르도 자동기록 장치 변경":"카르도 자동기록 켜기 · 기기 선택",
    false,()->pickCardo(bikeId));
  if(cardoHere){
   a.action(p,"카르도 자동기록 시작 테스트 (정차)",false,()->CardoRideStarter.startFromTap(a,false));
   a.action(p,"카르도 자동기록 끄기",false,()->new AlertDialog.Builder(a)
    .setTitle("카르도 자동기록 해제")
    .setMessage("연결 감지만 해제합니다. 기존 라이딩 GPS 파일과 차고 기록은 유지합니다.")
    .setNegativeButton("취소",null)
    .setPositiveButton("해제",(d,w)->{
     CardoCompanionPairing.stopObserving(a);
     CardoAutoRidePrefs.disable(a);
     CardoRideStarter.cancelTap(a);
     if(RideService.autoArmed&&bikeId.equals(RideService.activeRideBikeId))
      a.startService(new Intent(a,RideService.class).setAction(RideService.CARDO_DISCONNECTED));
     open(bikeId);
    }).show());
  }
  if(cardoHere){
   boolean gpsBackup=BikeObdPreferences.gpsFallbackEnabled(a,bikeId);
   a.label(p,gpsBackup?
    "GPS 안전망 켜짐 · ECU 속도 미수신 시 검증된 이동으로 기록 시작":
    "GPS 안전망 꺼짐 · ECU 속도 미수신 시 자동기록을 시작할 수 없음",13,a.muted,false);
   a.action(p,gpsBackup?"GPS 자동기록 안전망 끄기":"OBD 미지원 대비 GPS 안전망 켜기",false,()->{
    if(RideService.running||RideService.autoArmed||RideService.saving()){
     a.toast("현재 라이딩과 OBD 감지를 종료한 후 변경해 주세요.");return;
    }
    new AlertDialog.Builder(a).setTitle(gpsBackup?"GPS 안전망 해제":"카르도 GPS 안전망 · 추가 배터리 사용")
     .setMessage(gpsBackup?
      "GPS 이동 감지를 끕니다. 기존 주행 기록과 정비 내역은 보존합니다.":
      "카르도 GPS 안전망을 켜면 OBD 동글 없이도 최대 15분간 출발을 감시합니다. 출발 판정 전에 수신한 정확한 GPS 좌표도 약 45초 보존합니다. GPS 센서는 추가 전력을 사용하므로 기본적으로 꺼져 있습니다. 보행이나 다른 차량 이동을 바이크 주행이라고 오인할 수 있어 계기판 ODO에는 반영하지 않습니다.")
     .setNegativeButton("취소",null)
     .setPositiveButton(gpsBackup?"안전망 끄기":"동의하고 켜기",(dialog,which)->{
      BikeObdPreferences.setGpsFallbackEnabled(a,bikeId,!gpsBackup);
      open(bikeId);
     }).show();
   });
  }
  a.label(p,"카르도 Bluetooth 연결 이벤트만 감지합니다. 밤새 OBD 재연결을 반복하지 않습니다. Android 동반 기기 승인을 하면 앱이 닫혀도 연결 감지를 지원할 수 있습니다. Android 14+에서 화면이 꺼진 채 완전 자동 GPS 시작은 '위치 항상 허용' 등 시스템 권한이 필요하며 기기 정책에 따라 막힐 수 있습니다. 막히면 알림으로 시작합니다.",12,a.muted,false);
  a.action(p,"백그라운드 위치·배터리 권한 확인",false,()->{
   new AlertDialog.Builder(a).setTitle("카르도 자동시작 권한")
    .setMessage("설정 → 앱 → MOTO HUB → 권한 → 위치를 '항상 허용'으로 선택하면 Android 백그라운드 서비스 조건 충족에 도움이 됩니다. 정확한 위치·주변 기기·알림도 허용해 주세요. 항상 허용은 개인 위치 권한을 넓히므로 원치 않으면 알림을 눌러 시작하는 방식으로 사용하세요.")
    .setNegativeButton("취소",null)
    .setPositiveButton("이 앱 권한 설정",(d,w)->a.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
      android.net.Uri.parse("package:"+a.getPackageName())))).show();
  });
  a.gap(p,12);
  a.label(p,"계기판 총주행거리(ODO) 연동",16,a.fg,true);
  boolean odoEnabled=OdometerAutoSyncInbox.enabled(a,bikeId);
  a.label(p,odoEnabled?OdometerAutoSyncInbox.display(a,bikeId):
   "꺼짐 · ECU 표준 01A6 지원 여부는 실차 확인 필요",14,a.muted,false);
  a.action(p,odoEnabled?"이 바이크 ECU 누적거리 자동 동기화 끄기":
    "이 바이크 ECU 누적거리 자동 동기화 설정",false,()->confirmOdo(bikeId));
  a.label(p,"GPS 경로와 이동거리는 항상 바이크별로 기록합니다. 실제 계기판 총주행거리 자동 갱신은 ECU가 01A6을 지원하고 최초 계기판 km를 확인한 경우에만 가능합니다. 지원하지 않으면 라이딩 기록에서 검증 후 직접 반영할 수 있습니다.",13,a.muted,false);
  a.gap(p,12);
  a.label(p,"선택된 계기판 센서",16,a.fg,true);
  a.label(p,selected.isEmpty()?"아직 선별한 센서 없음 · 전체 탐색 후 선택":
   selected.size()+"개 저장 · "+preview(selected),14,a.muted,false);
  a.action(p,"OBD 연결 검사 · 전체 센서 탐색",false,()->{
   if(RideService.running||RideService.autoArmed||RideService.saving()){
    a.toast("주행/자동 감지를 종료하고 정차 상태에서 실행해 주세요.");return;
   }
   DeveloperObdScreen.launchQuick(a);
  });
  if(!selected.isEmpty())a.action(p,"이 바이크의 센서 선택만 초기화",false,()->
   new AlertDialog.Builder(a).setTitle("표시 센서 초기화")
    .setMessage("이 바이크의 PID 선택만 지웁니다. 다른 바이크와 기존 라이딩·정비 기록은 유지됩니다.")
    .setNegativeButton("취소",null)
    .setPositiveButton("초기화",(d,w)->{BikeObdPreferences.clearSelectedPids(a,bikeId);open(bikeId);}).show());
  if(!address.isEmpty())a.action(p,"이 바이크의 OBD 동글 등록 해제",false,()->
   new AlertDialog.Builder(a).setTitle("OBD 동글 등록 해제")
    .setMessage("이 바이크에 저장된 동글 연결 정보만 삭제합니다. Android Bluetooth 페어링과 다른 바이크 설정은 유지됩니다.")
    .setNegativeButton("취소",null)
    .setPositiveButton("등록 해제",(d,w)->{BikeObdPreferences.clearDevice(a,bikeId);open(bikeId);}).show());
  a.gap(p,12);
  a.label(p,"상시 대기를 한 번 켜면 알림과 함께 OBD Bluetooth 연결을 유지하거나 60초 간격으로 재시도합니다. 시동 OFF 후 주행 기록은 저장하지만 연결 대기는 계속됩니다. 앱을 다시 열면 동의한 바이크에 한해 대기를 재개합니다. 휴대폰 강제종료·재부팅 후 자동 시작은 보장하지 않습니다. 지속 알림과 배터리 소모가 발생합니다. 필요 없으면 반드시 상시 대기를 해제하세요.",13,a.muted,false);
  a.show();
 }

 private static String preview(List<String> pids){
  StringBuilder out=new StringBuilder();
  for(int i=0;i<Math.min(8,pids.size());i++){
   if(i>0)out.append(" · ");
   out.append(pids.get(i));
  }
  if(pids.size()>8)out.append(" 외 ").append(pids.size()-8).append("개");
  return out.toString();
 }



 private void pickCardo(String bikeId){
  if(RideService.autoArmed||RideService.running||RideService.saving()){
   a.toast("현재 주행·OBD 감지를 종료한 다음 설정하세요.");return;
  }
  if(a.readOnly||RestoreRideGate.isRestoring()||RestoreRideGate.isFormWriting())return;
  if(Build.VERSION.SDK_INT>=31&&
     a.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){
   a.requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},101);
   a.toast("주변 기기 권한 허용 후 다시 선택해 주세요.");return;
  }
  BluetoothManager manager=(BluetoothManager)a.getSystemService(Context.BLUETOOTH_SERVICE);
  BluetoothAdapter adapter=manager==null?null:manager.getAdapter();
  if(adapter==null||!adapter.isEnabled()){a.toast("Bluetooth를 켜고 카르도부터 페어링해 주세요.");return;}
  try{
   List<BluetoothDevice> bonded=new ArrayList<>(adapter.getBondedDevices());
   bonded.sort(Comparator.comparingInt((BluetoothDevice d)->{
     String n=d.getName();
     return n!=null&&(n.toLowerCase(java.util.Locale.ROOT).contains("cardo")||
       n.toLowerCase(java.util.Locale.ROOT).contains("freecom"))?0:1;
   }));
   if(bonded.isEmpty()){a.toast("페어링된 기기가 없습니다. 카르도를 먼저 Bluetooth에 등록하세요.");return;}
   String[] names=new String[bonded.size()];
   for(int i=0;i<bonded.size();i++){
    String name=bonded.get(i).getName();
    String mac=bonded.get(i).getAddress();
    boolean cardoName=name!=null&&(name.toLowerCase(java.util.Locale.ROOT).contains("cardo")||
     name.toLowerCase(java.util.Locale.ROOT).contains("freecom")||
     name.toLowerCase(java.util.Locale.ROOT).contains("packtalk"));
    names[i]=(cardoName?"카르도 추천 · ":"")+(name==null?"이름 없는 기기":name)+
     " · 끝번호 "+mac.substring(mac.length()-5);
   }
   a.toast("Freecom 2X를 선택해. OBD 동글은 선택하면 안 돼.");
   new AlertDialog.Builder(a).setTitle("카르도 연결 감지 기기 선택")
    .setItems(names,(d,index)->{
     BluetoothDevice selected=bonded.get(index);
     String mac=selected.getAddress(),name=selected.getName();
     String lowered=name==null?"":name.toLowerCase(java.util.Locale.ROOT);
     if(!BikeObdPreferences.validAddress(mac)||
        mac.equalsIgnoreCase(BikeObdPreferences.address(a,bikeId))||
        lowered.contains("obd")||lowered.contains("vgate")||
        lowered.contains("elm327")||lowered.contains("icar")){
      a.toast("OBD 동글은 카르도가 아니야. Freecom 2X 기기를 골라.");return;
     }
     new AlertDialog.Builder(a).setTitle("카르도 자동 라이딩 활성화")
      .setMessage((name==null?"선택 장치":name)+
       "\n\n휴대폰에 이 카르도가 연결되면 자동으로 출발을 감지하도록 등록해.\n"+
       "OBD가 없거나 속도를 못 읽으면 GPS 안전망을 따로 켜야 해.\n\n"+
       "Android가 자동시작을 제한하면 알림을 눌러 시작할 수 있어.")
      .setNegativeButton("취소",null)
      .setPositiveButton("동의 · 카르도 등록",(dd,w)->{
       if(!CardoAutoRidePrefs.enable(a,bikeId,mac,name)){
        a.toast("카르도 주소·바이크 설정 확인 필요");return;
       }
       a.toast("카르도 등록됨. 시스템 연결 감지를 승인하면 자동 출발을 감지할 수 있어.");
       open(bikeId);
       CardoCompanionPairing.associate(a,mac);
      }).show();
    }).setNegativeButton("취소",null).show();
  }catch(SecurityException e){a.toast("Bluetooth 권한을 확인해 주세요.");}
 }
 private void confirmOdo(String bikeId){
  if(a.readOnly||RideService.running||RideService.autoArmed||
     RideService.saving()||RestoreRideGate.isRestoring()||RestoreRideGate.isFormWriting()){
   a.toast("라이딩·자동 감지·데이터 복원이 끝난 뒤 ODO 설정을 변경해 주세요.");return;
  }
  if(a.bike()==null||!bikeId.equals(a.bike().optString("id"))){
   a.toast("현재 선택한 바이크를 다시 확인해 주세요.");return;
  }
  if(OdometerAutoSyncInbox.enabled(a,bikeId)){
   new AlertDialog.Builder(a).setTitle("ECU 누적거리 자동 동기화 끄기")
    .setMessage("이 바이크의 ECU ODO 자동 적용만 중지합니다. 기존 주행거리·GPS·정비 기록은 변경하지 않습니다.")
    .setNegativeButton("취소",null)
    .setPositiveButton("끄기",(dialog,which)->{
     OdometerAutoSyncInbox.disable(a,bikeId);
     open(bikeId);
    }).show();
   return;
  }
  double baseline=a.bike().optDouble("odo",Double.NaN);
  if(!OdoAutoSyncPolicy.sane(baseline)){
   a.toast("바이크 정보에 실제 계기판 총주행거리를 먼저 입력해 주세요.");return;
  }
  new AlertDialog.Builder(a).setTitle("이 바이크 ECU 총주행거리 동기화")
   .setMessage("바이크: "+a.bikeName(bikeId)+
    "\n차고 총주행거리: "+a.num(baseline)+" km\n\n"+
    "지금 바이크의 실제 계기판 누적 km와 차고 값이 정확히 일치하는지 확인했나요?"+
    "\n\nECU 표준 PID 01A6을 지원할 때만 두 번 독립 조회한 값을 비교하고, 최초 오차 및 급증 여부를 검증한 후 차고 km에 반영합니다. "+
    "미지원이면 자동 갱신되지 않으며 GPS 추정거리와 ECU 실제 누적거리를 섞지 않습니다.")
   .setNegativeButton("취소",null)
   .setPositiveButton("계기판 일치 확인 · 켜기",(dialog,which)->{
    if(RideService.running||RideService.autoArmed||a.bike()==null||
       !bikeId.equals(a.bike().optString("id"))||
       Double.compare(a.bike().optDouble("odo",Double.NaN),baseline)!=0)return;
    if(!OdometerAutoSyncInbox.enable(a,bikeId,baseline))
     a.toast("동기화 기준값 저장에 실패했습니다.");
    open(bikeId);
   }).show();
 }
 private void beginAuto(String bikeId){
  if(a.readOnly||RestoreRideGate.isRestoring()||RestoreRideGate.isFormWriting()||RideService.saving()){
   a.toast("데이터 보호·복원·저장이 끝난 뒤 실행해 주세요.");return;
  }
  if(RideService.running||RideService.autoArmed){
   a.toast("다른 라이딩 또는 OBD 자동 감지가 이미 진행 중입니다.");return;
  }
  if(a.bike()==null||!bikeId.equals(a.bike().optString("id"))){
   a.toast("현재 선택한 바이크가 바뀌었습니다. 다시 선택해 주세요.");return;
  }
  String address=BikeObdPreferences.address(a,bikeId);
  if(address.isEmpty()){a.toast("이 바이크의 OBD 동글을 먼저 등록해 주세요.");return;}
  if(a.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
   a.toast("라이딩 화면에서 정확한 위치 권한을 허용한 뒤 시작해 주세요.");return;
  }
  if(Build.VERSION.SDK_INT>=31&&a.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){
   a.toast("주변 기기 Bluetooth 권한을 허용한 뒤 시작해 주세요.");return;
  }
  if(Build.VERSION.SDK_INT>=33&&a.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
   a.toast("알림 권한을 허용한 뒤 시작해 주세요.");return;
  }
  LocationManager gps=a.getSystemService(LocationManager.class);
  if(gps==null||!gps.isProviderEnabled(LocationManager.GPS_PROVIDER)){
   a.toast("휴대폰 GPS를 켠 뒤 시작해 주세요.");return;
  }
  boolean enabled=BikeObdPreferences.autoRideEnabled(a,bikeId);
  new AlertDialog.Builder(a).setTitle(enabled?"이 바이크 OBD 자동 라이딩 시작":"이 바이크 OBD 자동 라이딩 활성화")
   .setMessage("선택 바이크: "+a.bikeName(bikeId)+
    "\n동글: "+BikeObdPreferences.deviceName(a,bikeId)+
    "\n\n이 버튼을 누르면 알림이 표시되는 동안 OBD 속도를 읽습니다. 속도 7km/h 이상 3회 감지 시 GPS 기록을 이 바이크에 저장하고, 정차 3/5/10분(설정값) 뒤 파일을 검증한 후 다음 출발을 감지합니다."+
    "\n\n시동 OFF 후 휴대폰 도보 GPS는 제외하고 바이크 주행기록만 저장합니다. OBD 상시 대기는 계속 남아 다음 시동을 기다립니다. 연결 실패 8회 이후에는 약 60초마다 재시도합니다. 백그라운드 알림/배터리가 계속 사용됩니다. 강제종료·재부팅 후 재실행은 보장하지 않으며 앱을 다시 열면 복구를 시도합니다."+
    "\n\nECU의 실제 누적 주행거리는 별도의 계기판 확인을 거쳐야 자동 동기화할 수 있으며 GPS 추정 거리를 계기판 값으로 속이지 않습니다.")
   .setNegativeButton("취소",null)
   .setPositiveButton("동의하고 상시 대기 시작",(dialog,which)->{
    if(RideService.running||RideService.autoArmed||
       a.bike()==null||!bikeId.equals(a.bike().optString("id")))return;
    if(CardoAutoRidePrefs.forBike(a,bikeId)){
     CardoCompanionPairing.stopObserving(a);
     CardoAutoRidePrefs.disable(a);
    }
    BikeObdPreferences.setAutoRideEnabled(a,bikeId,true);
    Intent start=new Intent(a,RideService.class).setAction(RideService.AUTO_ARM)
     .putExtra(RideService.EXTRA_BIKE_ID,bikeId).putExtra(RideService.EXTRA_DEVICE,address);
    a.attempt(()->a.startForegroundService(start));
    a.toast("상시 OBD 자동 연결 대기 요청 · 알림에서 실제 상태 확인");
   }).show();
 }

 private void disableAuto(String bikeId){
  new AlertDialog.Builder(a).setTitle("이 바이크 OBD 자동 기록 해제")
   .setMessage("현재 OBD 감지도 중지합니다. 진행 중인 GPS 기록은 삭제하지 않고 직접 종료할 때까지 유지합니다. 다른 바이크 기록은 변경하지 않습니다.")
   .setNegativeButton("취소",null)
   .setPositiveButton("해제",(dialog,which)->{
    BikeObdPreferences.setAutoRideEnabled(a,bikeId,false);
    if(RideService.autoArmed&&bikeId.equals(RideService.activeRideBikeId))
     a.startService(new Intent(a,RideService.class).setAction(RideService.AUTO_DISARM));
    open(bikeId);
   }).show();
 }
 private void pick(String bikeId){
  if(Build.VERSION.SDK_INT>=31&&a.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){
   a.requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},101);
   a.toast("주변 기기 권한 허용 후 '동글 등록'을 다시 눌러 주세요.");return;
  }
  BluetoothManager manager=(BluetoothManager)a.getSystemService(Context.BLUETOOTH_SERVICE);
  BluetoothAdapter bluetooth=manager==null?null:manager.getAdapter();
  if(bluetooth==null){a.toast("이 휴대폰에서 Bluetooth를 사용할 수 없습니다.");return;}
  if(!bluetooth.isEnabled()){
   a.toast("갤럭시 Bluetooth를 켜고 OBD 동글을 먼저 페어링해 주세요.");return;
  }
  try{
   Set<BluetoothDevice> bonded=bluetooth.getBondedDevices();
   if(bonded==null||bonded.isEmpty()){
    new AlertDialog.Builder(a).setTitle("먼저 OBD 동글 페어링")
     .setMessage("바이크 키 ON → OBD 동글 전원 확인 → 갤럭시 Bluetooth 설정에서 동글 페어링 후 돌아오세요.")
     .setPositiveButton("Bluetooth 설정",(d,w)->a.startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)))
     .setNegativeButton("취소",null).show();
    return;
   }
   List<BluetoothDevice> list=new ArrayList<>(bonded);
   list.sort(Comparator.comparingInt((BluetoothDevice device)->ObdFirstConnectionGuide.adapterPriority(device.getName()))
    .thenComparing(device->{String n=device.getName();return n==null?"":n;}));
   String[] labels=new String[list.size()];
   for(int i=0;i<list.size();i++){
    String name=list.get(i).getName();
    labels[i]=(name==null||name.trim().isEmpty()?"이름 없는 기기":name)+" · "+list.get(i).getAddress();
   }
   a.toast("카르도 등이 보여도 실제 OBD2 동글만 선택해 주세요.");
   new AlertDialog.Builder(a).setTitle("이 바이크의 OBD2 장치 선택")
    .setItems(labels,(dialog,index)->{
     BluetoothDevice chosen=list.get(index);
     String address=chosen.getAddress();
     String name=chosen.getName();
     String lower=name==null?"":name.toLowerCase(java.util.Locale.ROOT);
     if(lower.contains("cardo")||lower.contains("freecom")||lower.contains("packtalk")){
      a.toast("이건 카르도야. OBD 동글을 선택해.");return;
     }
     confirmDeviceRegistration(bikeId,address,name,false);
    }).setNegativeButton("취소",null).show();
  }catch(SecurityException e){a.toast("Bluetooth 주변 기기 권한이 필요합니다.");}
 }
}