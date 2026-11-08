package com.motohub.next;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
  a.label(p,"기억한 장치가 있어도 자동 연결되지는 않습니다. 연결 검사/라이딩 감지를 사용자가 직접 시작해야 합니다. ODO 값은 실차 계기판 확인 전까지 자동으로 수정하지 않습니다.",13,a.muted,false);
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