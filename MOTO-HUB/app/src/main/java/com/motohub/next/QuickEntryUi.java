package com.motohub.next;

import android.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.text.InputType;
import android.widget.*;
import org.json.JSONObject;
import java.time.LocalDate;
import java.util.List;

/** Deliberately small forms for the two everyday actions; full editors remain available. */
final class QuickEntryUi {
 final MainActivity a;
 QuickEntryUi(MainActivity a){this.a=a;}
 private EditText number(LinearLayout body,String label,String initial,String hint){
  a.label(body,label,14,a.fg,true);
  EditText field=new EditText(a);field.setSingleLine(true);
  field.setTextColor(a.fg);field.setHintTextColor(a.muted);field.setTextSize(19);
  field.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
  field.setImeOptions(EditorInfo.IME_ACTION_NEXT);
  field.setBackground(a.shape(a.panel,a.line,11));
  field.setPadding(a.dp(15),a.dp(12),a.dp(15),a.dp(12));
  field.setMinHeight(a.dp(54));field.setHint(hint);field.setContentDescription(label);
  if(initial!=null&&!initial.isEmpty())field.setText(initial);
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=a.dp(18);body.addView(field,lp);
  return field;
 }
 private double numeric(EditText input,String title,boolean required)throws Exception{
  String raw=input.getText().toString().replace(",","").trim();
  if(raw.isEmpty()&&!required)return 0;
  if(raw.isEmpty())throw new IllegalArgumentException(title+"을(를) 입력해 주세요.");
  try{double n=Double.parseDouble(raw);if(!Double.isFinite(n)||n<0)throw new NumberFormatException();return n;}
  catch(NumberFormatException ex){throw new IllegalArgumentException(title+"에 올바른 숫자를 입력해 주세요.");}
 }
 private Spinner options(LinearLayout body,String title,String[] items){
  a.label(body,title,14,a.fg,true);
  Spinner spinner=new Spinner(a);
  ArrayAdapter<String> adapter=new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,items){
   @Override public View getView(int position,View view,android.view.ViewGroup parent){
    TextView item=(TextView)super.getView(position,view,parent);item.setTextColor(a.fg);item.setTextSize(16);return item;
   }
  };spinner.setAdapter(adapter);spinner.setBackground(a.shape(a.panel,a.line,11));
  spinner.setPadding(a.dp(10),0,a.dp(8),0);
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,a.dp(56));lp.bottomMargin=a.dp(18);body.addView(spinner,lp);
  return spinner;
 }
 void fuel(){
  JSONObject bike=a.bike();if(bike==null||a.readOnly){a.toast("기록 가능한 바이크를 먼저 등록해 주세요.");return;}
  LinearLayout body=a.page("주유 기록");a.label(body,bike.optString("name")+" · 오늘",13,a.muted,false);
  a.gap(body,12);
  EditText amount=number(body,"주유 금액 (원)","","예: 12000");
  EditText liters=number(body,"실제 주유량 (L)","","예: 4.5");
  LinearLayout calculator=a.col();calculator.setVisibility(View.GONE);
  EditText unitPrice=number(calculator,"리터당 가격 (원)","","예: 1750");
  a.label(calculator,"리터당 가격 × 주유량으로 예상 금액을 자동 채웁니다. 실제 결제액이 다르면 금액을 수정하세요.",12,a.muted,false);
  Button calcToggle=a.button("리터당 가격으로 자동 계산",false,
   ()->calculator.setVisibility(calculator.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE));
  body.addView(calcToggle);body.addView(calculator);
  FuelAutoAmount fuelAuto=new FuelAutoAmount();
  amount.addTextChangedListener(new android.text.TextWatcher(){
   public void beforeTextChanged(CharSequence value,int start,int count,int after){}
   public void onTextChanged(CharSequence value,int start,int before,int count){
    fuelAuto.noteAmountEdited(value.toString());
   }
   public void afterTextChanged(android.text.Editable value){}
  });
  android.text.TextWatcher autoCalc=new android.text.TextWatcher(){
   public void beforeTextChanged(CharSequence value,int start,int count,int after){}
   public void onTextChanged(CharSequence value,int start,int before,int count){
    if(calculator.getVisibility()!=View.VISIBLE)return;
    String beforeAmount=amount.getText().toString();
    String nextAmount=fuelAuto.update(beforeAmount,liters.getText().toString(),unitPrice.getText().toString());
    if(!beforeAmount.equals(nextAmount))amount.setText(nextAmount);
   }
   public void afterTextChanged(android.text.Editable text){}
  };
  unitPrice.addTextChangedListener(autoCalc);liters.addTextChangedListener(autoCalc);
  a.label(body,"금액과 리터만 입력하면 바로 저장할 수 있어요.",13,a.muted,false);
  TextView costPerLiter=a.text("금액·주유량을 입력하면 리터당 가격을 표시합니다",12,a.muted,false);
  body.addView(costPerLiter);
  android.text.TextWatcher unitHint=new android.text.TextWatcher(){
   public void beforeTextChanged(CharSequence value,int start,int count,int after){}
   public void onTextChanged(CharSequence value,int start,int before,int count){
    costPerLiter.setText(FuelEntryMath.unitPrice(amount.getText().toString(),liters.getText().toString()));
   }
   public void afterTextChanged(android.text.Editable value){}
  };
  amount.addTextChangedListener(unitHint);liters.addTextChangedListener(unitHint);
  JSONObject recentFuel=RecentFuelRecord.latest(a.items("expenses"),bike.optString("id"));
  if(recentFuel!=null){
   final JSONObject previous=recentFuel;
   Button reuse=a.button("지난 주유 금액·리터 불러오기",false,()->{
    // Hide the calculator first: liters.setText triggers listeners synchronously.
    calculator.setVisibility(View.GONE);
    fuelAuto.reset();
    amount.setText(String.valueOf((long)previous.optDouble("amount")));
    liters.setText(String.valueOf(previous.optDouble("liters")));
   });
   body.addView(reuse);
  }
  CheckBox full=new CheckBox(a);full.setText("가득 주유했어요 (연비 계산용)");
  full.setTextColor(a.fg);full.setTextSize(14);full.setMinHeight(a.dp(50));body.addView(full);
  LinearLayout extra=a.col();extra.setVisibility(View.GONE);
  EditText odo=number(extra,"현재 계기판 (km)",String.valueOf((long)bike.optDouble("odo")),"현재 주행거리");
  // Reuse the bundled offline OCR model; nothing is sent to a server or saved before confirmation.
  a.quickOdoField=odo;
  a.action(extra,"갤러리에서 계기판 사진 선택",false,()->a.chooseQuickOdometerPhoto(odo));
  EditText date=new EditText(a);date.setText(a.today());date.setTextSize(16);date.setTextColor(a.fg);
  date.setFocusable(false);date.setClickable(true);date.setMinHeight(a.dp(50));date.setBackground(a.shape(a.panel,a.line,11));
  date.setPadding(a.dp(13),0,a.dp(13),0);date.setOnClickListener(v->a.pickDate(date));
  a.label(extra,"주유 날짜",14,a.fg,true);extra.addView(date,new LinearLayout.LayoutParams(-1,a.dp(52)));
  a.gap(extra,12);
  a.action(extra,"전체 주유·비용 편집 열기",false,()->a.edit("expenses",null));
  Button toggle=a.button("주행거리·날짜 수정",false,()->{
   if(full.isChecked()){extra.setVisibility(View.VISIBLE);return;}
   extra.setVisibility(extra.getVisibility()==View.GONE?View.VISIBLE:View.GONE);
  });
  body.addView(toggle);body.addView(extra);
  a.action(body,"계기판 바로 촬영",false,()->{
   extra.setVisibility(View.VISIBLE);
   a.startQuickOdometerCamera(odo);
  });
  // Full-to-full efficiency requires the real odometer; don't hide it on a full fill.
  full.setOnCheckedChangeListener((button,checked)->{
   if(checked)extra.setVisibility(View.VISIBLE);
  });
  a.gap(body,12);
  a.action(body,"저장",true,()->a.attempt(()->{
   JSONObject record=QuickRecordDraft.fuel(bike.optString("id"),date.getText().toString().trim(),
    numeric(odo,"계기판",true),numeric(amount,"주유 금액",true),numeric(liters,"주유량",true),full.isChecked());
   Runnable persist=()->a.attempt(()->{
    JSONObject next=new JSONObject(a.db.toString());
    Store.upsert(next,"expenses",record);
    JSONObject vehicle=Store.find(next,"vehicles",bike.optString("id"));
    if(vehicle!=null&&record.optDouble("mileage")>vehicle.optDouble("odo"))vehicle.put("odo",record.optDouble("mileage"));
    a.write(next);a.close();a.render();a.toast("주유 기록을 저장했어요.");DailyMaintenanceJob.checkSoon(a);
   });
   if(QuickLogDuplicate.exists(a.items("expenses"),record,"expenses")){
    new AlertDialog.Builder(a).setTitle("같은 주유 기록이 있어요")
     .setMessage("금액·리터·날짜·주행거리가 같은 기록이 이미 있습니다. 그래도 저장할까요?")
     .setNegativeButton("취소",null).setPositiveButton("저장",(dialog,which)->persist.run()).show();
   }else persist.run();
  }));
  a.show();
 }
 void maintenance(){
  JSONObject bike=a.bike();if(bike==null||a.readOnly){a.toast("기록 가능한 바이크를 먼저 등록해 주세요.");return;}
  LinearLayout body=a.page("정비주기 등록");
  a.label(body,bike.optString("name")+" · 기준 주행거리 "+a.num(bike.optDouble("odo"))+" km",13,a.muted,false);a.gap(body,13);
  String[] items={"엔진오일","오일필터","체인","타이어","브레이크 패드","냉각수","배터리","기타 직접 입력"};
  Spinner service=options(body,"정비 항목",items);
  EditText customType=new EditText(a);customType.setSingleLine(true);customType.setHint("정비 항목 이름");
  customType.setTextColor(a.fg);customType.setHintTextColor(a.muted);
  customType.setPadding(a.dp(14),a.dp(12),a.dp(14),a.dp(12));customType.setBackground(a.shape(a.panel,a.line,11));
  customType.setVisibility(View.GONE);body.addView(customType);
  service.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
   public void onItemSelected(android.widget.AdapterView<?> parent,View view,int position,long id){
    customType.setVisibility(position==items.length-1?View.VISIBLE:View.GONE);
   }
   public void onNothingSelected(android.widget.AdapterView<?> parent){}
  });
  String[] distance={"거리 주기 설정 안 함","+500km","+1,000km","+3,000km","+5,000km","+10,000km","직접 입력"};
  int[] intervals={0,500,1000,3000,5000,10000,-1};
  Spinner distanceChoice=options(body,"몇 km 뒤에 정비할까요?",distance);
  String[] periods={"날짜 주기 설정 안 함","1개월 뒤","3개월 뒤","6개월 뒤","12개월 뒤","날짜 직접 선택"};
  int[] months={0,1,3,6,12,-1};
  Spinner dateChoice=options(body,"또는 몇 개월 뒤에 정비할까요?",periods);
  Spinner kmAlert=options(body,"몇 km 전에 알림을 받을까요?",
   new String[]{"100km 전","300km 전","500km 전"});
  kmAlert.setSelection(2);
  Spinner dateAlert=options(body,"며칠 전에 알림을 받을까요?",
   new String[]{"7일 전","15일 전","30일 전"});
  dateAlert.setSelection(2);
  final int[] kmBeforeChoices={100,300,500},daysBeforeChoices={7,15,30};
  LinearLayout advanced=a.col();advanced.setVisibility(View.GONE);
  EditText mileage=number(advanced,"현재 계기판 (km)",String.valueOf((long)bike.optDouble("odo")),"km");
  EditText nextKm=number(advanced,"직접 지정하는 다음 정비 거리 (km)","","예: 14000");
  a.label(advanced,"직접 지정하는 다음 정비일",14,a.fg,true);
  EditText nextDate=new EditText(a);nextDate.setText(LocalDate.parse(a.today()).plusMonths(1).toString());nextDate.setTextColor(a.fg);nextDate.setSingleLine(true);
  nextDate.setFocusable(false);nextDate.setClickable(true);nextDate.setMinHeight(a.dp(54));
  nextDate.setBackground(a.shape(a.panel,a.line,11));nextDate.setPadding(a.dp(13),0,a.dp(13),0);
  nextDate.setOnClickListener(v->a.pickDate(nextDate));advanced.addView(nextDate);
  Button advancedToggle=a.button("계기판·직접 지정 수정",false,
   ()->advanced.setVisibility(advanced.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE));
  body.addView(advancedToggle);body.addView(advanced);
  // A custom interval must reveal its required input immediately; no second hidden tap.
  android.widget.AdapterView.OnItemSelectedListener revealCustom=new android.widget.AdapterView.OnItemSelectedListener(){
   @Override public void onItemSelected(android.widget.AdapterView<?> parent,View view,int position,long id){
    if(distanceChoice.getSelectedItemPosition()==intervals.length-1
      ||dateChoice.getSelectedItemPosition()==months.length-1)advanced.setVisibility(View.VISIBLE);
   }
   @Override public void onNothingSelected(android.widget.AdapterView<?> parent){}
  };
  distanceChoice.setOnItemSelectedListener(revealCustom);
  dateChoice.setOnItemSelectedListener(revealCustom);
  a.gap(body,13);a.label(body,"정비주기 끝에 가까워지면 알림을 보냅니다. 짧은 주기는 등록 즉시 울리지 않도록 알림 시점을 조정해요. 공식 정비주기는 제조사 매뉴얼을 확인해 주세요.",13,a.muted,false);
  a.action(body,"저장",true,()->a.attempt(()->{
   int kmInterval=intervals[distanceChoice.getSelectedItemPosition()];
   int monthsInterval=months[dateChoice.getSelectedItemPosition()];
   double odo=numeric(mileage,"현재 계기판",true);
   double dueKm=kmInterval<0?numeric(nextKm,"다음 정비 거리",true):QuickRecordDraft.nextOdometer(odo,kmInterval);
   String dueDate=monthsInterval<0?nextDate.getText().toString().trim():QuickRecordDraft.dueAfterMonths(a.today(),monthsInterval);
   String type=service.getSelectedItemPosition()==items.length-1?customType.getText().toString().trim():service.getSelectedItem().toString();
   JSONObject record=QuickRecordDraft.maintenance(bike.optString("id"),type,a.today(),odo,dueKm,dueDate,
    Math.max(0,kmInterval),Math.max(0,monthsInterval));
   record.put("alertKmBefore",kmBeforeChoices[kmAlert.getSelectedItemPosition()]);
   record.put("alertDaysBefore",daysBeforeChoices[dateAlert.getSelectedItemPosition()]);
   Runnable save=()->a.attempt(()->{
    JSONObject next=new JSONObject(a.db.toString());Store.upsert(next,"services",record);
    JSONObject vehicle=Store.find(next,"vehicles",bike.optString("id"));
    if(vehicle!=null&&odo>vehicle.optDouble("odo"))vehicle.put("odo",odo);
    a.write(next);a.close();a.render();DailyMaintenanceJob.checkSoon(a);
    if(!DailyMaintenanceJob.remindersEnabled(a))askEnableNotifications();
    else a.toast("정비주기를 등록했어요. 예정 시 알림을 확인하세요.");
   });
   if(QuickLogDuplicate.exists(a.items("services"),record,"services")){
    new AlertDialog.Builder(a).setTitle("이미 등록된 정비주기")
     .setMessage("같은 날짜와 계기판의 정비 기록이 있습니다. 추가로 저장할까요?")
     .setNegativeButton("취소",null).setPositiveButton("추가",(d,w)->save.run()).show();
   }else save.run();
  }));
  a.show();
 }
 private void askEnableNotifications(){
  new AlertDialog.Builder(a).setTitle("정비 알림을 켤까요?")
   .setMessage("등록한 정비주기가 다가오면 휴대폰 알림을 보냅니다. 주행거리는 앱에 저장한 계기판 기준이며, 짧은 주기에는 알림 범위가 더 좁아집니다. 배터리 제한에 따라 알림이 지연될 수 있고 설정에서 언제든 끌 수 있습니다.")
   .setNegativeButton("나중에",(d,w)->a.toast("정비주기는 저장됐어요. 알림은 설정에서 켤 수 있어요."))
   .setPositiveButton("알림 켜기",(d,w)->{
    a.getSharedPreferences("notifications",0).edit().putBoolean("maintenance_opt_in",true).apply();
    DailyMaintenanceJob.schedule(a);
    if(android.os.Build.VERSION.SDK_INT>=33&&a.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)
     a.requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},72);
    else DailyMaintenanceJob.checkSoon(a);
   }).show();
 }
}
