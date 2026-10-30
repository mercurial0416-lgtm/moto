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
  spinner.setMinimumHeight(a.dp(56));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=a.dp(14);body.addView(spinner,lp);
  return spinner;
 }
 void fuel(){
  JSONObject bike=a.bike();if(bike==null||a.readOnly){a.toast("기록 가능한 바이크를 먼저 등록해 주세요.");return;}
  LinearLayout body=a.page("주유 기록");a.label(body,bike.optString("name")+" · 오늘",13,a.muted,false);
  a.gap(body,12);
  EditText amount=number(body,"주유 금액 (원)","","예: 12000");
  amount.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_NEXT);
  EditText liters=number(body,"실제 주유량 (L)","","예: 4.5");
  liters.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
  LinearLayout tools=a.col();tools.setVisibility(View.GONE);
  LinearLayout calculator=a.col();calculator.setVisibility(View.GONE);
  EditText unitPrice=number(calculator,"리터당 가격 (원)","","예: 1750");
  a.label(calculator,"리터당 가격 × 주유량으로 예상 금액을 자동 채웁니다. 실제 결제액이 다르면 금액을 수정하세요.",12,a.muted,false);
  Button calcToggle=a.button("리터당 가격으로 자동 계산",false,
   ()->calculator.setVisibility(calculator.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE));
  tools.addView(calcToggle);tools.addView(calculator);
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
  if(recentFuel!=null&&recentFuel.optDouble("liters")>0){
   long typicalPrice=Math.round(recentFuel.optDouble("amount")/recentFuel.optDouble("liters"));
   Button lastFuel=a.button("이 바이크 최근 주유 · "+
     String.format(java.util.Locale.KOREA,"%.2f L",recentFuel.optDouble("liters"))+
     " · "+typicalPrice+"원/L 적용",false,()->{
    calculator.setVisibility(View.GONE);
    fuelAuto.reset();
    liters.setText(String.valueOf(recentFuelLitersSafe(recentFuel)));
    if(typicalPrice>0)unitPrice.setText(String.valueOf(typicalPrice));
    a.toast("지난 주유량과 단가를 참고값으로 채웠어요. 실제 결제 금액은 직접 확인하세요.");
   });
   tools.addView(lastFuel);
  }
  if(recentFuel!=null){
   final JSONObject previous=recentFuel;
   Button reuse=a.button("지난 주유 금액·리터 불러오기",false,()->{
    // Hide the calculator first: liters.setText triggers listeners synchronously.
    calculator.setVisibility(View.GONE);
    fuelAuto.reset();
    amount.setText(String.valueOf((long)previous.optDouble("amount")));
    liters.setText(String.valueOf(previous.optDouble("liters")));
   });
   tools.addView(reuse);
   double lastLiters=previous.optDouble("liters",Double.NaN);
   double lastAmount=previous.optDouble("amount",Double.NaN);
   if(Double.isFinite(lastLiters)&&lastLiters>0&&Double.isFinite(lastAmount)&&lastAmount>0){
    long lastUnitPrice=Math.round(lastAmount/lastLiters);
    if(lastUnitPrice>0&&lastUnitPrice<100_000){
     Button reuseUnitPrice=a.button("지난 리터당 가격 "+a.won(lastUnitPrice)+" 활용",false,()->{
      calculator.setVisibility(View.VISIBLE);
      unitPrice.setText(String.valueOf(lastUnitPrice));
      a.toast("지난 단가를 불러왔어요. 실제 주유소 단가를 확인해 주세요.");
     });
     reuseUnitPrice.setMinHeight(a.dp(45));tools.addView(reuseUnitPrice);
    }
   }
  }
  Button toolsToggle=a.button("지난 기록·단가 계산 도구  ▾",false,()->{
   boolean open=tools.getVisibility()!=View.VISIBLE;
   tools.setVisibility(open?View.VISIBLE:View.GONE);
  });
  body.addView(toolsToggle);body.addView(tools);
  a.label(body,"금액·리터만 입력하면 오늘 날짜와 현재 계기판으로 저장합니다.",12,a.muted,false);
  CheckBox full=new CheckBox(a);full.setText("가득 주유했어요 (연비 계산용)");
  full.setTextColor(a.fg);full.setTextSize(14);full.setMinHeight(a.dp(50));body.addView(full);
  LinearLayout extra=a.col();extra.setVisibility(View.GONE);
  EditText odo=number(extra,"현재 계기판 (km)",java.math.BigDecimal.valueOf(bike.optDouble("odo")).stripTrailingZeros().toPlainString(),"현재 주행거리");
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
  a.action(extra,"계기판 바로 촬영",false,()->{
   extra.setVisibility(View.VISIBLE);
   a.startQuickOdometerCamera(odo);
  });
  // Full-to-full efficiency requires the real odometer; don't hide it on a full fill.
  CheckBox odoConfirmed=new CheckBox(a);
  odoConfirmed.setText("실제 계기판 "+odo.getText().toString()+" km를 확인했어요");
  odoConfirmed.setTextColor(a.fg);odoConfirmed.setTextSize(14);odoConfirmed.setMinHeight(a.dp(46));
  odoConfirmed.setVisibility(View.GONE);body.addView(odoConfirmed);
  odo.addTextChangedListener(new android.text.TextWatcher(){
   public void beforeTextChanged(CharSequence t,int start,int count,int after){}
   public void onTextChanged(CharSequence t,int start,int before,int count){
    odoConfirmed.setChecked(false);
    odoConfirmed.setText("실제 계기판 "+t.toString()+" km를 확인했어요");
   }
   public void afterTextChanged(android.text.Editable e){}
  });
  full.setOnCheckedChangeListener((button,checked)->{
   if(checked)extra.setVisibility(View.VISIBLE);
   odoConfirmed.setVisibility(checked?View.VISIBLE:View.GONE);
   if(!checked)odoConfirmed.setChecked(false);
  });
  // Keep an isolated draft while switching to navigation or after Android process restart.
  final String bikeId=bike.optString("id");
  final boolean[] restoring={false};
  final boolean[] draftTouched={false};
  Runnable persistDraft=()->{
   if(restoring[0])return;
   draftTouched[0]=true;
   FuelDraft.save(a,bikeId,amount.getText().toString(),liters.getText().toString(),
    unitPrice.getText().toString(),odo.getText().toString(),date.getText().toString(),full.isChecked());
  };
  // Keep typing responsive with apply(), then synchronously commit the latest
  // state when this dialog closes or Android backgrounds the activity.
  a.quickFuelDraftFlush=()->{
   // Opening an existing unsent draft without editing must never replace it
   // with the new form's intentionally blank defaults.
   if(!draftTouched[0])return;
   if(!FuelDraft.saveDurable(a,bikeId,amount.getText().toString(),liters.getText().toString(),
     unitPrice.getText().toString(),odo.getText().toString(),date.getText().toString(),full.isChecked()))
    a.toast("주유 임시저장을 완료하지 못했어요. 저장공간을 확인해 주세요.");
  };
  android.text.TextWatcher draftWatcher=new android.text.TextWatcher(){
   public void beforeTextChanged(CharSequence v,int st,int c,int after){}
   public void onTextChanged(CharSequence v,int st,int before,int count){persistDraft.run();}
   public void afterTextChanged(android.text.Editable e){}
  };
  amount.addTextChangedListener(draftWatcher);liters.addTextChangedListener(draftWatcher);
  unitPrice.addTextChangedListener(draftWatcher);odo.addTextChangedListener(draftWatcher);
  date.addTextChangedListener(draftWatcher);full.setOnCheckedChangeListener((v,checked)->{
   if(checked)extra.setVisibility(View.VISIBLE);
   odoConfirmed.setVisibility(checked?View.VISIBLE:View.GONE);
   if(!checked)odoConfirmed.setChecked(false);
   persistDraft.run();
  });
  a.label(body,"입력하는 동안 자동으로 임시저장합니다. 저장 전 바이크·금액·날짜를 확인하세요.",12,a.muted,false);
  JSONObject previouslyTyped=FuelDraft.read(a,bikeId);
  if(previouslyTyped!=null){
   Button restoreDraftButton=a.button("주유 · 이전 임시기록 불러오기",false,()->{
    restoring[0]=true;
    try{
     amount.setText(previouslyTyped.optString("amount"));
     liters.setText(previouslyTyped.optString("liters"));
     unitPrice.setText(previouslyTyped.optString("unitPrice"));
     odo.setText(previouslyTyped.optString("odo"));
     String draftDate=previouslyTyped.optString("date",a.today());
     date.setText(draftDate);
     full.setChecked(previouslyTyped.optBoolean("full"));
     if(full.isChecked()||!a.today().equals(draftDate))extra.setVisibility(View.VISIBLE);
     odoConfirmed.setChecked(false);
     if(!a.today().equals(draftDate))
      new AlertDialog.Builder(a).setTitle("이전 주유 날짜 확인")
       .setMessage("작성 중이던 기록 날짜는 "+draftDate+"입니다. 실제로 주유한 날짜가 맞나요?")
       .setNegativeButton("오늘 날짜로 변경",(d,w)->date.setText(a.today()))
       .setPositiveButton("이전 날짜 유지",null).show();
    }finally{restoring[0]=false;}
    a.toast("임시기록을 불러왔어요. 금액·계기판·주유 날짜를 확인하세요.");
   });
   LinearLayout.LayoutParams restoreDraftLp=new LinearLayout.LayoutParams(-1,-2);
   restoreDraftLp.bottomMargin=a.dp(8);
   body.addView(restoreDraftButton,restoreDraftLp);
   a.action(body,"주유 · 임시기록 삭제",false,()->{
    FuelDraft.clear(a,bikeId);
    draftTouched[0]=false; // Explicit discard must not be undone by lifecycle flush.
    // The previous draft cannot be resurrected by a stale click handler.
    restoreDraftButton.setVisibility(View.GONE);
    a.toast("임시 주유 기록만 삭제했어요. 현재 입력칸은 그대로 유지됩니다.");
   });
  }
  a.gap(body,12);
  final long[] lastSaveClick={0};
  a.action(body,"저장",true,()->a.attempt(()->{
   long now=android.os.SystemClock.elapsedRealtime();
   if(now-lastSaveClick[0]<1200)return;
   lastSaveClick[0]=now;
   if(full.isChecked()&&!odoConfirmed.isChecked())
    throw new IllegalArgumentException("가득 주유는 실제 계기판 km를 확인한 뒤 체크해 주세요.");
   JSONObject record=QuickRecordDraft.fuel(bike.optString("id"),date.getText().toString().trim(),
    numeric(odo,"계기판",true),numeric(amount,"주유 금액",true),numeric(liters,"주유량",true),full.isChecked());
   // The user may leave the warning dialog open while another action changes the garage.
   // A confirmed fuel record may only commit against the exact state that was reviewed.
   final String reviewedSnapshot=a.db.toString();
   final boolean exact=QuickLogDuplicate.exists(a.items("expenses"),record,"expenses");
   final boolean near=FuelNearDuplicate.near(a.items("expenses"),record);
   Runnable persist=()->a.attempt(()->{
    if(a.readOnly||a.bike()==null||!bikeId.equals(a.bike().optString("id")))
     throw new java.io.IOException("바이크 또는 저장 상태가 변경됐어요. 주유 화면을 다시 확인해 주세요.");
    String beforeSave=a.db.toString();
    if(!reviewedSnapshot.equals(beforeSave))
     throw new java.io.IOException("주유 확인 중 기록이 변경됐어요. 중복 여부를 다시 확인하고 저장해 주세요.");
    if(!exact&&QuickLogDuplicate.exists(a.items("expenses"),record,"expenses"))
     throw new java.io.IOException("새로 저장된 동일 주유 기록이 있습니다. 다시 확인해 주세요.");
    if(!near&&FuelNearDuplicate.near(a.items("expenses"),record))
     throw new java.io.IOException("새로 저장된 비슷한 주유 기록이 있습니다. 다시 확인해 주세요.");
    JSONObject next=new JSONObject(beforeSave);
    JSONObject vehicle=Store.find(next,"vehicles",bikeId);
    if(vehicle==null||!bikeId.equals(record.optString("vehicleId")))
     throw new java.io.IOException("등록된 바이크 연결이 없어 주유 기록을 저장하지 않았어요.");
    Store.upsert(next,"expenses",record);
    if(record.optDouble("mileage")>vehicle.optDouble("odo"))vehicle.put("odo",record.optDouble("mileage"));
    a.write(next);a.quickFuelDraftFlush=null;FuelDraft.clear(a,bikeId);
    a.close();a.render();DailyMaintenanceJob.checkSoon(a);
    UndoLastSave.offer(a,beforeSave,a.db.toString(),"주유");
   });
   StringBuilder warnings=new StringBuilder();
   if(FuelAnomalyPolicy.lowOdometer(bike.optDouble("odo"),record.optDouble("mileage")))
    warnings.append("• 계기판이 현재 차고 값보다 작아요.\\n");
   if(FuelAnomalyPolicy.unusualLiters(record.optDouble("liters")))
    warnings.append("• 주유량이 25L를 초과해요. 입력 단위를 확인하세요.\\n");
   if(recentFuel!=null&&FuelAnomalyPolicy.unusualAmount(recentFuel.optDouble("amount"),record.optDouble("amount")))
    warnings.append("• 직전 주유 금액과 차이가 매우 커요.\\n");
   if(FuelAnomalyPolicy.staleDate(record.optString("date"),a.today()))
    warnings.append("• 주유 날짜가 오늘이 아닌 "+record.optString("date")+"입니다.\\n");
   if(exact||near)warnings.append(exact?"• 동일한 주유 기록이 있어요.\\n":"• 비슷한 주유 기록이 있어요.\\n");
   if(warnings.length()>0)
    new AlertDialog.Builder(a).setTitle("주유 기록 최종 확인")
     .setMessage("바이크: "+bike.optString("name")+
      "\\n날짜: "+record.optString("date")+
      "\\n주유: "+String.format(java.util.Locale.KOREA,"%.2f L · %,.0f원",record.optDouble("liters"),record.optDouble("amount"))+
      "\\n계기판: "+String.format(java.util.Locale.KOREA,"%.1f km",record.optDouble("mileage"))+
      "\\n\\n"+warnings.toString().replace("\\\\n","\\n")+
      "\\n실제 주유 기록이 맞으면 저장해 주세요. 숫자는 자동 수정되지 않습니다.")
     .setNegativeButton("다시 수정",null)
     .setPositiveButton("내용 확인하고 저장",(dialog,which)->persist.run()).show();
   else persist.run();
  }));
  // The primary save button is already in the dialog footer; keyboard Done invokes it.
  liters.setOnEditorActionListener((view,actionId,event)->{
   if(actionId==android.view.inputmethod.EditorInfo.IME_ACTION_DONE){
    a.hideKeyboard();
    if(a.formFooter!=null&&a.formFooter.getChildCount()>0){
     a.formFooter.getChildAt(a.formFooter.getChildCount()-1).performClick();
    }
    return true;
   }
   return false;
  });
  a.show();
  amount.requestFocus();
  amount.postDelayed(()->{
   if(a.dialog!=null&&a.dialog.isShowing()&&amount.isAttachedToWindow()){
    android.view.inputmethod.InputMethodManager imm=
     (android.view.inputmethod.InputMethodManager)a.getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
    if(imm!=null)imm.showSoftInput(amount,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
   }
  },200);
 }
 private static double recentFuelLitersSafe(JSONObject recent){
  double liters=recent.optDouble("liters",0);
  return Double.isFinite(liters)&&liters>0&&liters<1000?liters:0;
 }
 void maintenance(){
  JSONObject bike=a.bike();if(bike==null||a.readOnly){a.toast("기록 가능한 바이크를 먼저 등록해 주세요.");return;}
  LinearLayout body=a.page("정비주기 등록");
  a.label(body,bike.optString("name")+" · 기준 주행거리 "+a.num(bike.optDouble("odo"))+" km",13,a.muted,false);a.gap(body,13);
  String[] items={"엔진오일","오일필터","체인","타이어","브레이크 패드","냉각수","배터리","기타 직접 입력"};
  Spinner service=options(body,"정비 항목",items);
  for(JSONObject previousService:a.items("services")){
   if(!bike.optString("id").equals(previousService.optString("vehicleId")))continue;
   int match=java.util.Arrays.asList(items).indexOf(previousService.optString("type"));
   if(match>=0&&match<items.length-1){
    Button recent=a.button("이 바이크 최근 정비 항목 · "+items[match]+" 선택",false,()->service.setSelection(match));
    body.addView(recent);break;
   }
  }
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
  LinearLayout alertOptions=a.col();alertOptions.setVisibility(View.GONE);
  Spinner kmAlert=options(alertOptions,"거리 알림 시점",
   new String[]{"100km 전","300km 전","500km 전"});
  kmAlert.setSelection(2);
  Spinner dateAlert=options(alertOptions,"날짜 알림 시점",
   new String[]{"7일 전","15일 전","30일 전"});
  dateAlert.setSelection(2);
  final int[] kmBeforeChoices={100,300,500},daysBeforeChoices={7,15,30};
  Button alerts=a.button("알림 시점 상세 설정",false,
   ()->alertOptions.setVisibility(alertOptions.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE));
  body.addView(alerts);body.addView(alertOptions);
  LinearLayout advanced=a.col();advanced.setVisibility(View.GONE);
  EditText mileage=number(advanced,"현재 계기판 (km)",java.math.BigDecimal.valueOf(bike.optDouble("odo")).stripTrailingZeros().toPlainString(),"km");
  EditText nextKm=number(advanced,"직접 지정하는 다음 정비 거리 (km)","","예: 14000");
  a.label(advanced,"직접 지정하는 다음 정비일",14,a.fg,true);
  EditText nextDate=new EditText(a);nextDate.setText(LocalDate.parse(a.today()).plusMonths(1).toString());nextDate.setTextColor(a.fg);nextDate.setSingleLine(true);
  nextDate.setFocusable(false);nextDate.setClickable(true);nextDate.setMinHeight(a.dp(54));
  nextDate.setBackground(a.shape(a.panel,a.line,11));nextDate.setPadding(a.dp(13),0,a.dp(13),0);
  nextDate.setOnClickListener(v->a.pickDate(nextDate));advanced.addView(nextDate);
  Button advancedToggle=a.button("계기판·직접 지정 수정",false,
   ()->advanced.setVisibility(advanced.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE));
  body.addView(advancedToggle);body.addView(advanced);
  // Explicitly reuse the last SAME-BIKE/SAME-TYPE service interval.
  // The previous date, mileage, amount and motorcycle are never copied.
  Button reuse=a.button("이전 정비주기 불러오기",false,()->a.attempt(()->{
   String type=service.getSelectedItemPosition()==items.length-1
    ?customType.getText().toString().trim():service.getSelectedItem().toString();
   JSONObject last=null;
   for(JSONObject item:a.items("services")){
    if(!bike.optString("id").equals(item.optString("vehicleId"))
      ||!type.equals(item.optString("type")))continue;
    if(last==null||RecentRecordReuse.isNewer(item.optString("date"),item.optDouble("mileage"),
      last.optString("date"),last.optDouble("mileage")))last=item;
   }
   if(last==null){a.toast("같은 항목의 정비 이력이 없어요.");return;}
   int km=last.optInt("intervalKm",0),monthsCount=last.optInt("intervalMonths",0);
   if(km<=0&&monthsCount<=0){a.toast("이전 기록에 설정된 정비주기가 없어요.");return;}
   int distanceIndex=0,monthIndex=0;
   for(int i=1;i<intervals.length-1;i++)if(km==intervals[i])distanceIndex=i;
   for(int i=1;i<months.length-1;i++)if(monthsCount==months[i])monthIndex=i;
   if(km>0&&distanceIndex==0){
    distanceIndex=intervals.length-1;
    nextKm.setText(String.valueOf((long)QuickRecordDraft.nextOdometer(
      numeric(mileage,"현재 계기판",true),km)));
    advanced.setVisibility(View.VISIBLE);
   }
   if(monthsCount>0&&monthIndex==0){
    monthIndex=months.length-1;
    nextDate.setText(LocalDate.parse(a.today()).plusMonths(monthsCount).toString());
    advanced.setVisibility(View.VISIBLE);
   }
   distanceChoice.setSelection(distanceIndex);dateChoice.setSelection(monthIndex);
   int kmBefore=last.optInt("alertKmBefore",500),days=last.optInt("alertDaysBefore",30);
   kmAlert.setSelection(kmBefore<=100?0:kmBefore<=300?1:2);
   dateAlert.setSelection(days<=7?0:days<=15?1:2);
   a.toast("이전 주기만 적용했어요. 저장 전 계기판과 예정일을 확인하세요.");
  }));
  reuse.setMinHeight(a.dp(48));body.addView(reuse);
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
  a.gap(body,12);a.label(body,"알림은 입력한 계기판과 날짜를 기준으로 계산합니다. 정비 주기는 제조사 매뉴얼을 확인하세요.",13,a.muted,false);
  a.action(body,"저장",true,()->a.attempt(()->{
   int kmInterval=intervals[distanceChoice.getSelectedItemPosition()];
   int monthsInterval=months[dateChoice.getSelectedItemPosition()];
   if(kmInterval==0&&monthsInterval==0)
    throw new IllegalArgumentException("거리나 날짜 중 하나 이상 정비주기를 선택해 주세요.");
   double odo=numeric(mileage,"현재 계기판",true);
   double dueKm=kmInterval<0?numeric(nextKm,"다음 정비 거리",true):QuickRecordDraft.nextOdometer(odo,kmInterval);
   String dueDate=monthsInterval<0?nextDate.getText().toString().trim():QuickRecordDraft.dueAfterMonths(a.today(),monthsInterval);
   String type=service.getSelectedItemPosition()==items.length-1?customType.getText().toString().trim():service.getSelectedItem().toString();
   JSONObject record=QuickRecordDraft.maintenance(bike.optString("id"),type,a.today(),odo,dueKm,dueDate,
    Math.max(0,kmInterval),Math.max(0,monthsInterval));
   record.put("alertKmBefore",kmBeforeChoices[kmAlert.getSelectedItemPosition()]);
   record.put("alertDaysBefore",daysBeforeChoices[dateAlert.getSelectedItemPosition()]);
   // Snapshot the exact data reviewed before opening the duplicate-confirmation
   // dialog. A stale dialog must never silently overwrite a changed garage.
   final String reviewedServiceSnapshot=a.db.toString();
   final String maintenanceBikeId=bike.optString("id");
   final boolean reviewedDuplicate=QuickLogDuplicate.exists(a.items("services"),record,"services");
   Runnable save=()->a.attempt(()->{
    if(a.readOnly||a.bike()==null||!maintenanceBikeId.equals(a.bike().optString("id")))
     throw new java.io.IOException("바이크 또는 저장 상태가 변경됐어요. 정비주기를 다시 확인해 주세요.");
    String beforeSave=a.db.toString();
    if(!reviewedServiceSnapshot.equals(beforeSave))
     throw new java.io.IOException("정비주기를 확인하는 동안 기록이 변경됐어요. 다시 열어서 확인해 주세요.");
    if(!reviewedDuplicate&&QuickLogDuplicate.exists(a.items("services"),record,"services"))
     throw new java.io.IOException("같은 정비주기가 새로 저장됐어요. 중복 여부를 다시 확인해 주세요.");
    JSONObject next=new JSONObject(beforeSave);
    JSONObject vehicle=Store.find(next,"vehicles",maintenanceBikeId);
    if(vehicle==null||!maintenanceBikeId.equals(record.optString("vehicleId")))
     throw new java.io.IOException("정비주기를 연결할 바이크를 찾지 못했어요.");
    if(odo<vehicle.optDouble("odo"))
     throw new java.io.IOException("차고의 현재 계기판보다 이전 값이에요. 정비주기를 다시 확인해 주세요.");
    Store.upsert(next,"services",record);
    if(odo>vehicle.optDouble("odo"))vehicle.put("odo",odo);
    a.write(next);a.close();a.render();DailyMaintenanceJob.checkSoon(a);
    UndoLastSave.offer(a,beforeSave,a.db.toString(),"정비주기",()->{
     if(!DailyMaintenanceJob.remindersEnabled(a))askEnableNotifications();
    });
   });
   if(reviewedDuplicate){
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
