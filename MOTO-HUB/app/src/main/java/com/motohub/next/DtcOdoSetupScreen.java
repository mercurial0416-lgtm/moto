package com.motohub.next;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * User-started stationary validation of SAE 0131 with physical dashboard
 * odometer. Do not clear DTCs; no Service 04 requests exist anywhere here.
 */
final class DtcOdoSetupScreen {
 static final int PERMISSION=91;
 private static final UUID SPP=UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
 private DtcOdoSetupScreen(){}
 static void open(MainActivity a){
  if(a==null)return;
  if(a.isFinishing()||a.isDestroyed()||a.readOnly||
     RideService.running||RideService.autoArmed||RideService.saving()||
     RestoreRideGate.isRestoring()||RestoreRideGate.isFormWriting()){
   a.toast("정차 후 OBD 자동 감지·GPS 기록·복원을 종료해 주세요.");return;
  }
  JSONObject bike=a.bike();
  if(bike==null||!RideBikeBinding.valid(bike.optString("id"))||
     GarageBikeStatus.sold(bike.optString("soldDate"))){
   a.toast("차고에서 현재 운행 중인 바이크를 선택해 주세요.");return;
  }
  if(Build.VERSION.SDK_INT>=31&&
     a.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){
   a.requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},PERMISSION);return;
  }
  BluetoothManager manager=(BluetoothManager)a.getSystemService(Context.BLUETOOTH_SERVICE);
  BluetoothAdapter adapter=manager==null?null:manager.getAdapter();
  if(adapter==null||!adapter.isEnabled()){a.toast("Bluetooth를 켜고 OBD2 어댑터를 페어링해 주세요.");return;}
  try{
   List<BluetoothDevice> paired=new ArrayList<>(adapter.getBondedDevices());
   if(paired.isEmpty()){a.toast("페어링된 OBD2 동글이 없습니다.");return;}
   String[] names=new String[paired.size()];
   for(int i=0;i<paired.size();i++){
    String name=paired.get(i).getName();
    names[i]=(name==null?"이름 없는 장치":name)+" · "+paired.get(i).getAddress();
   }
   String bikeId=bike.optString("id");
   new AlertDialog.Builder(a).setTitle("PID 0131 기준값 연결 · "+bike.optString("name"))
    .setMessage("ECU의 고장코드 삭제 후 주행거리를 두 번 읽은 다음 계기판 TOTAL ODO를 직접 입력합니다. ECU·DTC는 변경하지 않습니다.")
    .setItems(names,(d,which)->new Probe(a,bikeId,paired.get(which)).begin())
    .setNegativeButton("취소",null).show();
  }catch(SecurityException ex){a.toast("Bluetooth 연결 권한을 확인해 주세요.");}
 }
 static void showStatus(MainActivity a,String bikeId){
  DtcOdoBridge.Reading r=DtcOdoBridge.snapshot(a,bikeId);
  new AlertDialog.Builder(a).setTitle("PID 0131 보조 ODO")
   .setMessage(DtcOdoBridge.label(a,bikeId)+
    "\n\n실제 ECU 총주행거리가 아닌 보조 추정치입니다. 고장코드 삭제, 배터리 초기화, 카운터 포화 시 보정이 필요합니다. 앱 밖에서 주행한 거리도 카운터가 유지되는 동안 다시 조회해 반영할 수 있습니다.\n\n원본 차고 ODO·정비·GPS 기록은 변경하지 않습니다.")
   .setNegativeButton("닫기",null)
   .setNeutralButton(r.enabled?"추정 연동 끄기":"설정 없음",(d,w)->{
     if(!r.enabled)return;
     if(RideService.running||RideService.autoArmed){a.toast("OBD 감지를 종료한 후 변경해 주세요.");return;}
     if(DtcOdoBridge.disable(a,bikeId))a.toast("PID 0131 연동 끔 · 기존 주행 기록 보존");
     else a.toast("설정 변경 실패");
   })
   .setPositiveButton("0131 다시 조회·계기판 보정",(d,w)->open(a))
   .show();
 }
 private static final class Probe {
  final MainActivity activity;
  final String bikeId;
  final BluetoothDevice device;
  volatile BluetoothSocket socket;
  volatile boolean cancelled=false;
  volatile boolean userCancelled=false;
  AlertDialog progress;
  Probe(MainActivity activity,String bikeId,BluetoothDevice device){
   this.activity=activity;this.bikeId=bikeId;this.device=device;
  }
  void begin(){
   if(RideService.running||RideService.autoArmed||RideService.saving()||
      RestoreRideGate.isRestoring())return;
   if(activity.developerObd!=null)activity.developerObd.stop();
   progress=new AlertDialog.Builder(activity)
    .setTitle("PID 0131 읽는 중")
    .setMessage("키 ON · 정차 상태. ECU 0131 값을 연속 두 번 확인하고 있습니다.")
    .setNegativeButton("중지",(d,w)->{userCancelled=true;cancel();}).create();
   progress.setCancelable(false);progress.show();
   Thread thread=new Thread(this::run,"moto-obd-0131-baseline");
   thread.setDaemon(true);thread.start();
  }
  void cancel(){
   cancelled=true;
   BluetoothSocket s=socket;
   if(s!=null)try{s.close();}catch(Exception ignored){}
  }
  String ask(InputStream in,OutputStream out,String cmd,long timeout)throws Exception{
   if(cancelled)throw new java.io.IOException("취소됨");
   int drained=0;
   while(in.available()>0&&drained++<1024)in.read();
   out.write(cmd.getBytes(StandardCharsets.US_ASCII));out.flush();
   StringBuilder b=new StringBuilder();long end=System.nanoTime()+timeout*1_000_000L;
   while(!cancelled&&System.nanoTime()<end&&b.length()<2048){
    if(in.available()==0){Thread.sleep(25);continue;}
    int c=in.read();
    if(c<0)throw new java.io.IOException("OBD 연결 종료");
    if(c=='>')return b.toString();
    b.append((char)c);
   }
   throw new java.io.IOException(cancelled?"연결 취소":"ECU 응답 시간 초과");
  }
  void run(){
   String result=null;
   int value=-1;
   try{
    BluetoothSocket s=device.createRfcommSocketToServiceRecord(SPP);socket=s;
    if(cancelled)return;
    s.connect();
    InputStream in=s.getInputStream();OutputStream out=s.getOutputStream();
    for(String setup:new String[]{"ATZ\r","ATE0\r","ATL0\r","ATS0\r","ATSP0\r"})
     ask(in,out,setup,4000);
    ask(in,out,ObdReadOnlyProtocol.query("0100"),2500);
    String first=ask(in,out,DtcDistanceSinceClear.REQUEST,3000);
    value=DtcDistanceSinceClear.parseKm(first);
    if(value<0)throw new java.io.IOException("0131 정상 응답 없음: "+ObdOdometer.responseSummary(first));
    String second=ask(in,out,DtcDistanceSinceClear.REQUEST,3000);
    int same=DtcDistanceSinceClear.parseKm(second);
    if(same!=value)throw new java.io.IOException("0131 두 번의 응답 불일치: "+value+"km / "+same+"km");
    if(value==65535)throw new java.io.IOException("0131 포화(65,535km) · GPS 방식 사용 필요");
   }catch(Exception ex){result=ex.getMessage()==null?ex.getClass().getSimpleName():ex.getMessage();}
   finally{cancel();} // Close socket after sampling. Keep its cancel status separate.
   final String error=result;
   final int counter=value;
   activity.runOnUiThread(()->{
    if(progress!=null&&progress.isShowing())progress.dismiss();
    if(userCancelled||activity.isFinishing()||activity.isDestroyed()||RideService.running||
       RideService.autoArmed)return;
    if(error!=null){
     new AlertDialog.Builder(activity).setTitle("PID 0131 조회 실패")
      .setMessage(error+"\n\n차고 주행거리는 변경하지 않았습니다.")
      .setPositiveButton("닫기",null).show();
    }else showCalibration(counter);
   });
  }
  void showCalibration(int counter){
   JSONObject bike=activity.bike();
   if(bike==null||!bikeId.equals(bike.optString("id"))||activity.readOnly)return;
   EditText input=new EditText(activity);
   input.setSingleLine(true);
   input.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
   input.setHint("실제 계기판 TOTAL ODO (km)");
   LinearLayout form=new LinearLayout(activity);
   form.setOrientation(LinearLayout.VERTICAL);
   form.setPadding(activity.dp(20),0,activity.dp(20),0);
   form.addView(input);
   AlertDialog dialog=new AlertDialog.Builder(activity)
    .setTitle("0131 "+counter+"km · 계기판 기준 등록")
    .setMessage("ECU가 고장코드 삭제 후 주행거리를 "+counter+
     "km로 두 번 응답했습니다. 지금 실제 바이크 계기판 총주행거리를 입력하세요.\n\n" +
     "앞으로 PID 0131이 증가한 만큼 추정 ODO가 증가합니다. 이는 실제 총주행거리 자체가 아니며 원본 차고 ODO를 자동 변경하지 않습니다.")
    .setView(form).setNegativeButton("취소",null)
    .setPositiveButton("계기판 확인 · 연동 시작",null).create();
   dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
    double odo;
    try{
     String raw=input.getText().toString().trim();
     if(!raw.matches("[0-9]{1,7}(?:\\.[0-9])?"))
      throw new IllegalArgumentException("계기판 ODO 숫자를 입력해 주세요.");
     odo=Double.parseDouble(raw);
     if(!Double.isFinite(odo)||odo<0||odo>2_000_000)
      throw new IllegalArgumentException("계기판 값 범위를 확인해 주세요.");
    }catch(Exception ex){input.setError(ex.getMessage());return;}
    JSONObject nowBike=activity.bike();
    if(nowBike==null||!bikeId.equals(nowBike.optString("id"))||
       activity.readOnly||RideService.running||RideService.autoArmed||
       RestoreRideGate.isRestoring()||RestoreRideGate.isFormWriting()){
     input.setError("바이크 선택·라이딩·복원 상태를 확인하세요.");return;
    }
    if(!DtcOdoBridge.calibrate(activity,bikeId,odo,counter)){
     input.setError("PID 0131 보정 저장 실패");return;
    }
    dialog.dismiss();activity.toast("PID 0131 추정 ODO 연동 시작 · 자동 OBD 감지 시 갱신됩니다.");
   }));
   dialog.show();
  }
 }
}
