package com.motohub.next;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.content.Intent;
import android.location.LocationManager;
import android.widget.LinearLayout;
import android.widget.EditText;
import android.text.InputType;
import org.json.JSONObject;
import android.widget.TextView;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Developer-only, explicitly started, local Bluetooth SPP telemetry; never changes ECU settings. */
final class DeveloperObdScreen {
 static final int CONNECT_PERMISSION_REQUEST=88;
 static final int AUTO_CONNECT_PERMISSION_REQUEST=89;
 static final int ODO_CONNECT_PERMISSION_REQUEST=90;
 private static final String PREFERRED_ADAPTER="obd_remembered_device";
 static final UUID SPP=UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
 private final MainActivity activity;
 private final String[] values=new String[ObdReadOnlyProtocol.SENSOR_PIDS.length];
 private final TextView[] fields=new TextView[ObdReadOnlyProtocol.SENSOR_PIDS.length];
 private TextView status;
 private Dialog owner;
 private volatile boolean stopped=true;
 private volatile BluetoothSocket socket;
 private Thread worker;
 DeveloperObdScreen(MainActivity activity){this.activity=activity;}
 void show(){
  if(!activity.getSharedPreferences("developer_experiments",0).getBoolean("enabled",false))return;
  LinearLayout p=activity.page("개발자 · OBD2 실험실");
  activity.label(p,"실험 기능 · KTM 390 DUKE 호환성 미검증",16,activity.accent,true);
  activity.label(p,"페어링된 ELM327 호환 Bluetooth Classic(SPP) 어댑터로 표준 OBD-II 센서만 조회합니다. 바이크 ECU가 지원하는 항목만 표시됩니다. 순정 계기판 Bluetooth 연결과는 별개입니다.",14,activity.muted,false);
  activity.label(p,"ECU 설정 변경 · 고장코드 삭제 · 출력 제어 없음. 주행 중 조작하지 마세요. 장치 연결은 사용자 선택 시에만 시작되고 원격 전송은 없습니다. 자동 라이딩 도중 일시적 연결 실패 시 최대 8회 재접속하며 앱 재시작 후에는 재연결하지 않습니다.",14,activity.muted,false);
  activity.gap(p,10);
  activity.label(p,"첫 연결 · ① 진단 케이블과 동글 연결 → ② 바이크 시동 ON → ③ 갤럭시에서 Vgate BT3.0 페어링 → ④ 아래에서 센서 테스트",13,activity.fg,false);
  activity.action(p,"갤럭시 Bluetooth 설정 열기",false,this::openBluetoothSettings);
  activity.label(p,"Bluetooth에 연결됐어도 차량 센서가 응답하지 않을 수 있어요. 속도·RPM 수신 여부를 확인한 다음 자동 기록을 사용하세요.",12,activity.muted,false);
  activity.gap(p,8);
  status=activity.text("센서 연결 전 · 시동과 Bluetooth 페어링을 확인하세요",15,activity.muted,true);p.addView(status);
  for(int i=0;i<fields.length;i++){
   fields[i]=activity.text(ObdReadOnlyProtocol.SENSOR_NAMES[i]+": —",17,activity.fg,false);
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=activity.dp(10);p.addView(fields[i],lp);
  }
  activity.gap(p,12);
  activity.action(p,"페어링된 장치로 센서 연결 테스트",true,this::loadPaired);
  activity.action(p,"지난 ECU 지원 항목 · 연결 테스트 기록",false,()->{
   new AlertDialog.Builder(activity).setTitle("OBD2 로컬 점검 기록")
    .setMessage(ObdLocalDiagnosticLog.summary(activity))
    .setPositiveButton("닫기",null)
    .setNeutralButton("기록 지우기",(dialog,which)->new AlertDialog.Builder(activity)
     .setTitle("OBD 점검 기록 삭제")
     .setMessage("연결 상태와 지원 항목 메모만 삭제합니다. 라이딩·GPS 기록은 유지됩니다.")
     .setNegativeButton("취소",null)
     .setPositiveButton("점검 기록 삭제",(d,w)->{
      ObdLocalDiagnosticLog.clear(activity);
      setStatus("OBD 점검 기록만 삭제했습니다.");
     }).show()).show();
  });
  activity.gap(p,15);
  activity.label(p,"자동 라이딩 기록 (개발자 모드 한정)",17,activity.accent,true);
  activity.label(p,"OBD2에서 속도 7km/h 이상을 연속 3회 확인하면 GPS 경로 기록을 자동 시작합니다. 설정한 연속 정차 시간(기본 10분) 후 완료하고 다음 출발을 기다립니다. 블루투스가 끊겨도 진행 중인 GPS 기록은 수동 종료까지 유지합니다. 최초 활성화에는 별도 동의가 필요합니다.",13,activity.muted,false);
  activity.action(p,"정차 종료 시간 설정 (3·5·10분)",false,()->{
   if(RideService.autoArmed||RideService.running){setStatus("기록 중에는 정차 시간을 변경할 수 없습니다.");return;}
   long value=activity.getSharedPreferences("developer_experiments",0).getLong("obd_stop_after_ms",ObdRideAutoGate.USER_DEFAULT_STOP_AFTER_MS);
   long[] values={180_000L,300_000L,600_000L};int selected=2;
   for(int i=0;i<values.length;i++)if(value==values[i])selected=i;
   new AlertDialog.Builder(activity).setTitle("연속 정차 후 기록 종료")
    .setSingleChoiceItems(new String[]{"3분 (빠른 종료)","5분 (일반 휴식)","10분 (기본 · 신호대기/휴식 통합)"},selected,(d,i)->{
     activity.getSharedPreferences("developer_experiments",0).edit().putLong("obd_stop_after_ms",values[i]).apply();
     setStatus("정차 종료 시간: "+(values[i]/60_000L)+"분 · 다음 자동 감지부터 적용");d.dismiss();
    }).setNegativeButton("취소",null).show();
  });
  activity.action(p,"카카오내비 열기 (라이딩 기록은 별도)",false,()->KakaoNaviLaunch.open(activity));
  activity.action(p,"실차 검증 체크 · 관측값 기록",false,()->ObdOnBikeVerification.show(activity));
  activity.action(p,"듀크 실차 확인 체크리스트",false,()->new AlertDialog.Builder(activity)
   .setTitle("실차 확인 · 개발자 모드")
   .setMessage("정차 상태에서만 OBD2 어댑터를 연결하세요.\n\n① OBD2 지원 PID와 연결 상태를 확인\n② 계기판 RPM·속도·수온과 표시값 비교\n③ 시동 OFF/ON 후 재연결·기록 보존 확인\n④ 선택한 내비 앱 실행, 화면 잠금·전화 수신 중 GPS 기록 확인\n⑤ 주행 종료 후 GPS·GPX 경로 확인\n⑥ 실제 계기판 ODO와 일치하지 않으면 반영 금지\n\n기기와 ECU 설정은 변경하지 않고, 주행 중 화면을 조작하지 마세요. 실차·장시간 배터리 시험이 완료되었다는 의미는 아닙니다.")
   .setPositiveButton("확인",null).show());
  android.os.PowerManager pm=(android.os.PowerManager)activity.getSystemService(Context.POWER_SERVICE);
  boolean unrestricted=pm!=null&&pm.isIgnoringBatteryOptimizations(activity.getPackageName());
  activity.label(p,"배터리 제한: "+(unrestricted?"앱 최적화 예외 설정됨":"절전 제한 가능 · 직접 확인 필요"),13,activity.muted,false);
  activity.action(p,"Android 배터리 제한 설정 확인",false,()->{
   new AlertDialog.Builder(activity).setTitle("화면 꺼짐·절전 상태 안내")
    .setMessage("GPS 라이딩은 포그라운드 서비스의 지속 알림을 사용합니다. 기기 절전 제한, 권한 철회, 앱 강제 종료 또는 재부팅 후 자동 복구는 보장하지 않습니다. 배터리 설정에서 MOTO HUB의 제한 상태를 직접 확인할 수 있습니다. 위치 공유 권한은 별도로 유지됩니다.")
    .setNegativeButton("취소",null).setPositiveButton("배터리 설정",(d,w)->{
     try{activity.startActivity(new Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));}
     catch(Exception e){setStatus("이 기기에서는 배터리 제한 설정 화면을 열 수 없습니다.");}
    }).show();
  });
  activity.label(p,"주차·배터리 주의: 일부 OBD 동글은 시동 OFF 후에도 전력을 사용할 수 있어요. 장기 주차 시 동글 분리 또는 대기전류 실측을 권장합니다. 시트 잠금장치와 배선이 눌리지 않는지도 확인하세요.",13,activity.muted,false);
  activity.label(p,"자동 감지를 켜는 동안 알림이 계속 표시됩니다. GPS 기록은 이 휴대폰에만 저장하고 친구·그룹 위치 공유는 시작하지 않습니다. 앱 종료/폰 재부팅 뒤 새로 자동 실행하지 않습니다. 같은 기록 세션 중에는 연결을 최대 8회 재시도합니다.",13,activity.muted,false);
  String remembered=activity.getSharedPreferences("developer_experiments",0).getString(PREFERRED_ADAPTER,"");
  if(!remembered.isEmpty()){
   activity.action(p,"최근 OBD2 장치로 빠르게 연결",true,this::connectRememberedAuto);
   activity.action(p,"기억한 OBD2 장치 삭제",false,()->{
    activity.getSharedPreferences("developer_experiments",0).edit().remove(PREFERRED_ADAPTER).apply();
    setStatus("기억한 장치를 삭제했습니다. 다음부터 다시 선택해 주세요.");
   });
  }
  activity.action(p,"OBD2 연결 실패 후 다시 시도",false,()->{
   if(RideService.autoArmed||RideService.running){
    setStatus("현재 기록 또는 자동 감지가 켜져 있습니다. 기록 보존을 위해 중지한 뒤 재연결하세요.");
    return;
   }
   loadPairedAuto();
  });
  activity.action(p,"OBD2 자동 라이딩 감지 켜기 · 장치 선택",true,this::loadPairedAuto);
  activity.action(p,"OBD2 자동 라이딩 감지 끄기",false,()->{
   activity.startService(new Intent(activity,RideService.class).setAction(RideService.AUTO_DISARM));
   setStatus("자동 감지 중지 요청 · 기존 라이딩 기록은 유지됩니다.");
  });
  if(RideService.autoArmed)setStatus("자동 감지 작동 중 · 알림에서 끌 수 있습니다.");
  activity.label(p,"실시간 진단 · 앱 내부 임시값 (서버 전송·영구 저장 없음)",16,activity.accent,true);
  TextView telemetry=activity.text("OBD2 상태 확인 중…",14,activity.fg,false);p.addView(telemetry);
  activity.action(p,"디지털 계기판 전체 화면",true,()->{
   if(!RideService.autoArmed){setStatus("자동 라이딩 감지를 먼저 켜야 실시간 차량 데이터를 확인할 수 있어요.");return;}
   activity.startActivity(new Intent(activity,ObdDashboardActivity.class));
  });
  activity.action(p,"최근 라이딩 기록 확인",false,()->{stop();activity.close();activity.navigate("라이딩");});
  activity.action(p,"실험 · ECU 표준 ODO(01A6) 1회 조회",false,this::loadPairedOdo);
  activity.action(p,"계기판 누적 주행거리 수동 확인·반영",false,this::manualOdo);
  JSONObject configured=activity.bike();
  String configuredId=configured==null?"":configured.optString("id");
  boolean odoEnabled=OdometerAutoSyncInbox.enabled(activity,configuredId);
  activity.action(p,odoEnabled?"OBD 누적거리 자동 동기화 끄기":"OBD 누적거리 자동 동기화 켜기 · 최초 계기판 확인",false,()->{
   if(RideService.running||RideService.autoArmed){setStatus("OBD 자동 감지를 중지한 뒤 누적거리 동기화 설정을 바꿔 주세요.");return;}
   JSONObject chosen=activity.bike();
   if(chosen==null||!RideBikeBinding.valid(chosen.optString("id"))){setStatus("먼저 차고에서 바이크를 등록하고 선택해 주세요.");return;}
   String id=chosen.optString("id");
   if(OdometerAutoSyncInbox.enabled(activity,id)){
    new AlertDialog.Builder(activity).setTitle("ODO 자동 동기화 끄기")
     .setMessage("기존 주행 기록과 차고 km는 그대로 유지합니다.")
     .setNegativeButton("취소",null).setPositiveButton("끄기",(d,w)->{
       OdometerAutoSyncInbox.disable(activity);setStatus("ODO 자동 동기화 꺼짐 · 기록 유지");
     }).show();return;
   }
   double baseline=chosen.optDouble("odo",Double.NaN);
   if(!OdoAutoSyncPolicy.sane(baseline)){setStatus("차고에 실제 계기판의 누적 km를 먼저 입력해 주세요.");return;}
   new AlertDialog.Builder(activity).setTitle("실제 계기판과 일치할 때만 자동 동기화")
    .setMessage(chosen.optString("name")+" · 차고 "+activity.num(baseline)+" km\\n\\n지금 실제 바이크 계기판에 표시된 누적거리와 일치하는지 확인했나요?\\n\\nECU 표준 01A6을 지원할 때 2회 검증 후 자동 반영합니다. ECU 미지원·불일치·이상 급증 시 실제 km를 변경하지 않고, GPS 누적 추정치는 별도로 남깁니다. 자동 감지를 매번 사용자가 켜야 하며 앱 재부팅 후 자동 접속은 하지 않습니다.")
    .setNegativeButton("취소",null).setPositiveButton("계기판과 일치 · 켜기",(d,w)->{
      if(!OdometerAutoSyncInbox.enable(activity,id,baseline))
       setStatus("동기화 설정 저장 실패 · 기록 변경 없음");
      else setStatus("ODO 동기화 활성화 · 다음 OBD 자동 감지부터 ECU 지원 확인");
    }).show();
  });
  activity.label(p,OdometerAutoSyncInbox.display(activity,configuredId),12,activity.muted,false);
  activity.label(p,"표준 01A6 ODO는 지원 차량에서만 조회됩니다. 듀크 390 지원은 미검증입니다. 조회 결과가 실제 계기판과 일치하는지 두 번 확인해야만 앱 주행거리를 변경합니다. GPS·정비 이력은 그대로 보존합니다.",13,activity.muted,false);
  activity.action(p,"연결 끊기",false,this::stop);
  activity.show();owner=activity.dialog;
  owner.setOnDismissListener(d->{stop();if(activity.developerObd==this)activity.developerObd=null;});
  final Runnable update=new Runnable(){@Override public void run(){
   if(owner==null||!owner.isShowing()||telemetry.getParent()==null)return;
   int qualityScore=ObdLinkQuality.score(RideService.obdLastSampleElapsed,
     android.os.SystemClock.elapsedRealtime(),RideService.obdAverageLatencyMs,
     (int)Math.min(Integer.MAX_VALUE,RideService.obdSpeedSamples),
     RideService.obdQueryFailures,RideService.obdReconnects,
     RideService.obdConnectionAttempts,RideService.obdConnectionSuccesses);
   String quality=ObdLinkQuality.label(qualityScore,(int)Math.min(Integer.MAX_VALUE,RideService.obdSpeedSamples));
   telemetry.setText("상태: "+RideService.obdLink+" · 재접속 "+RideService.obdReconnects+"회 · 정상 속도 샘플 "+RideService.obdSpeedSamples+"건\n"+
    "OBD 세부 상태: "+ObdConnectionState.label(RideService.autoArmed,RideService.obdLastSampleElapsed, android.os.SystemClock.elapsedRealtime(),RideService.obdConnectionAttempts,RideService.obdConnectionSuccesses,RideService.obdReconnects,RideService.obdLastError,RideService.obdPidSupport)+"\n"+
    "통신 품질: "+quality+" · 실패 "+RideService.obdQueryFailures+"회"+
    (RideService.obdSpeedSamples>=3&&qualityScore<55?"\n주의: OBD 신호가 불안정합니다. GPS 기록은 유지됩니다.":"")+"\n"+
     "ECU 실제 수신: "+ObdTelemetryFreshness.connectionLabel(RideService.autoArmed,RideService.running,
      ObdTelemetryFreshness.speedFresh(RideService.obdLastSampleElapsed,android.os.SystemClock.elapsedRealtime()))+
     " · "+ObdSignalAge.label(RideService.obdLastSampleElapsed,android.os.SystemClock.elapsedRealtime())+"\n"+
    "속도 "+ObdTelemetryFreshness.speedLabel(RideService.obdSpeedKmh,RideService.obdLastSampleElapsed,android.os.SystemClock.elapsedRealtime())+
    " · RPM "+(RideService.autoArmed&&ObdTelemetryFreshness.fresh(RideService.obdRpmAt,android.os.SystemClock.elapsedRealtime())&&Double.isFinite(RideService.obdRpm)?String.format(java.util.Locale.KOREA,"%.0f",RideService.obdRpm):"—")+
    "\n수온 "+(RideService.autoArmed&&ObdTelemetryFreshness.fresh(RideService.obdCoolantAt,android.os.SystemClock.elapsedRealtime())&&Double.isFinite(RideService.obdCoolantC)?String.format(java.util.Locale.KOREA,"%.0f°C",RideService.obdCoolantC):"—")+
    " · 전압 "+(RideService.autoArmed&&ObdTelemetryFreshness.fresh(RideService.obdVoltageAt,android.os.SystemClock.elapsedRealtime())&&Double.isFinite(RideService.obdVoltage)?String.format(java.util.Locale.KOREA,"%.1fV",RideService.obdVoltage):"—")+
    "\n"+RideService.engineEstimate()+"\n지원 항목: "+RideService.obdPidSupport+
    "\n보조 센서 진단: "+RideService.obdAuxDiagnostics+
    "\n"+ObdRestStatus.label(RideService.autoArmed,RideService.running,RideService.obdStationaryRemainingMs)+
    "\n연료 잔량: "+(RideService.autoArmed&&ObdTelemetryFreshness.fresh(RideService.obdFuelAt,android.os.SystemClock.elapsedRealtime())&&Double.isFinite(RideService.obdFuelPercent)?String.format(java.util.Locale.KOREA,"%.1f%%",RideService.obdFuelPercent):"미지원/응답 없음")+"\n최근 오류: "+RideService.obdLastError+"\nODO/연료 소모량 미검증 · 통신 데이터 외부 전송 없음");
   telemetry.postDelayed(this,2500);
  }};telemetry.post(update);
 }
 private void manualOdo(){
  if(activity.readOnly){setStatus("원본 보호 상태에서는 주행거리를 수정할 수 없습니다.");return;}
  JSONObject bike=activity.bike();
  if(bike==null||GarageBikeStatus.sold(bike.optString("soldDate"))){setStatus("현재 운행 중인 바이크를 차고에서 먼저 선택해 주세요.");return;}
  String bikeId=bike.optString("id");
  double before=bike.optDouble("odo",0);
  EditText field=new EditText(activity);field.setSingleLine(true);
  field.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
  field.setHint("예: 8500");field.setText(String.format(java.util.Locale.ROOT,"%.0f",before));field.selectAll();
  LinearLayout pane=new LinearLayout(activity);pane.setOrientation(LinearLayout.VERTICAL);
  pane.setPadding(activity.dp(22),0,activity.dp(22),0);pane.addView(field);
  AlertDialog entry=new AlertDialog.Builder(activity).setTitle("계기판 값 직접 입력")
   .setMessage(bike.optString("name")+" · 앱 기록 "+activity.num(before)+" km\n실제 계기판에서 확인한 누적 km만 입력하세요. GPS 추정 거리와 다릅니다.")
   .setView(pane).setNegativeButton("취소",null).setPositiveButton("다음",null).create();
  entry.setOnShowListener(dialog->entry.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button->{
    final double value;
    try{value=OdometerSyncPolicy.checkedManual(field.getText().toString(),before);}
    catch(IllegalArgumentException ex){field.setError(ex.getMessage());return;}
    if(value==before){field.setError("기존 주행거리와 같습니다.");return;}
    entry.dismiss();
    new AlertDialog.Builder(activity).setTitle("주행거리 변경 확인")
     .setMessage(bike.optString("name")+"\n"+activity.num(before)+" km → "+activity.num(value)+" km\n정비 시점 계산이 바뀔 수 있습니다. 기존 주행·정비 기록은 유지됩니다. 계기판을 직접 확인했나요?")
     .setNegativeButton("취소",null).setPositiveButton("직접 확인 · 반영",(x,y)->activity.attempt(()->{
      if(RideService.running||RideService.autoArmed)throw new IllegalStateException("기록 중에는 ODO를 수정할 수 없습니다.");
      JSONObject currentBike=activity.bike();
      if(currentBike==null||!bikeId.equals(currentBike.optString("id")))throw new IllegalStateException("선택한 바이크가 변경됐습니다.");
      JSONObject next=new JSONObject(activity.db.toString());
      JSONObject target=Store.find(next,"vehicles",bikeId);
      if(target==null||GarageBikeStatus.sold(target.optString("soldDate")))throw new IllegalStateException("바이크를 다시 선택해 주세요.");
      if(Double.compare(target.optDouble("odo",0),before)!=0)throw new IllegalStateException("주행거리가 다른 화면에서 변경됐습니다. 다시 확인해 주세요.");
      OdometerSyncPolicy.checkedManual(String.valueOf(value),target.optDouble("odo",0));
      target.put("odo",value);
      activity.write(next);DailyMaintenanceJob.checkSoon(activity);
      setStatus("계기판에서 직접 확인한 "+activity.num(value)+" km 반영 완료 (OBD2 자동 조회 아님)");
     })).show();
   }));
  entry.show();
 }
 private boolean permissionGranted(){return Build.VERSION.SDK_INT<31||activity.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED;}
 void loadPaired(){loadPaired(false,false);}
 void loadPairedAuto(){loadPaired(true,false);}
 void loadPairedOdo(){loadPaired(false,true);}
 private void openBluetoothSettings(){
  try{activity.startActivity(new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS));}
  catch(Exception unavailable){setStatus("갤럭시 설정 → 연결 → Bluetooth에서 동글을 페어링해 주세요.");}
 }
 private void loadPaired(boolean automatic,boolean odo){
  if(owner==null||!owner.isShowing())return;
  if(!permissionGranted()){
   activity.requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},automatic?AUTO_CONNECT_PERMISSION_REQUEST:odo?ODO_CONNECT_PERMISSION_REQUEST:CONNECT_PERMISSION_REQUEST);
   return;
  }
  BluetoothManager manager=(BluetoothManager)activity.getSystemService(Context.BLUETOOTH_SERVICE);
  BluetoothAdapter adapter=manager==null?null:manager.getAdapter();
  if(adapter==null){setStatus("이 휴대폰은 Bluetooth를 지원하지 않아요.");return;}
  if(!adapter.isEnabled()){setStatus("휴대폰 설정에서 Bluetooth를 켜고 어댑터를 페어링해 주세요.");return;}
  try{
   Set<BluetoothDevice> bonded=adapter.getBondedDevices();
   if(bonded==null||bonded.isEmpty()){
    setStatus("페어링된 장치가 없어요. 갤럭시 Bluetooth 설정에서 Vgate/OBDII를 먼저 연결하세요.");
    new AlertDialog.Builder(activity).setTitle("OBD2 동글 먼저 페어링")
     .setMessage("바이크 시동을 켠 뒤 갤럭시 설정 → 연결 → Bluetooth에서 Vgate iCar Pro BT3.0을 페어링해 주세요. 이후 앱으로 돌아와 다시 선택하면 됩니다.")
     .setNegativeButton("나중에",null)
     .setPositiveButton("Bluetooth 설정", (d,w)->openBluetoothSettings()).show();
    return;
   }
   List<BluetoothDevice> devices=new ArrayList<>(bonded);
   devices.sort(Comparator.comparingInt((BluetoothDevice device)->ObdFirstConnectionGuide.adapterPriority(device.getName()))
    .thenComparing(device->{String name=device.getName();return name==null?"":name.toLowerCase(java.util.Locale.ROOT);}));
   String[] labels=new String[devices.size()];
   for(int i=0;i<devices.size();i++){String name=devices.get(i).getName();labels[i]=ObdFirstConnectionGuide.adapterLabel(name)+(name==null||name.trim().isEmpty()?"이름 없는 장치":name)+" · "+devices.get(i).getAddress();}
   new AlertDialog.Builder(activity).setTitle("OBD2 어댑터 선택")
    .setItems(labels,(d,index)->{if(automatic)confirmAuto(devices.get(index));else if(odo)confirmOdo(devices.get(index));else confirmConnect(devices.get(index));})
    .setNegativeButton("취소",null).show();
  }catch(SecurityException e){setStatus("Bluetooth 연결 권한이 필요합니다.");}
 }

 private void confirmOdo(BluetoothDevice device){
  if(RideService.running||RideService.autoArmed){setStatus("주행 중에는 ODO 실험 조회를 사용할 수 없습니다.");return;}
  if(activity.readOnly){setStatus("저장 데이터 보호 상태에서는 ODO를 반영하지 않습니다.");return;}
  if(activity.bike()==null||GarageBikeStatus.sold(activity.bike().optString("soldDate"))){setStatus("먼저 운행 중인 바이크를 선택해 주세요.");return;}
  new AlertDialog.Builder(activity).setTitle("표준 ODO 조회 · 실험")
   .setMessage("선택한 OBD2 장치에 지원 목록 01A0과 읽기 전용 01A6 요청만 전송합니다. 듀크 390 지원 여부는 확인되지 않았습니다. 측정값은 계기판과 대조하기 전 앱에 저장하지 않습니다. 주행 중에는 조작하지 마세요.")
   .setNegativeButton("취소",null).setPositiveButton("조회 동의",(d,w)->startOdoProbe(device)).show();
 }
 private synchronized void startOdoProbe(BluetoothDevice device){
  stop();
  if(worker!=null&&worker.isAlive()){setStatus("기존 조회 연결을 종료 중입니다. 다시 시도해 주세요.");return;}
  if(!permissionGranted()){setStatus("Bluetooth 연결 권한이 필요합니다.");return;}
  stopped=false;setStatus("표준 ODO PID 지원 확인 중…");
  worker=new Thread(()->runOdoProbe(device),"moto-obd-odo-probe");worker.setDaemon(true);worker.start();
 }
 private void runOdoProbe(BluetoothDevice device){
  try{
   BluetoothSocket created=device.createRfcommSocketToServiceRecord(SPP);socket=created;
   if(stopped)return;
   created.connect();if(stopped)return;
   InputStream in=created.getInputStream();OutputStream out=created.getOutputStream();
   for(String setup:new String[]{"ATZ\r","ATE0\r","ATL0\r","ATS0\r","ATSP0\r"}){
    request(in,out,setup,4000);if(stopped)return;
   }
   String bitmap=request(in,out,ObdReadOnlyProtocol.query("01A0"),3000);
   ObdPidCapabilities.Support support=ObdPidCapabilities.fromResponses(null,null,bitmap).forPid("01A6");
   if(support!=ObdPidCapabilities.Support.YES){
    setStatus(support==ObdPidCapabilities.Support.NO?"이 ECU는 표준 ODO PID 01A6을 지원하지 않습니다.":"ODO 지원 목록을 확인할 수 없습니다. 앱 기록은 변경되지 않았습니다.");return;
   }
   String raw=request(in,out,ObdReadOnlyProtocol.query(ObdReadOnlyProtocol.ODOMETER_PID),3000);
   double odo=ObdOdometer.parseKm(raw);
   if(!Double.isFinite(odo)){setStatus("ODO 응답이 없거나 유효하지 않습니다. 기록 변경 없음.");return;}
   if(!stopped)activity.runOnUiThread(()->confirmProbedOdo(odo));
  }catch(Exception e){if(!stopped)setStatus("ODO 조회 실패: "+e.getClass().getSimpleName()+" · 기록 변경 없음");}
  finally{closeSocket();stopped=true;}
 }
 private void confirmProbedOdo(double odo){
  if(owner==null||!owner.isShowing()||activity.dialog!=owner||RideService.running||RideService.autoArmed)return;
  JSONObject bike=activity.bike();
  if(bike==null||GarageBikeStatus.sold(bike.optString("soldDate"))||activity.readOnly)return;
  String id=bike.optString("id");double before=bike.optDouble("odo",0);
  try{OdometerSyncPolicy.checkedManual(String.valueOf(odo),before);}
  catch(IllegalArgumentException ex){setStatus("ECU 값 "+odo+" km · "+ex.getMessage()+" · 반영하지 않음");return;}
  new AlertDialog.Builder(activity).setTitle("실험 ODO 수신 · 1차 확인")
   .setMessage("ECU 표준 PID 01A6: "+activity.num(odo)+" km\n앱: "+activity.num(before)+" km\n이 값이 실제 KTM 계기판에 표시된 누적거리와 정확히 같은지 직접 확인했나요? 자동 변경되지 않습니다.")
   .setNegativeButton("반영 안 함",null).setPositiveButton("계기판과 일치",(d,w)->{
    new AlertDialog.Builder(activity).setTitle("주행거리 변경 · 최종 확인")
     .setMessage("바이크 "+bike.optString("name")+"\n"+activity.num(before)+" → "+activity.num(odo)+" km\n확인한 ODO만 앱에 반영하고 기존 정비·주행 기록은 보존합니다.")
     .setNegativeButton("취소",null).setPositiveButton("동의하고 반영",(x,y)->activity.attempt(()->{
      if(RideService.running||RideService.autoArmed)throw new IllegalStateException("주행 중에는 주행거리를 바꿀 수 없습니다.");
      JSONObject updated=new JSONObject(activity.db.toString());
      JSONObject target=Store.find(updated,"vehicles",id);
      if(target==null||GarageBikeStatus.sold(target.optString("soldDate")))throw new IllegalStateException("바이크가 변경되었습니다.");
      JSONObject selectedBike=activity.bike();
      if(selectedBike==null||!id.equals(selectedBike.optString("id")))throw new IllegalStateException("선택한 바이크가 변경됐습니다. 다시 조회해 주세요.");
      // A stale confirmation must never overwrite a more recent manual update.
      if(Double.compare(target.optDouble("odo",0),before)!=0)throw new IllegalStateException("기존 주행거리가 변경됐습니다. 다시 확인해 주세요.");
      OdometerSyncPolicy.checkedManual(String.valueOf(odo),target.optDouble("odo",0));
      target.put("odo",odo);activity.write(updated);DailyMaintenanceJob.checkSoon(activity);
      setStatus("계기판과 대조한 ECU ODO "+activity.num(odo)+" km 반영 완료");
     })).show();
   }).show();
 }
 private void connectRememberedAuto(){
  if(!permissionGranted()){
   activity.requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},AUTO_CONNECT_PERMISSION_REQUEST);return;
  }
  String address=activity.getSharedPreferences("developer_experiments",0).getString(PREFERRED_ADAPTER,"");
  if(address.isEmpty()||!BluetoothAdapter.checkBluetoothAddress(address)){
   setStatus("기억한 OBD2 장치가 없습니다. 다시 선택해 주세요.");return;
  }
  BluetoothManager manager=(BluetoothManager)activity.getSystemService(Context.BLUETOOTH_SERVICE);
  BluetoothAdapter adapter=manager==null?null:manager.getAdapter();
  if(adapter==null||!adapter.isEnabled()){setStatus("먼저 Bluetooth를 켜 주세요.");return;}
  try{
   for(BluetoothDevice device:adapter.getBondedDevices())if(address.equals(device.getAddress())){
    confirmAuto(device);return; // Fresh explicit consent even for a remembered adapter.
   }
   setStatus("기억한 장치가 페어링 목록에 없습니다. 다시 선택해 주세요.");
  }catch(SecurityException e){setStatus("Bluetooth 연결 권한이 필요합니다.");}
 }
 private void confirmAuto(BluetoothDevice device){
  if(RideService.running||RideService.autoArmed){setStatus("먼저 기존 라이딩 또는 자동 감지를 중지해 주세요.");return;}
  if(activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
   setStatus("라이딩 탭에서 정확한 위치 권한을 허용한 뒤 다시 시도해 주세요.");return;
  }
  if(Build.VERSION.SDK_INT>=33&&activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
   setStatus("라이딩 탭에서 알림 권한을 허용한 뒤 다시 시도해 주세요.");return;
  }
  LocationManager location=activity.getSystemService(LocationManager.class);
  if(location==null||!location.isProviderEnabled(LocationManager.GPS_PROVIDER)){
   setStatus("먼저 휴대폰 GPS를 켜 주세요.");return;
  }
  final boolean[] remember={false};
  new AlertDialog.Builder(activity).setTitle("OBD2 자동 라이딩 기록을 켤까요?")
   .setMessage("이 장치가 연결된 동안 속도 7km/h 이상을 연속 3회 감지하면 GPS 경로를 이 휴대폰에 자동 저장합니다. 설정한 정차 시간 후 종료하며 다음 출발을 다시 감지합니다. 백그라운드 위치 사용과 지속 알림이 필요합니다. 그룹 공유는 켜지지 않습니다. 재연결에 최종 실패하면 진행 중인 GPS 주행은 유지되며 알림에서 직접 종료합니다. 원할 때 알림에서 중지하세요. 실험 기능이므로 실제 듀크 390 동작은 아직 검증되지 않았습니다.")
   .setMultiChoiceItems(new String[]{"이 장치를 이 휴대폰에 기억하기 (다음 연결 선택만 생략)"},remember,(dialog,index,selected)->remember[index]=selected)
   .setNegativeButton("취소",null).setPositiveButton("동의하고 자동 감지 시작",(d,w)->{
    if(remember[0])activity.getSharedPreferences("developer_experiments",0).edit().putString(PREFERRED_ADAPTER,device.getAddress()).apply();
    stop();
    Intent start=new Intent(activity,RideService.class).setAction(RideService.AUTO_ARM).putExtra(RideService.EXTRA_DEVICE,device.getAddress())
     .putExtra(RideService.EXTRA_BIKE_ID,activity.bike()==null?"":activity.bike().optString("id"));
    activity.attempt(()->activity.startForegroundService(start));
    setStatus("OBD2 자동 감지 시작 요청 · 알림으로 상태 확인");
   }).show();
 }
 private void confirmConnect(BluetoothDevice device){
  if(RideService.autoArmed){setStatus("자동 라이딩 감지 중에는 개별 OBD2 연결을 사용할 수 없습니다. 먼저 자동 감지를 종료하세요.");return;}
  new AlertDialog.Builder(activity).setTitle("읽기 전용 연결")
   .setMessage("선택한 Bluetooth 장치와 연결하고 표준 OBD-II 센서(PID 01 0C/0D/05/42, 확인된 2F 연료 비율)만 읽습니다. ECU·계기판에 설정을 기록하지 않습니다. KTM 국내 사양과 어댑터 호환성은 검증되지 않았습니다.")
   .setNegativeButton("취소",null).setPositiveButton("동의하고 연결",(d,w)->connect(device)).show();
 }
 private synchronized void connect(BluetoothDevice device){
  stop();
  if(worker!=null&&worker.isAlive()){setStatus("기존 연결을 종료 중입니다. 잠시 후 다시 선택해 주세요.");return;}
  if(!permissionGranted()){setStatus("Bluetooth 권한이 필요합니다.");return;}
  stopped=false;setStatus("연결 중…");
  worker=new Thread(()->runConnection(device),"moto-obd-read-only");worker.setDaemon(true);worker.start();
 }
 private void runConnection(BluetoothDevice device){
  try{
   if(stopped)return;
   BluetoothSocket created=device.createRfcommSocketToServiceRecord(SPP);
   socket=created;if(stopped){created.close();return;}
   created.connect();if(stopped)return;
   InputStream in=created.getInputStream();OutputStream out=created.getOutputStream();
   // AT commands configure only the ELM adapter. All vehicle requests are whitelisted mode 01 reads.
   for(String setup:new String[]{"ATZ\r","ATE0\r","ATL0\r","ATS0\r","ATSP0\r"}){
    request(in,out,setup,4000);if(stopped)return;
   }
   String supported00="",supported20="",supported40="";
   try{supported00=request(in,out,ObdReadOnlyProtocol.query("0100"),2500);}catch(Exception ignored){}
   try{supported20=request(in,out,ObdReadOnlyProtocol.query("0120"),2500);}catch(Exception ignored){}
   try{supported40=request(in,out,ObdReadOnlyProtocol.query("0140"),2500);}catch(Exception ignored){}
   ObdPidCapabilities capabilities=ObdPidCapabilities.fromAllResponses(supported00,supported20,supported40,null);
   ObdLocalDiagnosticLog.record(activity,"BT_OK");
   ObdLocalDiagnosticLog.capability(activity,capabilities);
   setStatus("Bluetooth 연결됨 · 차량 센서 응답 확인 중…");
   int noDataCycles=0,validSensors=0;
   boolean gotSpeed=false,loggedSensor=false;
   while(!stopped){
    int successThisCycle=0;
    for(int i=0;i<ObdReadOnlyProtocol.SENSOR_PIDS.length&&!stopped;i++){
     String pid=ObdReadOnlyProtocol.SENSOR_PIDS[i];
     if(capabilities.forPid(pid)==ObdPidCapabilities.Support.NO){updateField(i,"미지원 (PID 목록)");continue;}
     if("012F".equals(pid)&&capabilities.forPid(pid)!=ObdPidCapabilities.Support.YES){updateField(i,"지원 미확인 · 연료 조회 생략");continue;}
     try{
      String result=ObdReadOnlyProtocol.decode(pid,request(in,out,ObdReadOnlyProtocol.query(pid),2500));
      if(!"지원 안 함".equals(result)){
       successThisCycle++;validSensors++;if("010D".equals(pid))gotSpeed=true;
       if(!loggedSensor){ObdLocalDiagnosticLog.record(activity,"SENSOR_OK");loggedSensor=true;}
      }
      updateField(i,result);
     }catch(InterruptedException interrupted){throw interrupted;}
     catch(Exception temporary){if(stopped)return;updateField(i,"응답 지연 · 다음 주기 재시도");}
    }
    if(stopped)break;
    setStatus(ObdFirstConnectionGuide.result(capabilities.forPid("010D"),gotSpeed,validSensors)+"\n"+capabilities.summary());
    if(successThisCycle==0){
     if(++noDataCycles>=3){
      ObdLocalDiagnosticLog.record(activity,"NO_ECU");
      throw new java.io.IOException("ECU 응답을 확인하지 못했습니다. 시동 ON과 유로5 6핀 변환 케이블을 확인해 주세요.");
     }
    }
    else noDataCycles=0;
    Thread.sleep(750);
   }
  }catch(Exception e){if(!stopped){
   ObdLocalDiagnosticLog.record(activity,"ERROR");
   setStatus(ObdFirstConnectionGuide.failure(e));
  }}
  finally{closeSocket();stopped=true;}
 }
 private String request(InputStream in,OutputStream out,String command,long timeoutMs)throws Exception{
  if(stopped)throw new java.io.IOException("연결이 종료됐습니다.");
  // Drain a bounded amount of leftover adapter response before issuing the next query.
  int discarded=0;while(in.available()>0&&discarded++<1024)in.read();
  out.write(command.getBytes(StandardCharsets.US_ASCII));out.flush();
  StringBuilder response=new StringBuilder();long until=System.nanoTime()+timeoutMs*1_000_000L;
  while(!stopped&&System.nanoTime()<until&&response.length()<2048){
   if(in.available()==0){Thread.sleep(25);continue;}
   int c=in.read();if(c<0)throw new java.io.IOException("OBD2 연결 종료");
   if(c=='>')return response.toString();response.append((char)c);
  }
  if(stopped)throw new java.io.IOException("OBD2 연결 중지");
  throw new java.io.IOException("OBD2 응답이 완료되지 않았습니다.");
 }
 private void updateField(int index,String result){activity.runOnUiThread(()->{
  if(!stopped&&owner!=null&&owner.isShowing()&&activity.dialog==owner)fields[index].setText(ObdReadOnlyProtocol.SENSOR_NAMES[index]+": "+result);
 });}
 private void setStatus(String message){activity.runOnUiThread(()->{
  if(owner!=null&&owner.isShowing()&&activity.dialog==owner)status.setText(message);
 });}
 synchronized void stop(){stopped=true;closeSocket();for(int i=0;i<fields.length;i++)updateStopped(i);setStatus("연결 끊김");}
 private void updateStopped(int index){activity.runOnUiThread(()->{if(owner!=null&&owner.isShowing()&&activity.dialog==owner)fields[index].setText(ObdReadOnlyProtocol.SENSOR_NAMES[index]+": —");});}
 private void closeSocket(){BluetoothSocket old=socket;socket=null;if(old!=null)try{old.close();}catch(Exception ignored){}}
}
