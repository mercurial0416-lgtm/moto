package com.motohub.next;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.Uri;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

/** One-touch ride controls, imported GPX and real saved-ride insights. */
final class RideUi {
 static final class HistoryEntry {
  final File file;final String date,status;
  HistoryEntry(File file,String date,String status){this.file=file;this.date=date;this.status=status;}
 }
 final MainActivity a;RideUi(MainActivity a){this.a=a;}
 void render(){
  LinearLayout hero=a.col();hero.setPadding(a.dp(18),a.dp(18),a.dp(18),a.dp(18));hero.setBackground(a.shape(a.panel,a.line,19));
  TextView eyebrow=a.text(RideService.running?"●  GPS 기록 중":"●  라이딩 준비 완료",13,RideService.running?0xff48d99a:a.accent,true);hero.addView(eyebrow);
  TextView title=a.text(RideService.running?RideService.count+"개 좌표 저장":"READY TO RIDE",25,a.fg,true);LinearLayout.LayoutParams t=new LinearLayout.LayoutParams(-1,-2);t.topMargin=a.dp(11);hero.addView(title,t);
  TextView sub=a.text(RideService.running?"화면을 꺼도 기기에 GPS 경로를 기록해요":"출발 버튼 한 번으로 기록을 시작해요",13,a.muted,false);sub.setPadding(0,a.dp(9),0,a.dp(12));hero.addView(sub);
  RideReadiness.State readiness=readiness();
  if(!RideService.running){
   if(readiness!=RideReadiness.State.READY){
    LinearLayout warning=a.col();warning.setPadding(a.dp(13),a.dp(13),a.dp(13),a.dp(13));warning.setBackground(a.shape(a.bg,a.line,12));
    warning.addView(a.text(RideReadiness.title(readiness),15,a.fg,true));a.gap(warning,6);
    warning.addView(a.text(RideReadiness.guidance(readiness),13,a.muted,false));a.gap(warning,10);
    String action=RideReadiness.action(readiness);
    if(!action.isEmpty())warning.addView(a.button(action,false,()->handleReadiness(readiness)));
    hero.addView(warning);a.gap(hero,12);
   }
   if(!RideService.status.equals("기록 꺼짐")&&!RideService.status.equals("기록 종료")){
    TextView last=a.text("최근 GPS 상태 · "+RideService.status,12,a.muted,false);
    last.setPadding(0,0,0,a.dp(12));hero.addView(last);
   }
  }
  if(RideService.running){
   LinearLayout live=new LinearLayout(a);
   TextView[] values=new TextView[3];String[] names={"주행거리","경과시간","실시간 속도"};
   for(int i=0;i<3;i++){
    LinearLayout box=a.col();box.setPadding(a.dp(9),a.dp(12),a.dp(9),a.dp(12));box.setBackground(a.shape(a.bg,0,13));
    values[i]=a.text("—",19,a.fg,true);values[i].setGravity(Gravity.CENTER);box.addView(values[i]);
    TextView name=a.text(names[i],11,a.muted,false);name.setGravity(Gravity.CENTER);
    LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2);np.topMargin=a.dp(7);box.addView(name,np);
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);if(i>0)lp.leftMargin=a.dp(7);live.addView(box,lp);
   }hero.addView(live);a.gap(hero,12);
   Runnable refresh=new Runnable(){@Override public void run(){
    if(!RideService.running||hero.getParent()==null)return;
    long seconds=Math.max(0,(System.currentTimeMillis()-RideService.startedAt)/1000);
    values[0].setText(String.format(Locale.KOREA,"%.1f km",RideService.liveDistanceKm));
    values[1].setText(String.format(Locale.KOREA,"%02d:%02d",seconds/3600,seconds/60%60));
    values[2].setText(Math.round(RideService.liveSpeedKmh)+" km/h");
    hero.postDelayed(this,1000);
   }};hero.post(refresh);
  }

  Button start=a.button(RideService.running?"■  라이딩 종료":"➤  GPS 라이딩 시작",true,()->{
   if(RideService.running){new AlertDialog.Builder(a).setTitle("라이딩 기록 종료").setMessage("기록을 끝내고 주행 분석에서 이동거리와 경로를 확인할 수 있어요.").setNegativeButton("취소",null).setPositiveButton("종료",(d,w)->{
    a.startService(new Intent(a,RideService.class).setAction(RideService.STOP));a.root.postDelayed(()->{if(!a.isFinishing())a.render();},900);
   }).show();}else requestStart();
  });start.setTextColor(Color.WHITE);start.setBackground(a.shape(RideService.running?0xffda443c:0xffff6b1b,0,14));start.setMinHeight(a.dp(58));hero.addView(start);
  a.content.addView(hero);a.gap(a.content,16);
  LinearLayout tools=new LinearLayout(a);
  Button importButton=a.button("↧  GPX 가져오기",false,a::pickGpx);Button refresh=a.button("↻  새로고침",false,a::render);
  tools.addView(importButton,new LinearLayout.LayoutParams(0,a.dp(50),1));LinearLayout.LayoutParams second=new LinearLayout.LayoutParams(0,a.dp(50),1);second.leftMargin=a.dp(8);tools.addView(refresh,second);a.content.addView(tools);
  a.gap(a.content,8);
  a.label(a.content,"GPX 가져오기: 다른 앱에서 저장한 코스 파일을 지도와 주행 기록으로 불러옵니다. 가져오기만으로 GPS 기록이나 길 안내가 시작되지는 않아요.",12,a.muted,false);
  a.gap(a.content,12);
  a.label(a.content,"기록된 라이딩",20,a.fg,true);
  try{
   List<File> rides=TrackFile.tracks(a.getFilesDir());
   String activeName=RideService.active;
   boolean recording=RideService.running;
   String selected=RideHistoryFilter.normalize(a.rideHistoryFilter);
   a.rideHistoryFilter=selected;
   // Horizontally scrollable, labelled status chips avoid squeezing six actions onto a phone screen.
   HorizontalScrollView strip=new HorizontalScrollView(a);
   strip.setHorizontalScrollBarEnabled(false);
   strip.setFillViewport(false);
   LinearLayout chips=new LinearLayout(a);
   chips.setOrientation(LinearLayout.HORIZONTAL);
   chips.setPadding(0,a.dp(4),0,a.dp(6));
   Map<String,Integer> counts=RideHistoryFilter.counts(rides,activeName,recording);
   for(String option:RideHistoryFilter.OPTIONS){
    int count=counts.get(option);
    boolean checked=option.equals(selected);
    Button chip=a.button(option+"  "+count,checked,()->{a.selectRideHistoryFilter(option);});
    chip.setTextSize(13);
    chip.setMinHeight(a.dp(44));
    chip.setContentDescription("라이딩 "+option+" 필터, "+count+"건"+(checked?", 선택됨":""));
    chip.setSelected(checked);
    LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-2,a.dp(44));
    cp.rightMargin=a.dp(7);
    chips.addView(chip,cp);
   }
   strip.addView(chips);
   a.content.addView(strip);
   List<File> visible=RideHistorySort.order(RideHistoryFilter.select(rides,selected,activeName,recording),a.rideHistorySort);
   // Search only already-known local titles and statuses; no GPS content or network requests.
   List<HistoryEntry> entries=new ArrayList<>();
   for(File file:visible)entries.add(new HistoryEntry(file,RideHistoryLabel.title(file,java.time.ZoneId.systemDefault()),RideHistoryFilter.status(file,activeName,recording)));
   LinearLayout searchBar=new LinearLayout(a);searchBar.setGravity(Gravity.CENTER_VERTICAL);
   EditText search=new EditText(a);search.setSingleLine(true);search.setTextSize(15);
   search.setTextColor(a.fg);search.setHintTextColor(a.muted);
   search.setHint("날짜·시간·상태 검색 (예: 10/17, GPX)");
   search.setContentDescription("라이딩 기록 검색");
   search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
   search.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
   search.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(RideHistorySearch.MAX_LENGTH)});
   search.setPadding(a.dp(14),a.dp(10),a.dp(14),a.dp(10));
   search.setBackground(a.shape(a.panel,a.line,13));
   searchBar.addView(search,new LinearLayout.LayoutParams(0,a.dp(50),1));
   Button clear=a.button("지우기",false,()->search.setText(""));
   clear.setTextSize(12);clear.setMinHeight(a.dp(44));
   clear.setContentDescription("라이딩 검색어 지우기");
   LinearLayout.LayoutParams clearLp=new LinearLayout.LayoutParams(-2,a.dp(50));clearLp.leftMargin=a.dp(7);
   searchBar.addView(clear,clearLp);a.content.addView(searchBar);a.gap(a.content,8);
   LinearLayout historyTools=new LinearLayout(a);
   historyTools.setGravity(Gravity.CENTER_VERTICAL);
   TextView summary=a.text("표시 "+visible.size()+"건 / 전체 "+rides.size()+"건",12,a.muted,false);
   historyTools.addView(summary,new LinearLayout.LayoutParams(0,-2,1));
   Button sortButton=a.button("정렬: "+a.rideHistorySort+"  ▾",false,()->{
    String[] options=RideHistorySort.OPTIONS;
    int checked=RideHistorySort.OLDEST.equals(a.rideHistorySort)?1:0;
    new AlertDialog.Builder(a).setTitle("라이딩 날짜 정렬")
     .setSingleChoiceItems(options,checked,(dialog,which)->{
      dialog.dismiss();a.selectRideHistorySort(options[which]);
     }).setNegativeButton("취소",null).show();
   });
   sortButton.setTextSize(12);
   sortButton.setMinHeight(a.dp(44));
   sortButton.setContentDescription("라이딩 정렬 기준 "+a.rideHistorySort+", 변경하기");
   historyTools.addView(sortButton,new LinearLayout.LayoutParams(-2,a.dp(44)));
   a.content.addView(historyTools);
   LinearLayout results=a.col();a.content.addView(results);
   search.setText(a.rideHistoryQuery);
   search.setSelection(search.length());
   clear.setEnabled(!a.rideHistoryQuery.isEmpty());
   clear.setAlpha(a.rideHistoryQuery.isEmpty()?0.5f:1f);
   search.setOnEditorActionListener((v,id,event)->{a.hideKeyboard();return true;});
   search.addTextChangedListener(new android.text.TextWatcher(){
    public void beforeTextChanged(CharSequence value,int start,int count,int after){}
    public void onTextChanged(CharSequence value,int start,int before,int count){
     a.rideHistoryQuery=RideHistorySearch.limit(value.toString());
     clear.setEnabled(!a.rideHistoryQuery.isEmpty());
     clear.setAlpha(a.rideHistoryQuery.isEmpty()?0.5f:1f);
     showHistoryResults(results,entries,rides.size(),selected,summary);
    }
    public void afterTextChanged(android.text.Editable value){}
   });
   showHistoryResults(results,entries,rides.size(),selected,summary);
  }catch(Exception e){a.note("GPS 기록을 불러오지 못했어요. 저장 공간을 확인해 주세요.");}
  a.gap(a.content,12);
  a.label(a.content,"위치 기록과 공유는 별개야. 친구와 공유하려면 친구 탭에서 직접 켜야 해요. 주행 중에는 휴대폰을 조작하지 마세요.",13,a.muted,false);
 }
 void showHistoryResults(LinearLayout results,List<HistoryEntry> entries,int total,String selected,TextView summary){
  results.removeAllViews();
  int count=0;
  String query=a.rideHistoryQuery;
  for(HistoryEntry entry:entries)if(RideHistorySearch.matches(entry.date,entry.status,query))count++;
  summary.setText("표시 "+count+"건 / 전체 "+total+"건");
  if(count==0){
   String message=total==0?"아직 저장된 주행이 없어요. GPS를 시작하거나 GPX 파일을 가져와 주세요.":
    entries.isEmpty()?"'"+selected+"' 상태의 라이딩이 없어요. 다른 필터를 선택해 주세요.":
    "검색 결과가 없어요. 날짜·시간·상태를 바꾸거나 검색어를 지워 주세요.";
   TextView empty=a.text(message,14,a.muted,false);
   empty.setPadding(a.dp(15),a.dp(16),a.dp(15),a.dp(16));empty.setBackground(a.shape(a.panel,0,14));
   results.addView(empty);return;
  }
  for(HistoryEntry entry:entries){
   if(!RideHistorySearch.matches(entry.date,entry.status,query))continue;
   File f=entry.file;String date=entry.date,status=entry.status;
   boolean live=RideHistoryFilter.ACTIVE.equals(status);
   boolean interrupted=RideHistoryFilter.INTERRUPTED.equals(status);
   String detail=live?"현재 기록 중 · 종료 후 상세 확인":RideHistoryFilter.IMPORTED.equals(status)?"가져온 GPX · 분석과 지도 보기":RideHistoryFilter.COMPLETE.equals(status)?"주행 완료 · 거리·시간·지도":RideHistoryFilter.RECOVERED.equals(status)?"복구된 라이딩 · 거리·시간·지도":"중단된 기록 · 복구하고 분석하기";
    new HubScreens(a).line(results,date,detail,live?"기록 중":interrupted?"복구  ›":"자세히  ›",()->{
     if(live){a.toast("기록을 종료하면 주행 요약을 볼 수 있어요.");return;}
     if(interrupted){
      new AlertDialog.Builder(a).setTitle("중단된 GPS 기록 복구")
       .setMessage("강제 종료나 GPS 오류로 중단된 기록이야. 정상 저장된 좌표만 보존하고, 쓰다 만 마지막 행이 있다면 제거해. 원본을 교체하므로 먼저 전체 ZIP 백업을 권장해.")
       .setNegativeButton("취소",null)
       .setNeutralButton("복구 없이 분석",(d,w)->a.openRideInsights(f))
       .setPositiveButton("복구",(d,w)->new Thread(()->{
        try{TrackRecovery.Result result=TrackRecovery.recover(f);
         a.runOnUiThread(()->{if(a.isFinishing()||a.isDestroyed())return;a.toast("복구 완료 · "+result.points+"개 좌표 · 제거 "+result.discardedBytes+"바이트");a.render();a.openRideInsights(f);});
        }catch(Exception e){a.runOnUiThread(()->{if(!a.isFinishing()&&!a.isDestroyed())a.error("GPS 복구 실패: "+e.getMessage());});}
       },"MOTO-ride-recovery").start()).show();
      return;
     }
     a.openRideInsights(f);
    });
  }
 }
 RideReadiness.State readiness(){
  boolean fine=a.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;
  LocationManager manager=a.getSystemService(LocationManager.class);
  boolean gps=manager!=null&&manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
  boolean notifyPermission=Build.VERSION.SDK_INT<33||a.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;
  android.app.NotificationManager nm=a.getSystemService(android.app.NotificationManager.class);
  boolean notifyEnabled=nm!=null&&nm.areNotificationsEnabled();
  return RideReadiness.check(RideService.running,fine,gps,notifyPermission,notifyEnabled);
 }
 void handleReadiness(RideReadiness.State state){
  if(state==RideReadiness.State.GPS_DISABLED){
   a.attempt(()->a.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)));return;
  }
  if(state==RideReadiness.State.NOTIFICATIONS_DISABLED){
   a.attempt(()->a.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+a.getPackageName()))));return;
  }
  if(state==RideReadiness.State.LOCATION_PERMISSION||state==RideReadiness.State.NOTIFICATION_PERMISSION)requestStart();
 }
 void requestStart(){
  RideReadiness.State state=readiness();
  if(state==RideReadiness.State.GPS_DISABLED||state==RideReadiness.State.NOTIFICATIONS_DISABLED){handleReadiness(state);return;}
  new AlertDialog.Builder(a).setTitle("GPS 경로를 기록할까요?").setMessage("정확한 위치를 라이딩 중 기기에 저장합니다. 화면이 꺼져도 알림과 함께 기록하며 종료하면 위치 수집을 중단합니다. 친구에게 전송하지 않습니다.").setNegativeButton("취소",null).setPositiveButton("동의하고 시작",(d,w)->{a.rideStartConsent.approve();start();}).show();}
 void start(){
  if(!a.rideStartConsent.pending()||!"라이딩".equals(a.tab))return;
  RideReadiness.State state=readiness();
  if(state==RideReadiness.State.GPS_DISABLED||state==RideReadiness.State.NOTIFICATIONS_DISABLED){a.rideStartConsent.cancel();handleReadiness(state);return;}
  if(a.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){a.requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,android.Manifest.permission.ACCESS_COARSE_LOCATION},70);return;}
  if(Build.VERSION.SDK_INT>=33&&a.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){a.requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},71);return;}
  if(readiness()!=RideReadiness.State.READY){a.rideStartConsent.cancel();a.toast("GPS 준비 상태가 변경됐어요. 확인 후 다시 시작해 주세요.");return;}
  if(!a.rideStartConsent.consume("라이딩".equals(a.tab),true))return;
  a.attempt(()->{a.startForegroundService(new Intent(a,RideService.class));a.toast("GPS 시작을 요청했어요.");a.root.postDelayed(()->{if(!a.isFinishing())a.render();},800);});
 }
}
