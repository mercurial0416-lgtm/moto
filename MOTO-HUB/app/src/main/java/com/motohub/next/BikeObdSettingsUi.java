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
  if(!RideBikeBinding.valid(bikeId)){a.toast("바이크를 먼저 등록해 주세요.");return;}
  org.json.JSONObject bike=null;
  for(org.json.JSONObject entry:a.items("vehicles"))if(bikeId.equals(entry.optString("id"))){bike=entry;break;}
  if(bike==null){a.toast("등록된 바이크를 찾을 수 없습니다.");return;}
  String name=bike.optString("name","내 바이크");
  String address=BikeObdPreferences.address(a,bikeId);
  String adapter=BikeObdPreferences.deviceName(a,bikeId);
  List<String> selected=BikeObdPreferences.selectedPids(a,bikeId);
  LinearLayout p=a.page("바이크 설정 · OBD");
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
     new AlertDialog.Builder(a).setTitle("이 바이크에 OBD 동글 등록")
      .setMessage((name==null?"OBD2 동글":name)+"\n"+address+
       "\n\n연결 주소를 이 바이크에만 저장합니다. 실제 ECU 연결·자동 감지는 시작하지 않습니다.")
      .setNegativeButton("취소",null)
      .setPositiveButton("등록",(d,w)->{
       BikeObdPreferences.setDevice(a,bikeId,address,name);
       open(bikeId);
      }).show();
    }).setNegativeButton("취소",null).show();
  }catch(SecurityException e){a.toast("Bluetooth 주변 기기 권한이 필요합니다.");}
 }
}