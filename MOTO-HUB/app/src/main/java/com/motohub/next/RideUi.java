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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/** One-touch ride controls, imported GPX and real saved-ride insights. */
final class RideUi {
 static final class HistoryEntry {
  final File file;final String date,status,searchKey;
  HistoryEntry(File file,String date,String status){this.file=file;this.date=date;this.status=status;this.searchKey=RideHistorySearch.index(date,status);}
 }
 /** One background reader prevents file-system I/O from freezing the history screen. */
 private static final ExecutorService HISTORY_EXECUTOR=Executors.newSingleThreadExecutor(r->{
  Thread thread=new Thread(r,"moto-history-reader");thread.setDaemon(true);return thread;
 });
 private static final AtomicLong HISTORY_GENERATION=new AtomicLong();
 private static final AtomicLong MERGE_GENERATION=new AtomicLong();
 private int historyVisibleLimit=RideHistoryPage.PAGE_SIZE;
 private static final class HistoryData {
  final List<File> rides;final List<HistoryEntry> entries;
  final Map<String,Integer> counts;final String selected;final int damaged,unfinished;
  HistoryData(List<File> rides,List<HistoryEntry> entries,Map<String,Integer> counts,String selected,int damaged,int unfinished){
   this.rides=rides;this.entries=entries;this.counts=counts;this.selected=selected;this.damaged=damaged;this.unfinished=unfinished;
  }
 }
 final MainActivity a;RideUi(MainActivity a){this.a=a;}
 void render(){
  final boolean saving=RideService.saving();
  LinearLayout hero=a.col();hero.setPadding(a.dp(20),a.dp(22),a.dp(20),a.dp(20));hero.setBackground(a.shape(a.panel,a.line,20));hero.setElevation(a.dp(1));
  TextView eyebrow=a.text(saving?"GPS 저장 중":RideService.running?"GPS 기록 중":RideService.autoArmed?"자동 출발 감지 중":"GPS 준비 완료",13,RideService.running?a.accent:a.accent,true);hero.addView(eyebrow);
  TextView title=a.text(saving?"주행 기록 저장 중":RideService.running?RideService.count+"개 위치 기록":RideService.autoArmed?"자동 출발 대기":"라이딩 시작하기",24,a.fg,true);title.setTypeface(MotoDesign.display());LinearLayout.LayoutParams t=new LinearLayout.LayoutParams(-1,-2);t.topMargin=a.dp(13);hero.addView(title,t);
  TextView sub=a.text(saving?"경로를 확인하고 기기에 저장하고 있어요.":RideService.running?"화면을 꺼도 기기에 GPS 경로를 기록해요":RideService.autoArmed?"OBD2 속도가 감지되면 GPS 기록이 자동 시작돼요":"위치 정보는 휴대폰에만 저장됩니다.",13,a.muted,false);sub.setPadding(0,a.dp(9),0,a.dp(12));hero.addView(sub);
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
    LinearLayout box=a.col();box.setPadding(a.dp(9),a.dp(12),a.dp(9),a.dp(12));box.setBackground(a.shape(a.highlight,0,15));
    values[i]=a.text("—",19,a.fg,true);values[i].setGravity(Gravity.CENTER);box.addView(values[i]);
    TextView name=a.text(names[i],11,a.muted,false);name.setGravity(Gravity.CENTER);
    LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2);np.topMargin=a.dp(7);box.addView(name,np);
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);if(i>0)lp.leftMargin=a.dp(7);live.addView(box,lp);
   }hero.addView(live);
   TextView average=a.text("평균속도 계산 중",13,a.muted,false);average.setPadding(a.dp(6),a.dp(10),0,a.dp(8));hero.addView(average);
   TextView peak=a.text("최고속도 계산 중",13,a.muted,false);peak.setPadding(a.dp(6),0,0,a.dp(9));hero.addView(peak);
   a.gap(hero,12);
   Runnable refresh=new Runnable(){@Override public void run(){
    if(!hero.isAttachedToWindow()||a.isFinishing()||a.isDestroyed())return;
    if(!RideService.running){a.render();return;}
    long seconds=Math.max(0,(android.os.SystemClock.elapsedRealtime()-RideService.startedElapsed)/1000);
    values[0].setText(String.format(Locale.KOREA,"%.1f km",RideService.liveDistanceKm));
    values[1].setText(String.format(Locale.KOREA,"%02d:%02d",seconds/3600,seconds/60%60));
    values[2].setText(Math.round(RideService.liveSpeedKmh)+" km/h");
    average.setText("평균속도 " + RideMetrics.averageText(RideService.liveDistanceKm,seconds));
    peak.setText(String.format(Locale.KOREA,"최고속도 %.0f km/h · GPS 추정값",RideService.livePeakKmh)+" · "+RideService.status);
    hero.postDelayed(this,1000);
   }};hero.post(refresh);
  }

  Button start=a.button(RideService.autoArmed?"자동 감지 종료":RideService.running?"라이딩 종료":"GPS 기록 시작",true,()->{
   if(RideService.autoArmed){new AlertDialog.Builder(a).setTitle("OBD2 자동 기록을 종료할까요?").setMessage("OBD2 자동 출발 감지만 끕니다. 진행 중인 GPS 기록은 계속되며, 종료하려면 라이딩 종료를 눌러 주세요.").setNegativeButton("취소",null).setPositiveButton("중지",(d,w)->{a.startService(new Intent(a,RideService.class).setAction(RideService.AUTO_DISARM));a.root.postDelayed(()->{if(!a.isFinishing())a.render();},900);}).show();return;}
   if(RideService.running){new AlertDialog.Builder(a).setTitle("라이딩 기록 종료").setMessage("기록을 끝내고 주행 분석에서 이동거리와 경로를 확인할 수 있어요.").setNegativeButton("취소",null).setPositiveButton("종료",(d,w)->{
    a.startService(new Intent(a,RideService.class).setAction(RideService.STOP));a.root.postDelayed(()->{if(!a.isFinishing())a.render();},900);
   }).show();}else requestStart();
  });start.setTextColor(Color.WHITE);start.setBackground(a.shape(RideService.running?0xffda443c:a.accent,0,17));start.setMinHeight(a.dp(60));start.setEnabled(!saving);start.setAlpha(saving?0.6f:1.0f);hero.addView(start);
  // Auto detection and GPS recording are independent. When both are active,
  // expose a direct way to finish the ride instead of hiding it behind a
  // developer-mode disarm button or relying exclusively on a notification.
  if(RideService.autoArmed&&RideService.running){
   a.gap(hero,8);
   Button finishRide=a.button("라이딩 저장 후 종료",false,()->{
    new AlertDialog.Builder(a).setTitle("자동 라이딩 기록 종료")
     .setMessage("진행 중인 GPS 기록을 저장하고 OBD2 자동 감지도 종료합니다. 그룹 위치 공유는 시작하지 않습니다.")
     .setNegativeButton("취소",null)
     .setPositiveButton("기록 종료",(d,w)->{
      a.startService(new Intent(a,RideService.class).setAction(RideService.STOP));
      a.root.postDelayed(()->{if(!a.isFinishing())a.render();},900);
     }).show();
   });
   finishRide.setMinHeight(a.dp(48));finishRide.setEnabled(!saving);
   hero.addView(finishRide);
  }
  a.content.addView(hero);a.gap(a.content,17);
  // External navigation remains separate: no Kakao app key or GPS upload is required.
  a.action(a.content,"카카오내비 열기",false,()->KakaoNaviLaunch.open(a));
  a.label(a.content,"카카오내비는 길 안내, MOTO HUB는 로컬 라이딩 기록을 담당합니다. 경로·위치 정보는 자동 전달되지 않아요.",12,a.muted,false);
  if(RideService.autoArmed&&a.getSharedPreferences("developer_experiments",0).getBoolean("enabled",false)){
   LinearLayout diagnostics=a.col();diagnostics.setPadding(a.dp(14),a.dp(14),a.dp(14),a.dp(14));diagnostics.setBackground(a.shape(a.panel,a.line,14));
   TextView telem=a.text("OBD2 데이터를 읽는 중…",14,a.fg,false);diagnostics.addView(telem);
   a.content.addView(diagnostics);a.gap(a.content,12);
   Runnable statusUpdate=new Runnable(){@Override public void run(){
    if(!diagnostics.isAttachedToWindow()||!RideService.autoArmed||a.isFinishing()||a.isDestroyed())return;
    telem.setText("OBD2 · "+RideService.obdLink+"\n"+
     "차량 속도 "+ObdTelemetryFreshness.speedLabel(RideService.obdSpeedKmh,RideService.obdLastSampleElapsed,android.os.SystemClock.elapsedRealtime())+
     "  · RPM "+(Double.isFinite(RideService.obdRpm)?String.format(Locale.KOREA,"%.0f",RideService.obdRpm):"—")+
     "\n수온 "+(Double.isFinite(RideService.obdCoolantC)?String.format(Locale.KOREA,"%.0f°C",RideService.obdCoolantC):"—")+
     "  · ECU 전압 "+(Double.isFinite(RideService.obdVoltage)?String.format(Locale.KOREA,"%.1fV",RideService.obdVoltage):"—")+
     "\n계기판 누적거리(ODO)는 별도 확인 필요 · 읽기 전용");
    diagnostics.postDelayed(this,2500);
   }};diagnostics.post(statusUpdate);
  }
  a.gap(a.content,4);
  LinearLayout tools=new LinearLayout(a);
  Button importButton=a.button("GPX 가져오기",false,a::pickGpx);Button refresh=a.button("새로고침",false,a::render);
  tools.addView(importButton,new LinearLayout.LayoutParams(0,a.dp(50),1));LinearLayout.LayoutParams second=new LinearLayout.LayoutParams(0,a.dp(50),1);second.leftMargin=a.dp(8);tools.addView(refresh,second);a.content.addView(tools);
  a.gap(a.content,8);
  a.label(a.content,"GPX 가져오기: 다른 앱에서 저장한 코스 파일을 지도와 주행 기록으로 불러옵니다. 가져오기만으로 GPS 기록이나 길 안내가 시작되지는 않아요.",12,a.muted,false);
  a.gap(a.content,12);
  a.action(a.content,"최근 30일 주행 통계 · 바이크별",false,this::chooseRideReport);
  a.action(a.content,"라이딩 2건 통합 · 원본 보존",false,this::chooseMerge);
  a.gap(a.content,8);a.label(a.content,"기록된 라이딩",20,a.fg,true);
  // Render controls immediately and read track/sidecar metadata on one background worker.
  // A new render invalidates the previous snapshot; no GPS file is modified here.
  LinearLayout historyPanel=a.col();
  historyPanel.addView(a.text("라이딩 기록 확인 중…",13,a.muted,false));
  a.content.addView(historyPanel);
  loadHistoryAsync(historyPanel,a.content);
  a.gap(a.content,12);
  a.label(a.content,"GPS 기록은 이 휴대폰에만 저장됩니다. 주행 중에는 휴대폰을 조작하지 마세요.",13,a.muted,false);
 }
 /** Snapshot all filesystem metadata off the Android UI thread. Stale screen loads are discarded. */
 void loadHistoryAsync(LinearLayout historyPanel,LinearLayout requestedContent){
  final long generation=HISTORY_GENERATION.incrementAndGet();
  final String scope=a.rideHistoryBike, filter=a.rideHistoryFilter, sort=a.rideHistorySort, period=a.rideHistoryPeriod;
  final String activeName=RideService.active;
  final boolean recording=RideService.running;
  final long now=System.currentTimeMillis();
  final File files=a.getFilesDir();
  // Capture garage names on the UI thread; org.json and Activity state are not thread-safe.
  final Map<String,String> bikeNames=new HashMap<>();
  for(org.json.JSONObject bike:a.items("vehicles"))bikeNames.put(bike.optString("id",""),bike.optString("name","이름 없는 바이크"));
  HISTORY_EXECUTOR.execute(()->{
   if(generation!=HISTORY_GENERATION.get())return;
   try{
    List<File> rides=TrackFile.tracks(files);
    List<File> bikeRides=RideHistoryBikeFilter.select(rides,scope);
    int damagedBindings=RideHistoryBikeFilter.damaged(rides);
    String selected=RideHistoryFilter.normalize(filter);
    // Apply the date range BEFORE calculating status-chip counts. Otherwise a
    // 7-day view can claim hundreds of rides even when only two are visible.
    // Imported GPX dates come from their first committed GPS point.
    bikeRides.removeIf(file->!RideHistoryPeriod.include(file,now,period));
    Map<String,Integer> counts=RideHistoryFilter.counts(bikeRides,activeName,recording);
    List<File> visible=RideHistorySort.order(RideHistoryFilter.select(bikeRides,selected,activeName,recording),sort);
    List<HistoryEntry> entries=new ArrayList<>();
    for(File file:visible){
     if(generation!=HISTORY_GENERATION.get())return;
     String category=RideCategory.read(file);
     String label=RideHistoryLabel.title(file,java.time.ZoneId.systemDefault())+
      (RideTrackMerge.mergedSource(file)?" · 통합 원본":"")+
      (RideTrackMerge.archived(file)?" · 통합 취소 보관본":"")+
      (RideCategory.AUTO.equals(category)?"":" · "+category)+
      " · "+RideHistoryBikeLabel.label(file,bikeNames);
     entries.add(new HistoryEntry(file,label,RideHistoryFilter.status(file,activeName,recording)));
    }
    int unfinished=0;
    for(File file:bikeRides)if(RideHistoryFilter.INTERRUPTED.equals(RideHistoryFilter.status(file,activeName,recording)))unfinished++;
    final HistoryData data=new HistoryData(rides,entries,counts,selected,damagedBindings,unfinished);
    a.runOnUiThread(()->{
     if(generation!=HISTORY_GENERATION.get()||a.isFinishing()||a.isDestroyed()||
        !"라이딩".equals(a.tab)||a.content!=requestedContent||historyPanel.getParent()!=requestedContent)return;
     historyPanel.removeAllViews();
     populateHistory(historyPanel,data);
    });
   }catch(Exception failure){
    a.runOnUiThread(()->{
     if(generation!=HISTORY_GENERATION.get()||a.isFinishing()||a.isDestroyed()||
        a.content!=requestedContent||historyPanel.getParent()!=requestedContent)return;
     historyPanel.removeAllViews();
     historyPanel.addView(a.text("라이딩 목록을 읽지 못했어요. 원본 기록은 그대로 보존됩니다.",14,a.muted,false));
     a.gap(historyPanel,8);
     historyPanel.addView(a.button("목록 다시 불러오기",false,()->{
      historyPanel.removeAllViews();
      historyPanel.addView(a.text("라이딩 목록 확인 중…",13,a.muted,false));
      loadHistoryAsync(historyPanel,requestedContent);
     }));
    });
   }
  });
 }
 /** Only Android view creation runs on the UI thread; the file scan has already completed. */
 void populateHistory(LinearLayout historyPanel,HistoryData data){
  List<File> rides=data.rides;
  List<HistoryEntry> entries=data.entries;
  Map<String,Integer> counts=data.counts;
  String selected=data.selected;
  int damagedBindings=data.damaged;
  if(data.unfinished>0){
   Button recover=a.button("중단된 주행 "+data.unfinished+"건 확인 · 복구하기",false,()->a.selectRideHistoryFilter(RideHistoryFilter.INTERRUPTED));
   recover.setContentDescription("중단된 GPS 기록 "+data.unfinished+"건 필터 열기");
   historyPanel.addView(recover);a.gap(historyPanel,10);
  }
   // Horizontally scrollable, labelled status chips avoid squeezing six actions onto a phone screen.
   HorizontalScrollView strip=new HorizontalScrollView(a);
   strip.setHorizontalScrollBarEnabled(false);
   strip.setFillViewport(false);
   LinearLayout chips=new LinearLayout(a);
   chips.setOrientation(LinearLayout.HORIZONTAL);
   chips.setPadding(0,a.dp(4),0,a.dp(6));
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
   historyPanel.addView(strip);
   LinearLayout searchBar=new LinearLayout(a);searchBar.setGravity(Gravity.CENTER_VERTICAL);
   EditText search=new EditText(a);search.setSingleLine(true);search.setTextSize(15);
   search.setTextColor(a.fg);search.setHintTextColor(a.muted);
   search.setHint("날짜·바이크·상태 검색 (예: 10/17 GPX)");
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
   searchBar.addView(clear,clearLp);historyPanel.addView(searchBar);a.gap(historyPanel,8);
   LinearLayout historyTools=new LinearLayout(a);
   historyTools.setGravity(Gravity.CENTER_VERTICAL);
   TextView summary=a.text("표시 "+entries.size()+"건 / 전체 "+rides.size()+"건",12,a.muted,false);
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
   String bikeCaption=RideHistoryBikeFilter.ALL.equals(a.rideHistoryBike)?"전체":
    RideHistoryBikeFilter.UNASSIGNED.equals(a.rideHistoryBike)?"미지정":a.bikeName(a.rideHistoryBike);
   Button bikeButton=a.button("바이크: "+bikeCaption+"  ▾",false,()->{
    List<String> ids=new ArrayList<>(),captions=new ArrayList<>();
    ids.add(RideHistoryBikeFilter.ALL);captions.add("전체 바이크");
    ids.add(RideHistoryBikeFilter.UNASSIGNED);captions.add("바이크 미지정");
    for(org.json.JSONObject bike:a.items("vehicles")){
     String id=bike.optString("id","");
     if(RideBikeBinding.valid(id)&&!ids.contains(id)){
      ids.add(id);captions.add(bike.optString("name","이름 없는 바이크"));
     }
    }
    if(RideBikeBinding.valid(a.rideHistoryBike)&&!ids.contains(a.rideHistoryBike)){
     ids.add(a.rideHistoryBike);captions.add("삭제된 바이크의 기록");
    }
    new AlertDialog.Builder(a).setTitle("라이딩 바이크 선택")
     .setSingleChoiceItems(captions.toArray(new String[0]),Math.max(0,ids.indexOf(a.rideHistoryBike)),(dialog,which)->{
      dialog.dismiss();a.selectRideHistoryBike(ids.get(which));
     }).setNegativeButton("취소",null).show();
   });
   bikeButton.setTextSize(12);
   bikeButton.setMinHeight(a.dp(44));
   bikeButton.setContentDescription("라이딩 바이크 "+bikeCaption+", 변경하기");
   LinearLayout.LayoutParams bikeLp=new LinearLayout.LayoutParams(-2,a.dp(44));bikeLp.leftMargin=a.dp(6);
   historyTools.addView(bikeButton,bikeLp);
   Button periodButton=a.button("기간: "+a.rideHistoryPeriod+"  ▾",false,()->{
    String[] options=RideHistoryPeriod.OPTIONS;
    int checked=java.util.Arrays.asList(options).indexOf(a.rideHistoryPeriod);
    new AlertDialog.Builder(a).setTitle("라이딩 날짜 범위")
     .setSingleChoiceItems(options,Math.max(0,checked),(dialog,which)->{
      dialog.dismiss();a.selectRideHistoryPeriod(options[which]);
     }).setNegativeButton("취소",null).show();
   });
   periodButton.setTextSize(12);
   periodButton.setMinHeight(a.dp(44));
   periodButton.setContentDescription("라이딩 기간 "+a.rideHistoryPeriod+", 변경하기");
   LinearLayout.LayoutParams periodLp=new LinearLayout.LayoutParams(-2,a.dp(44));periodLp.leftMargin=a.dp(6);
   historyTools.addView(periodButton,periodLp);
   // Keep the summary readable on narrow phones; controls scroll horizontally.
   historyPanel.addView(summary);
   if(damagedBindings>0){
    TextView warning=a.text("주의: 바이크 연결 정보 손상 "+damagedBindings+"건 · 미지정으로 분류하지 않음",12,0xffe7a04e,false);
    historyPanel.addView(warning);
   }
   HorizontalScrollView historyControlStrip=new HorizontalScrollView(a);
   historyControlStrip.setHorizontalScrollBarEnabled(false);
   historyControlStrip.addView(historyTools);
   historyPanel.addView(historyControlStrip);
   LinearLayout results=a.col();historyPanel.addView(results);
   search.setText(a.rideHistoryQuery);
   search.setSelection(search.length());
   clear.setEnabled(!a.rideHistoryQuery.isEmpty());
   clear.setAlpha(a.rideHistoryQuery.isEmpty()?0.5f:1f);
   search.setOnEditorActionListener((v,id,event)->{a.hideKeyboard();return true;});
   final Runnable[] pendingSearch={null};
   search.addTextChangedListener(new android.text.TextWatcher(){
    public void beforeTextChanged(CharSequence value,int start,int count,int after){}
    public void onTextChanged(CharSequence value,int start,int before,int count){
     a.rideHistoryQuery=RideHistorySearch.limit(value.toString());
     historyVisibleLimit=RideHistoryPage.PAGE_SIZE;
     clear.setEnabled(!a.rideHistoryQuery.isEmpty());
     clear.setAlpha(a.rideHistoryQuery.isEmpty()?0.5f:1f);
     if(pendingSearch[0]!=null)results.removeCallbacks(pendingSearch[0]);
     pendingSearch[0]=()->{
      if(results.isAttachedToWindow())showHistoryResults(results,entries,rides.size(),selected,summary);
     };
     results.postDelayed(pendingSearch[0],180);
    }
    public void afterTextChanged(android.text.Editable value){}
   });
   showHistoryResults(results,entries,rides.size(),selected,summary);
 }
 /** The CR button previously referenced this missing method, which prevented Android compilation. */
 void chooseMerge(){
  if(a.readOnly||RideService.running||RideService.autoArmed){a.toast("라이딩 기록 또는 자동 감지를 종료한 뒤 통합해 주세요.");return;}
  final long request=MERGE_GENERATION.incrementAndGet();
  a.toast("통합 가능한 라이딩을 확인 중이에요.");
  HISTORY_EXECUTOR.execute(()->{
   try{
    final List<File> candidates=new ArrayList<>();
    for(File file:TrackFile.tracks(a.getFilesDir())){
     if(request!=MERGE_GENERATION.get())return;
     if(candidates.size()>=80)break;
     if(!file.isFile()||file.length()>TrackFile.MAX_BYTES/2||RideTrackMerge.mergedSource(file)||RideTrackMerge.archived(file)||
        new File(file.getPath()+".imported").isFile())continue;
     if(new File(file.getPath()+".done").isFile()||new File(file.getPath()+".recovered").isFile())candidates.add(file);
    }
    final String[] names=new String[candidates.size()];
    for(int i=0;i<names.length;i++)names[i]=RideHistoryLabel.title(candidates.get(i),java.time.ZoneId.systemDefault())+
     (RideTrackMerge.canUndo(a.getFilesDir(),candidates.get(i))?" · 통합본":"");
    a.runOnUiThread(()->{
     if(request!=MERGE_GENERATION.get()||a.isFinishing()||a.isDestroyed()||!"라이딩".equals(a.tab))return;
     if(RideService.running||RideService.autoArmed){a.toast("진행 중인 라이딩을 먼저 종료해 주세요.");return;}
     if(candidates.size()<2){a.toast("통합할 완료 라이딩이 2건 이상 필요해요.");return;}
     showMergeChoice(candidates,names);
    });
   }catch(Exception ex){
    a.runOnUiThread(()->{if(request==MERGE_GENERATION.get()&&!a.isFinishing()&&!a.isDestroyed())
     a.error("라이딩 목록을 불러오지 못했어요: "+ex.getMessage());});
   }
  });
 }
 void showMergeChoice(List<File> candidates,String[] names){
  new AlertDialog.Builder(a).setTitle("첫 번째 라이딩 선택").setItems(names,(dialog,first)->{
   List<File> others=new ArrayList<>();List<String> captions=new ArrayList<>();
   for(int i=0;i<candidates.size();i++)if(i!=first){others.add(candidates.get(i));captions.add(names[i]);}
   new AlertDialog.Builder(a).setTitle("두 번째 라이딩 선택").setItems(captions.toArray(new String[0]),(d,second)->{
    final File left=candidates.get(first),right=others.get(second);
    new AlertDialog.Builder(a).setTitle("라이딩 2건 통합")
     .setMessage("휴식 간격이 30분 이내이고 시간이 겹치지 않는 기록만 통합합니다. 원본 2건은 그대로 보존하며, 통계는 통합본 1건만 계산해요. 필요하면 통합본에서 취소할 수 있습니다.")
     .setNegativeButton("취소",null).setPositiveButton("통합",(confirm,button)->new Thread(()->{
      try{
       if(RideService.running||RideService.autoArmed)throw new IOException("주행 중에는 통합할 수 없습니다.");
       RideTrackMerge.Result result=RideTrackMerge.merge(a.getFilesDir(),left,right);
       a.runOnUiThread(()->{if(!a.isFinishing()&&!a.isDestroyed()){a.render();a.toast("통합 완료 · "+result.points+"개 GPS 좌표 · 원본 보존");}});
      }catch(Exception ex){a.runOnUiThread(()->{if(!a.isFinishing()&&!a.isDestroyed())a.error("라이딩 통합 실패: "+ex.getMessage());});}
     },"moto-ride-merge").start()).show();
   }).setNegativeButton("취소",null).show();
  }).setNegativeButton("취소",null).show();
 }
 void showHistoryResults(LinearLayout results,List<HistoryEntry> entries,int total,String selected,TextView summary){
  results.removeAllViews();
  int count=0;
  final String[] terms=RideHistorySearch.terms(a.rideHistoryQuery);
  // One pass counts all matches and collects only the visible page. This avoids
  // scanning the same 10k-record history again every time the user types.
  final List<HistoryEntry> page=new ArrayList<>(Math.min(entries.size(),Math.min(historyVisibleLimit,100)));
  for(HistoryEntry entry:entries)if(RideHistorySearch.matchesIndex(entry.searchKey,terms)){
   count++;
   if(page.size()<historyVisibleLimit)page.add(entry);
  }
  String bikeLabel=RideHistoryBikeFilter.ALL.equals(a.rideHistoryBike)?"전체":
   RideHistoryBikeFilter.UNASSIGNED.equals(a.rideHistoryBike)?"미지정":a.bikeName(a.rideHistoryBike);
  summary.setText("검색 "+count+"건 / 전체 "+total+"건 · 바이크 "+bikeLabel+" · "+a.rideHistoryPeriod+(count>historyVisibleLimit?" · 처음 "+historyVisibleLimit+"건 표시":""));
  if(count==0){
   String message=total==0?"아직 저장된 주행이 없어요. GPS를 시작하거나 GPX 파일을 가져와 주세요.":
    entries.isEmpty()?"선택한 바이크·상태·기간에 라이딩이 없어요. 필터를 바꿔 주세요.":
    "검색 결과가 없어요. 날짜·시간·상태를 바꾸거나 검색어를 지워 주세요.";
   TextView empty=a.text(message,14,a.muted,false);
   empty.setPadding(a.dp(15),a.dp(16),a.dp(15),a.dp(16));empty.setBackground(a.shape(a.panel,0,14));
   results.addView(empty);
   if(total>0){
    a.gap(results,8);
    Button reset=a.button("전체 기록 보기 · 필터 초기화",false,a::resetRideHistoryControls);
    reset.setMinHeight(a.dp(44));results.addView(reset);
   }
   return;
  }
  final int shown=page.size();
  for(HistoryEntry entry:page){
   File f=entry.file;String date=entry.date,status=entry.status;
   boolean live=RideHistoryFilter.ACTIVE.equals(status);
   boolean interrupted=RideHistoryFilter.INTERRUPTED.equals(status);
   boolean starred=RideFavorite.isFavorite(f);
   String detail=live?"현재 기록 중 · 종료 후 상세 확인":RideHistoryFilter.IMPORTED.equals(status)?"가져온 GPX · 분석과 지도 보기":RideHistoryFilter.COMPLETE.equals(status)?"주행 완료 · 거리·시간·지도":RideHistoryFilter.RECOVERED.equals(status)?"복구된 라이딩 · 거리·시간·지도":"중단된 기록 · 복구하고 분석하기";
    new HubScreens(a).line(results,date,detail,live?"기록 중":interrupted?"복구  ›":starred?"★  자세히  ›":"자세히  ›",()->{
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
    },live||interrupted||a.readOnly?null:()->{
     a.attempt(()->{
      boolean isNowFavorite=RideFavorite.toggle(f);
      a.toast(isNowFavorite?"라이딩 즐겨찾기에 추가했어요.":"즐겨찾기를 해제했어요.");
      showHistoryResults(results,entries,total,selected,summary);
     });
    },starred);
  }
  final int remaining=RideHistoryPage.remaining(count,shown);
  if(remaining>0){
   a.gap(results,8);
   Button more=a.button("라이딩 더 보기 · 남은 "+remaining+"건",false,()->{
    historyVisibleLimit=RideHistoryPage.nextLimit(historyVisibleLimit);
    showHistoryResults(results,entries,total,selected,summary);
   });
   more.setContentDescription("라이딩 기록 "+Math.min(RideHistoryPage.PAGE_SIZE,remaining)+"건 더 보기");
   more.setMinHeight(a.dp(50));
   results.addView(more);
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
 void chooseRideReport(){
  String selectedBike=a.bike()==null?"":a.bike().optString("id","");
  boolean hasBike=RideBikeBinding.valid(selectedBike);
  String[] choices=hasBike?new String[]{"전체 바이크", "현재 바이크 · "+a.bikeName(selectedBike), "바이크 미지정"}:
   new String[]{"전체 바이크", "바이크 미지정"};
  new AlertDialog.Builder(a).setTitle("주행 통계 범위")
   .setItems(choices,(dialog,which)->{
    String scope=which==0?null:(hasBike&&which==1?selectedBike:"");
    rideReport(scope,choices[which]);
   }).setNegativeButton("취소",null).show();
 }
 void rideReport(String bikeScope,String scopeLabel){
  a.toast("기기에 저장된 경로를 분석합니다.");
  new Thread(()->{
   RideMonthlyReport report=new RideMonthlyReport();
   int limited=0;
   try{
    long now=System.currentTimeMillis();
    int inspected=0;
    // Import-time filenames can be much newer than the actual GPX ride date.
    // Sort by the recorded ride time before enforcing the work limit.
    for(File track:RideHistorySort.order(TrackFile.tracks(a.getFilesDir()),RideHistorySort.NEWEST)){
     if(!RideMonthlyReport.recent(track,now,60))continue;
     if(RideService.running&&track.getName().equals(RideService.active))continue;
     if(RideTrackMerge.mergedSource(track)||RideTrackMerge.archived(track))continue;
     try{if(!RideReportScope.include(track,bikeScope))continue;}
     catch(IOException invalidBinding){report.skipped++;continue;}
     if(inspected>=150){limited++;continue;}
     inspected++;
     if(track.length()>5_000_000){report.skipped++;continue;}
     try{RideInsights.Result data=RideInsights.analyze(track);report.add(track,now,data.distanceKm,data.durationMillis);}
     catch(Exception ignored){report.skipped++;}
    }
   }catch(Exception ignored){report.skipped++;}
   final int omitted=limited;
   a.runOnUiThread(()->{
    if(a.isFinishing()||a.isDestroyed())return;
    String text="대상: "+scopeLabel+"\n최근 30일: "+report.current.count+"회 · "+String.format(Locale.KOREA,"%.1f km",report.current.km)
     +" · "+(report.current.minutes/60)+"시간 "+(report.current.minutes%60)+"분"
     +"\n이전 30일: "+report.previous.count+"회 · "+String.format(Locale.KOREA,"%.1f km",report.previous.km)
     +"\n"+RideTrend.compare(report.current.km,report.previous.km)
     +"\n최근   "+RideReportBars.render(report.current.km,Math.max(report.current.km,report.previous.km))
     +"\n이전   "+RideReportBars.render(report.previous.km,Math.max(report.current.km,report.previous.km))
     +"\n손상·크기 초과 등 제외 "+report.skipped+"건"
     +(omitted>0?"\n분석 제한(최신 150건)으로 제외 "+omitted+"건 · 전체 통계 아님":"");
    new AlertDialog.Builder(a).setTitle("내 라이딩 통계").setMessage(text+"\n\nGPS 계산값이며 현재 기록 중인 라이딩은 제외합니다. GPX는 가져온 날짜가 아닌 실제 기록 날짜를 사용합니다.").setPositiveButton("확인",null).show();
   });
  },"moto-local-ride-summary").start();
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
  a.attempt(()->{a.startForegroundService(new Intent(a,RideService.class).putExtra(RideService.EXTRA_BIKE_ID,a.bike()==null?"":a.bike().optString("id")));a.toast("GPS 시작을 요청했어요.");a.root.postDelayed(()->{if(!a.isFinishing())a.render();},800);});
 }
}
