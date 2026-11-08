package com.motohub.next;

import android.app.AlertDialog;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import org.json.JSONObject;

/**
 * Explicit calibration UI for the fallback odometer. Does not change garage
 * ODO, ECU values, maintenance dates or any historical ride. GPS gaps remain
 * gaps, never fabricated mileage.
 */
final class HybridOdometerUi {
 private HybridOdometerUi(){}
 static void show(MainActivity a){
  JSONObject bike=a.bike();
  if(bike==null||GarageBikeStatus.sold(bike.optString("soldDate"))||
      !RideBikeBinding.valid(bike.optString("id"))){
   a.toast("운행 중인 바이크를 차고에서 선택해 주세요.");return;
  }
  String id=bike.optString("id");
  String name=bike.optString("name");
  HybridOdometerEstimate.Reading r=HybridOdometerEstimate.snapshot(a,id);
  new AlertDialog.Builder(a).setTitle("ODO 우회 추정 · "+name)
   .setMessage(HybridOdometerEstimate.label(a,id)+
    "\n\n실제 계기판 ODO를 한번 입력하면 이후 해당 바이크에 저장된 GPS 주행거리를 더해 추정값을 표시합니다."+
    "\nGPS를 끄고 달린 거리·누락된 기록·타이어 오차는 계산할 수 없습니다. 실제 ECU 누적거리와 동일하지 않습니다."+
    "\n\n계기판에서 직접 확인할 때만 보정하세요. 차고/정비 이력은 자동 변경하지 않습니다.")
   .setNegativeButton("닫기",null)
   .setPositiveButton("실제 계기판 값으로 보정",(d,w)->calibrate(a,id,name))
   .show();
 }
 private static void calibrate(MainActivity a,String id,String name){
  if(a.readOnly||RideService.running||RideService.autoArmed||RideService.saving()||
   RestoreRideGate.isRestoring()||RestoreRideGate.isFormWriting()){
   a.toast("라이딩/자동감지/복원/읽기전용 상태를 종료하고 보정해 주세요.");return;
  }
  JSONObject current=a.bike();
  if(current==null||!id.equals(current.optString("id"))){
   a.toast("바이크가 바뀌었습니다. 다시 열어 주세요.");return;
  }
  EditText input=new EditText(a);
  input.setSingleLine(true);
  input.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
  input.setHint("계기판 총 주행거리 km");
  HybridOdometerEstimate.Reading existing=HybridOdometerEstimate.snapshot(a,id);
  if(existing.configured&&existing.valid)
   input.setText(String.format(java.util.Locale.ROOT,"%.1f",existing.estimatedKm));
  else input.setText(String.format(java.util.Locale.ROOT,"%.0f",current.optDouble("odo",0)));
  input.selectAll();
  LinearLayout pane=new LinearLayout(a);pane.setOrientation(LinearLayout.VERTICAL);
  pane.setPadding(a.dp(22),0,a.dp(22),0);pane.addView(input);
  AlertDialog dialog=new AlertDialog.Builder(a).setTitle("계기판 실측 기준 등록")
   .setMessage(name+" · 실제 계기판의 TOTAL/ODO 숫자를 직접 읽어 입력해 주세요."+
     "\nTRIP A/B, 0121, 0131은 총주행거리가 아닙니다."+
     "\n\n이 보정은 GPS 추정 기준만 갱신합니다. 차고 실제 ODO나 정비 기록은 바꾸지 않습니다.")
   .setView(pane).setNegativeButton("취소",null)
   .setPositiveButton("계기판 대조 · 보정",null).create();
  dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
   final double value;
   try{
    String raw=input.getText().toString().trim();
    if(!raw.matches("[0-9]{1,7}(?:\\.[0-9])?"))throw new IllegalArgumentException("0~2,000,000 km 숫자만 입력하세요.");
    value=Double.parseDouble(raw);
    if(!HybridOdometerEstimate.validAnchor(id,value,0))throw new IllegalArgumentException("계기판 범위를 확인하세요.");
   }catch(Exception e){input.setError(e.getMessage());return;}
   JSONObject latest=a.bike();
   if(latest==null||!id.equals(latest.optString("id"))){
    input.setError("선택된 바이크가 변경됐습니다.");return;
   }
   if(RideService.running||RideService.autoArmed||RideService.saving()||
      RestoreRideGate.isRestoring()||RestoreRideGate.isFormWriting()){
    input.setError("라이딩과 복원 작업을 종료한 뒤 다시 시도해 주세요.");return;
   }
   if(!HybridOdometerEstimate.calibrate(a,id,value)){
    input.setError("GPS 기준/저장 상태를 확인해 주세요.");return;
   }
   dialog.dismiss();
   a.toast("GPS 추정 ODO 기준 갱신 · 실제 계기판 확인값 "+value+" km");
   show(a);
  }));
  dialog.show();
 }
}
