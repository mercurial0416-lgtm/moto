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
  final File file;final String date,status,searchKey,groupKey;
  HistoryEntry(File file,String date,String status,String memo,String groupKey){
   this.file=file;this.date=date;this.status=status;
   this.searchKey=RideHistorySearch.index(date+" "+memo,status);
   this.groupKey=groupKey==null?"":groupKey;
  }
 }
 /** One background reader prevents file-system I/O from freezing the history screen. */
 private static final ExecutorService HISTORY_EXECUTOR=Executors.newSingleThreadExecutor(r->{
  Thread thread=new Thread(r,"moto-history-reader");thread.setDaemon(true);return thread;
 });
 private static final AtomicLong HISTORY_GENERATION=new AtomicLong();
 private static final AtomicLong MERGE_GENERATION=new AtomicLong();
 /** Latest-request-wins: only one 60-day report analyzes GPS at a time. */
 private static final ExecutorService REPORT_EXECUTOR=Executors.newSingleThreadExecutor(r->{
  Thread thread=new Thread(r,"moto-ride-report-reader");thread.setDaemon(true);return thread;
 });
 private static final AtomicLong REPORT_GENERATION=new AtomicLong();
 private int historyVisibleLimit=RideHistoryPage.PAGE_SIZE;
 /** Only view state; never modifies GPS tracks or per-file metadata. */
 private final Set<String> expandedRideSessions=new HashSet<>();
 private static final class HistoryData {
  final List<File> rides;final List<HistoryEntry> entries;
  final Map<String,Integer> counts;final String selected;final int damaged,unfinished;
  final File latestUnapplied;final int unappliedCount;final String distanceInfo;
  HistoryData(List<File> rides,List<HistoryEntry> entries,Map<String,Integer> counts,String selected,int damaged,int unfinished,
              File latestUnapplied,int unappliedCount,String distanceInfo){
   this.distanceInfo=distanceInfo;
   this.rides=rides;this.entries=entries;this.counts=counts;this.selected=selected;this.damaged=damaged;this.unfinished=unfinished;
   this.latestUnapplied=latestUnapplied;this.unappliedCount=unappliedCount;
  }
 }
 final MainActivity a;RideUi(MainActivity a){this.a=a;}
 /** Never say "saved" until the GPS file has a verified completion marker. */
 void finishWithSummary(){
  if(!RideService.running||RideService.saving()){a.toast("GPS 저장 중이거나 기록이 없어요.");return;}
  a.startService(new Intent(a,RideService.class).setAction(RideService.STOP));
  a.toast("GPS 기록 저장·검증 중… 앱에 돌아오면 저장된 주행 결과를 확인할 수 있어요.");
  a.root.postDelayed(()->{if(!a.isFinishing()&&!a.isDestroyed())a.showPendingRideSummary();},1300);
 }
 void render(){
  a.content.setTag("ride-workspace");
  ExperienceUi.title(a,"라이딩","주행에 집중하고, 경로는 기록으로 남기세요.");
  String workspace=a.getSharedPreferences("ride_display",0).getString("workspace","ride");
  ExperienceUi.switcher(a,"주행하기","라이딩 기록",!"history".equals(workspace),()->switchWorkspace("ride"),()->switchWorkspace("history"));
  if("history".equals(workspace)){renderHistoryWorkspace();return;}
  final boolean saving=RideService.saving();
  final boolean focus=RideService.running&&a.getSharedPreferences("ride_display",0).getBoolean("focus",false);
  LinearLayout hero=a.col();hero.setPadding(a.dp(12),a.dp(12),a.dp(12),a.dp(16));
  ExperienceUi.RideConsole console=new ExperienceUi.RideConsole(a,RideService.running);
  hero.addView(console.view,new LinearLayout.LayoutParams(-1,a.dp(250)));
  if(focus)console.view.setVisibility(View.GONE);
  TextView eyebrow=a.text(saving?"GPS 저장 중":RideService.running?"GPS 기록 중":RideService.autoArmed?"자동 출발 감지 중":"기록 대기",13,RideService.running?(a.dark?0xFFFFA79C:0xFFAA3833):a.accent,true);eyebrow.setGravity(Gravity.CENTER);hero.addView(eyebrow);
  TextView title=a.text(saving?"주행 기록 저장 중":RideService.running?"라이딩 진행 중":RideService.autoArmed?"자동 출발 대기":"라이딩 시작하기",22,a.fg,true);title.setTypeface(MotoDesign.display());title.setGravity(Gravity.CENTER);LinearLayout.LayoutParams t=new LinearLayout.LayoutParams(-1,-2);t.topMargin=a.dp(11);hero.addView(title,t);
  TextView sub=a.text(saving?"경로를 확인하고 기기에 저장하고 있어요.":RideService.running?RideService.count+"개 위치 기록 · 화면을 꺼도 GPS 기록 유지":RideService.autoArmed?"OBD2 속도가 감지되면 GPS 기록이 자동 시작돼요":"위치 정보는 휴대폰에만 저장됩니다.",13,a.muted,false);sub.setGravity(Gravity.CENTER);sub.setPadding(0,a.dp(9),0,a.dp(12));hero.addView(sub);
   String initialGps=recordingFeedback();
   TextView gpsHealth=a.text(initialGps,13,RideService.running&&RideService.count==0?a.accent:a.muted,false);
   gpsHealth.setPadding(a.dp(8),a.dp(4),a.dp(8),a.dp(8));
   gpsHealth.setGravity(Gravity.CENTER);
   gpsHealth.setVisibility(initialGps.isEmpty()?View.GONE:View.VISIBLE);
   hero.addView(gpsHealth);
   TextView gpsDiagnostics=a.text("GPS 상태·기록 진단  ›",13,a.accent,true);
   gpsDiagnostics.setGravity(Gravity.CENTER);
   gpsDiagnostics.setMinHeight(a.dp(48));
   gpsDiagnostics.setClickable(true);gpsDiagnostics.setFocusable(true);
   gpsDiagnostics.setOnClickListener(v->showGpsDiagnostics());
   hero.addView(gpsDiagnostics);
  // No redundant pre-ride checklist. Explicit permission/consent handling stays
  // inside requestStart/start; foreground GPS never starts silently.
  if(RideService.running){
   Button focusToggle=a.button(focus?"집중 모드 종료":"주행 집중 모드",false,()->{
    a.getSharedPreferences("ride_display",0).edit().putBoolean("focus",!focus).apply();
    a.render();
   });
   focusToggle.setMinHeight(a.dp(48));hero.addView(focusToggle);a.gap(hero,10);
   LinearLayout live=new LinearLayout(a);
   boolean narrowMetrics=a.getResources().getConfiguration().fontScale>=1.3f||
     a.getResources().getConfiguration().screenWidthDp<360;
   if(narrowMetrics)live.setOrientation(LinearLayout.VERTICAL);
   TextView[] values=new TextView[3];values[2]=console.speed;String[] names={"주행거리","경과시간"};
   for(int i=0;i<2;i++){
    LinearLayout box=a.col();box.setPadding(a.dp(9),a.dp(12),a.dp(9),a.dp(12));box.setBackground(a.shape(a.highlight,0,16));
    values[i]=a.text("—",focus?32:24,a.fg,true);values[i].setGravity(Gravity.CENTER);
    values[i].setAutoSizeTextTypeUniformWithConfiguration(14,focus?32:24,1,android.util.TypedValue.COMPLEX_UNIT_SP);
    box.addView(values[i]);
    TextView name=a.text(names[i],12,a.muted,false);name.setGravity(Gravity.CENTER);
    LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2);np.topMargin=a.dp(7);box.addView(name,np);
    LinearLayout.LayoutParams lp=narrowMetrics?new LinearLayout.LayoutParams(-1,-2):
      new LinearLayout.LayoutParams(0,-2,1);
    if(i>0){if(narrowMetrics)lp.topMargin=a.dp(7);else lp.leftMargin=a.dp(7);}live.addView(box,lp);
   }hero.addView(live);live.setVisibility(focus?android.view.View.GONE:android.view.View.VISIBLE); // Phone navigation is primary; never duplicate a dashboard in focus mode.
   TextView average=a.text("평균속도 계산 중",13,a.muted,false);average.setPadding(a.dp(6),a.dp(10),0,a.dp(8));if(!focus)hero.addView(average);
   TextView peak=a.text("최고속도 계산 중",13,a.muted,false);peak.setPadding(a.dp(6),0,0,a.dp(9));if(!focus)hero.addView(peak);
   TextView motion=a.text("이동·정차 분석 중",13,a.muted,false);motion.setPadding(a.dp(6),0,0,a.dp(9));if(!focus)hero.addView(motion);
   a.gap(hero,12);
   Runnable refresh=new Runnable(){@Override public void run(){
    if(!hero.isAttachedToWindow()||a.isFinishing()||a.isDestroyed())return;
    // When the user switches to external navigation the view can remain attached.
    // Do not refresh hidden gauges or show a stop-confirmation dialog over another app.
    // GPS capture continues in the foreground service, untouched.
    if(!a.hasWindowFocus()){
     hero.postDelayed(this,RideScreenRefreshPolicy.BACKGROUND_UI_POLL_MS);
     return;
    }
    if(!RideService.running){a.renderPreservingPosition();return;}
     String liveStatus=recordingFeedback();
     gpsHealth.setText(liveStatus);
     gpsHealth.setTextColor(RideService.count==0?a.accent:a.muted);
     android.location.Location last=RideService.lastGood;
     boolean freshGps=RideService.gpsFresh();
     sub.setText(RideService.count+"개 위치 기록 · "+(freshGps?"GPS 수신 정상":"GPS 신호 공백")+
      (freshGps&&last!=null&&last.hasAccuracy()?
       String.format(Locale.KOREA," · 정확도 ±%.0fm",last.getAccuracy()):"")+
      (RideService.lastFixElapsed>0?
       " · 마지막 수신 "+Math.max(0,(android.os.SystemClock.elapsedRealtime()-RideService.lastFixElapsed)/1000)+"초 전":" · 첫 수신 대기"));
    long seconds=Math.max(0,(android.os.SystemClock.elapsedRealtime()-RideService.startedElapsed)/1000);
    // Gauge cards are invisible in navigation-first focus mode; GPS is still recorded.
    if(!focus){
    values[0].setText(String.format(Locale.KOREA,"%.1f km",RideService.liveDistanceKm));
    values[1].setText(String.format(Locale.KOREA,"%02d:%02d",seconds/3600,seconds/60%60));
    values[2].setText(GpsSignalStatus.liveSpeedLabel(RideService.liveSpeedKmh,
     RideService.gpsFixElapsed(),android.os.SystemClock.elapsedRealtime()));
    average.setText("평균속도 " + RideMetrics.averageText(RideService.liveDistanceKm,seconds));
    peak.setText(String.format(Locale.KOREA,"최고속도 %.0f km/h · GPS 추정값",RideService.livePeakKmh)+" · "+RideService.status);
    motion.setText(String.format(Locale.KOREA,"이동 %d분 · 정차 %d분 · 이동평균 %.1f km/h (GPS 관측 구간)",
      RideService.liveMovingMillis/60000,RideService.liveStationaryMillis/60000,
      RideTimePolicy.movingAverage(RideService.liveDistanceKm,RideService.liveMovingMillis)));
    }
    // Suggest, never silently stop a manually recorded ride. A fresh GPS fix,
    // real prior travel and uninterrupted stationary time are all mandatory.
    if(RideStopSuggestion.shouldSuggest(
        android.os.SystemClock.elapsedRealtime(),RideService.startedElapsed,
        RideService.lastMotionElapsed,RideService.gpsFresh(),
        RideService.liveDistanceKm,RideService.liveSpeedKmh,RideService.stopSuggestionShown)){
     RideService.stopSuggestionShown=true;
     MotoDialog.confirm(a,"라이딩을 종료할까요?",
       "약 12분 동안 움직임이 확인되지 않았어요. 신호대기나 휴식일 수도 있으므로 기록을 자동 종료하지 않습니다.",
       "저장하고 종료","계속 기록",()->{
        if(RideService.running)a.startService(new Intent(a,RideService.class).setAction(RideService.STOP));
        a.root.postDelayed(()->{if(!a.isFinishing())a.render();},900);
       },null);
    }
    hero.postDelayed(this,RideScreenRefreshPolicy.interval(RideService.gpsFresh(),RideService.liveSpeedKmh,focus));
   }};hero.post(refresh);
  }

  // Primary action always finishes GPS if GPS is live. Never accidentally
  // label OBD-only disarm as the main "ride finish" control.
  Button start=a.button(RideService.running?"라이딩 저장 후 종료":
   RideService.autoArmed?"OBD 자동 감지 중지":"GPS 기록 시작",true,()->{
   if(RideService.running){
    MotoDialog.confirm(a,
      RideService.autoArmed?"자동 라이딩 기록 종료":"라이딩 기록 종료",
      RideService.autoArmed?
       "현재 GPS 기록을 저장하고 OBD2 자동 출발 감지도 함께 종료합니다. 기록은 검증 후 완료됩니다.":
       "GPS 경로를 저장·검증한 뒤 기록을 종료합니다.",
      "저장하고 종료","취소",()->{
       finishWithSummary();
       a.root.postDelayed(()->{if(!a.isFinishing())a.render();},900);
      },null);
    return;
   }
   if(RideService.autoArmed){
    MotoDialog.confirm(a,"OBD2 자동 출발 감지를 중지할까요?",
      "아직 시작하지 않은 GPS 기록은 없습니다. 감지를 중지하고 기존 주행 기록은 보존합니다.",
      "감지 중지","취소",()->{
       a.startService(new Intent(a,RideService.class).setAction(RideService.AUTO_DISARM));
       a.root.postDelayed(()->{if(!a.isFinishing())a.render();},900);
      },null);
    return;
   }
   requestStart();
  });
  start.setTextColor(RideService.running?Color.WHITE:(a.dark?0xFF10203D:Color.WHITE));
  start.setBackground(a.shape(RideService.running?0xffc94b44:a.accent,0,18));
  start.setMinHeight(a.dp(60));start.setEnabled(!saving);start.setAlpha(saving?0.6f:1.0f);
  start.setContentDescription(RideService.running?"현재 GPS 기록을 저장 후 종료":
    RideService.autoArmed?"OBD 자동 출발 감지만 중지":"GPS 기록 시작, 사용자 동의 필요");
  ExperienceUi.dock(a,start);
  if(RideService.autoArmed&&RideService.running){
   a.gap(hero,8);
   Button disarm=a.button("OBD만 중지 · GPS 기록은 계속",false,()->{
    new AlertDialog.Builder(a).setTitle("OBD 자동 감지만 중지할까요?")
     .setMessage("OBD 연결만 끄고 현재 GPS 경로 기록은 계속됩니다. GPS를 끝내려면 '라이딩 저장 후 종료'를 눌러 주세요.")
     .setNegativeButton("취소",null)
     .setPositiveButton("OBD만 중지",(d,w)->{
      a.startService(new Intent(a,RideService.class).setAction(RideService.AUTO_DISARM));
      a.root.postDelayed(()->{if(!a.isFinishing())a.render();},900);
     }).show();
   });
   disarm.setMinHeight(a.dp(50));disarm.setEnabled(!saving);
   hero.addView(disarm);
  }
  a.content.addView(hero);a.gap(a.content,12);
  // The navigation provider is a compact utility row, not a second hero card.
  NavigationChoice chosen=NavigationApps.preferred(a);
  LinearLayout navRow=new LinearLayout(a);navRow.setGravity(Gravity.CENTER_VERTICAL);
  navRow.setPadding(a.dp(13),a.dp(11),a.dp(12),a.dp(11));
  MotoDesign.ripple(a,navRow,a.panel,a.line,15);
  NavIcon navIcon=new NavIcon(a,3,a.accent);
  LinearLayout.LayoutParams nIcon=new LinearLayout.LayoutParams(a.dp(25),a.dp(25));
  nIcon.rightMargin=a.dp(13);navRow.addView(navIcon,nIcon);
  LinearLayout navDescription=a.col();
  navDescription.addView(a.text("내비게이션",15,a.fg,true));
  TextView preferred=a.text(chosen.label+" · 기본 앱 변경  ›",12,a.muted,false);
  LinearLayout.LayoutParams descriptionLp=new LinearLayout.LayoutParams(-1,-2);
  descriptionLp.topMargin=a.dp(5);navDescription.addView(preferred,descriptionLp);
  navDescription.setClickable(true);navDescription.setFocusable(true);
  navDescription.setContentDescription("기본 내비 앱 변경 · "+chosen.label);
  navDescription.setOnClickListener(v->NavigationApps.choose(a));
  navRow.addView(navDescription,new LinearLayout.LayoutParams(0,-2,1));
  Button launch=a.button("열기",false,()->NavigationApps.openPreferred(a));
  launch.setMinHeight(a.dp(44));launch.setContentDescription(chosen.label+" 외부 내비 앱 실행");
  navRow.addView(launch,new LinearLayout.LayoutParams(a.dp(78),-2));
  a.content.addView(navRow);
  if(!focus){
   TextView navHint=a.text("외부 내비를 열어도 GPS 기록은 계속됩니다. 이륜차 통행 제한은 확인하세요.",12,a.muted,false);
   navHint.setPadding(a.dp(5),a.dp(7),a.dp(5),a.dp(3));
   a.content.addView(navHint);
  }
  a.gap(a.content,13);

  // A two-action row replaces the stacked ROUTE & INSIGHTS promotion.
  LinearLayout routesBar=new LinearLayout(a);routesBar.setGravity(Gravity.CENTER_VERTICAL);
  Button routeButton=a.button("내 코스",false,a::savedRoutes);
  Button analysisButton=a.button("라이딩 통계",false,()->RideStatsUi.open(a));
  routesBar.addView(routeButton,new LinearLayout.LayoutParams(0,-2,1));
  LinearLayout.LayoutParams analysisLp=new LinearLayout.LayoutParams(0,-2,1);
  analysisLp.leftMargin=a.dp(9);routesBar.addView(analysisButton,analysisLp);
  a.content.addView(routesBar);
  TextView rideTools=a.text("GPX · 리포트 · 기록 도구  ›",12,a.muted,false);
  rideTools.setGravity(Gravity.CENTER_VERTICAL);rideTools.setMinHeight(a.dp(48));
  rideTools.setClickable(true);rideTools.setFocusable(true);
  rideTools.setContentDescription("GPX 가져오기와 최근 30일 바이크별 주행 통계");
  rideTools.setOnClickListener(v->new AlertDialog.Builder(a)
   .setTitle("라이딩 도구")
   .setItems(new String[]{"GPX 가져오기","최근 30일 리포트","라이딩 2건 통합"},(dialog,which)->{
    if(which==0)a.pickGpx();
    else if(which==1)chooseRideReport();
    else chooseMerge();
   }).setNegativeButton("취소",null).show());
  a.content.addView(rideTools);
  if(focus&&RideService.running)return; // Hide data entry only after nav selection is shown.
  a.gap(a.content,10);
  a.label(a.content,"GPS 기록은 이 기기에 저장됩니다. 주행 중 화면 조작은 피하세요.",12,a.muted,false);
 }
 String recordingFeedback(){
   if(RideService.running){
    if(RideService.count==0){
     long elapsed=android.os.SystemClock.elapsedRealtime()-RideService.startedElapsed;
     return elapsed>=20_000L?
      "GPS 위치 0개 · 첫 신호 미수신. 실외에서 정확한 위치 권한을 확인해 주세요.":
      "GPS 첫 위치 수신 대기 · 아직 저장된 경로 좌표가 없습니다.";
    }
    return RideService.gpsFresh()?
     "GPS 좌표 "+RideService.count+"개 기록됨 · 현재 위치 신호 정상":
     "좌표 "+RideService.count+"개 기록됨 · 현재 GPS 신호가 끊겼거나 지연 중입니다.";
   }
   return RideCaptureHealth.summary(a,false);
  }
  void showGpsDiagnostics(){
   boolean precise=a.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;
   LocationManager manager=a.getSystemService(LocationManager.class);
   boolean enabled=manager!=null&&manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
   boolean notice=Build.VERSION.SDK_INT<33||a.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;
   android.app.NotificationManager nm=a.getSystemService(android.app.NotificationManager.class);
   boolean notifications=nm!=null&&nm.areNotificationsEnabled();
   String result=RideCaptureHealth.summary(a,RideService.running);
   String report="기록 서비스: "+(RideService.running?"실행 중":"중지됨")
     +"\nGPS 좌표: "+RideService.count+"개"
     +"\nGPS 수신: "+(RideService.gpsFresh()?"최근 신호 정상":"최근 유효 신호 없음")
     +"\n정확한 위치 권한: "+(precise?"허용":"미허용")
     +"\n기기 GPS: "+(enabled?"켜짐":"꺼짐")
     +"\n알림: "+(notice&&notifications?"허용":"차단")
     +"\n최근 처리 결과: "+(result.isEmpty()?"아직 기록 시도 없음":result)
     +"\n\n좌표 0개 상태에서는 완료 주행 경로가 생성되지 않습니다. 첫 신호 확인 후 안전한 곳에서 기록을 테스트해 주세요.";
   MotoDialog.info(a,"GPS 기록 진단",report);
  }
  void switchWorkspace(String workspace){a.getSharedPreferences("ride_display",0).edit().putString("workspace",workspace).apply();a.render();}
 void renderHistoryWorkspace(){
  a.content.setTag("ride-history-workspace");
  a.label(a.content,"라이딩 기록",19,a.fg,true);
   String previous=RideCaptureHealth.summary(a,RideService.running);
   if(!previous.isEmpty())a.label(a.content,"최근 GPS 결과 · "+previous,13,a.muted,false);
  LinearLayout historyPanel=a.col();historyPanel.addView(a.text("라이딩 기록 확인 중…",13,a.muted,false));a.content.addView(historyPanel);
  loadHistoryAsync(historyPanel,a.content);
  if(RideService.running)ExperienceUi.dock(a,a.button("현재 라이딩으로 돌아가기",true,()->switchWorkspace("ride")));
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
  // Capture mutable org.json state on UI thread; background scan never touches the live database.
  final org.json.JSONObject selectedBike=a.bike();
  final String currentBike=selectedBike==null?"":selectedBike.optString("id");
  final Set<String> appliedNames=new HashSet<>();
  final org.json.JSONObject ledger=a.settings().optJSONObject("appliedRides");
  if(ledger!=null)for(java.util.Iterator<String> keys=ledger.keys();keys.hasNext();)appliedNames.add(keys.next());
  // Freeze only ride notes, not the whole database, while the Activity still owns JSON mutations.
  final org.json.JSONObject liveNotes=a.settings().optJSONObject("rideNotes");
  final String rideNotesSnapshot=liveNotes==null?"{}":liveNotes.toString();
  final String distanceRange=RideHistoryDistanceFilter.normalize(
   a.getSharedPreferences("ride_history_ui",0).getString("distance_filter",RideHistoryDistanceFilter.ALL));
  final String categoryRange=RideCategoryFilter.normalize(
   a.getSharedPreferences("ride_history_ui",0).getString("category_filter",RideCategoryFilter.ALL));
  HISTORY_EXECUTOR.execute(()->{
   if(generation!=HISTORY_GENERATION.get())return;
   try{
    org.json.JSONObject stableNotes=new org.json.JSONObject(rideNotesSnapshot);
    org.json.JSONObject stableDb=new org.json.JSONObject()
      .put("settings",new org.json.JSONObject().put("rideNotes",stableNotes));
    List<File> rides=TrackFile.tracks(files);
    List<File> bikeRides=RideHistoryBikeFilter.select(rides,scope);
    int damagedBindings=RideHistoryBikeFilter.damaged(rides);
    String selected=RideHistoryFilter.normalize(filter);
    // Apply the date range BEFORE calculating status-chip counts. Otherwise a
    // 7-day view can claim hundreds of rides even when only two are visible.
    // Imported GPX dates come from their first committed GPS point.
    bikeRides=RideHistorySessionPeriod.select(bikeRides,now,period);
    // Recovery warning must not disappear just because an unrelated category was selected.
    int unfinished=0;
    for(File file:bikeRides)
     if(RideHistoryFilter.INTERRUPTED.equals(RideHistoryFilter.status(file,activeName,recording)))unfinished++;
    // Saved category is a separate exact-match filter. Never erase user's text query.
    bikeRides.removeIf(file->!RideCategoryFilter.matches(categoryRange,RideCategory.read(file)));
    Map<String,Integer> counts=RideHistoryFilter.counts(bikeRides,activeName,recording);
    List<File> visible=RideHistorySort.order(RideHistoryFilter.select(bikeRides,selected,activeName,recording),sort);
    String distanceInfo="";
    Map<String,Integer> sessionCounts=RideSegmentGroup.counts(rides);
    // Do not advertise a partly recorded/recovered session as one complete ride.
    // All sibling metadata is checked on the history worker, never the UI thread.
    Set<String> unsafeSessions=new HashSet<>();
    for(File sibling:rides){
     String session=RideSegmentGroup.id(sibling);
     if(session.isEmpty())continue;
     if(!RideHistoryFilter.COMPLETE.equals(RideHistoryFilter.status(sibling,activeName,recording))
        ||!RideMileageIntegrity.validCompleted(sibling)
        ||new File(sibling.getPath()+".imported").exists())unsafeSessions.add(session);
    }
    List<HistoryEntry> entries=new ArrayList<>();
    for(File file:visible){
     if(generation!=HISTORY_GENERATION.get())return;
     String category=RideCategory.read(file);
     String savedTitle=RideNotes.title(stableDb,file);
     String savedMemo=RideNotes.memo(stableDb,file);
     String label=(savedTitle.isEmpty()?"":savedTitle+" · ")+RideHistoryLabel.title(file,java.time.ZoneId.systemDefault())+
      (RideTrackMerge.mergedSource(file)?" · 통합 원본":"")+
      (RideTrackMerge.archived(file)?" · 통합 취소 보관본":"")+
      (RideCategory.AUTO.equals(category)?"":" · "+category)+
      (sessionCounts.getOrDefault(RideSegmentGroup.id(file),0)>1?
       " · 연속 GPS "+sessionCounts.get(RideSegmentGroup.id(file))+"구간":"")+
      " · "+RideHistoryBikeLabel.label(file,bikeNames);
     String status=RideHistoryFilter.status(file,activeName,recording);
     String groupKey="";
     // Background worker only: never inspect sidecars on the UI thread.
     if(RideHistoryFilter.COMPLETE.equals(status)&&RideMileageIntegrity.validCompleted(file)
        &&!new File(file.getPath()+".imported").exists()){
      try{
       String bikeBinding=RideBikeBinding.readStrict(file);
       String session=RideSegmentGroup.id(file);
       if(!session.isEmpty()&&!unsafeSessions.contains(session))
        groupKey=session+"|"+bikeBinding+"|"+category;
      }catch(IOException corrupted){/* Damaged bike binding never silently joins a ride. */}
     }
     String bikeSearch="";
     try{
      String assigned=RideBikeBinding.readStrict(file);
      if(RideBikeBinding.valid(assigned)){
       bikeSearch=assigned;
       for(org.json.JSONObject candidate:a.items("vehicles"))
        if(assigned.equals(candidate.optString("id"))){bikeSearch+=" "+candidate.optString("name","");break;}
      }
     }catch(IOException ignored){}
     entries.add(new HistoryEntry(file,label,status,savedMemo+" "+category+" "+bikeSearch,groupKey));
    }
    if(!RideHistoryDistanceFilter.ALL.equals(distanceRange)){
     // Analyze GPS sections off the UI thread and compare session totals,
     // never mistakenly discard a 60 km ride split into several 20 km files.
     Map<String,Double> measured=new HashMap<>();
     int unreadable=0;
     for(HistoryEntry entry:entries){
      if(generation!=HISTORY_GENERATION.get())return;
      try{measured.put(entry.file.getName(),RideDistanceCache.read(entry.file).distanceKm);}
      catch(Exception invalid){measured.put(entry.file.getName(),Double.NaN);unreadable++;}
     }
     entries=RideHistorySessionDistance.select(entries,entry->entry.groupKey,
      entry->measured.getOrDefault(entry.file.getName(),Double.NaN),
      km->RideHistoryDistanceFilter.accepts(distanceRange,km));
     distanceInfo="GPS 거리 "+distanceRange+
      " · 여러 구간의 한 라이딩은 합산 기준 · 손상·미완료 "+unreadable+
      "개 제외 · 계기판 거리와 다를 수 있어요.";
    }
    File unapplied=null;int unappliedCount=0;
    if(!currentBike.isEmpty())for(File file:rides){
     if(generation!=HISTORY_GENERATION.get())return;
     // Imported GPX files are not automatic mileage suggestions.
     if(new File(file.getPath()+".imported").isFile())continue;
     if(RideMileageIntegrity.eligibleForReview(file,appliedNames,currentBike)){
      unappliedCount++;
      if(unapplied==null)unapplied=file;
     }
    }
    final HistoryData data=new HistoryData(rides,entries,counts,selected,damagedBindings,unfinished,unapplied,unappliedCount,distanceInfo);
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
  if(data.latestUnapplied!=null&&data.unappliedCount>0){
   LinearLayout review=a.col();review.setPadding(a.dp(14),a.dp(12),a.dp(14),a.dp(12));
   review.setBackground(a.shape(a.panel,a.line,17));
   review.addView(a.text("주행거리 확인 대기 · "+data.unappliedCount+"건",15,a.fg,true));
   review.addView(a.text("완료된 GPS 기록을 검토한 뒤 원하는 경우에만 계기판 누적거리에 반영해요. 중복 반영은 막습니다.",12,a.muted,false));
   a.gap(review,7);
   review.addView(a.button("최근 미반영 라이딩 확인",false,()->a.openRideInsights(data.latestUnapplied)));
   historyPanel.addView(review);
   a.gap(historyPanel,14);
  }
  List<File> rides=data.rides;
  List<HistoryEntry> entries=data.entries;
  Map<String,Integer> counts=data.counts;
  String selected=data.selected;
  int damagedBindings=data.damaged;
  if(!data.distanceInfo.isEmpty()){
   TextView info=a.text(data.distanceInfo,12,a.muted,false);
   historyPanel.addView(info);a.gap(historyPanel,7);
  }
  if(data.unfinished>0){
   Button recover=a.button("중단된 주행 "+data.unfinished+"건 확인 · 복구하기",false,()->a.selectRideHistoryFilter(RideHistoryFilter.INTERRUPTED));
   recover.setContentDescription("중단된 GPS 기록 "+data.unfinished+"건 필터 열기");
   historyPanel.addView(recover);a.gap(historyPanel,10);
  }
   // One state selector replaces six always-visible horizontal chips.
   String[] statusOptions=RideHistoryFilter.OPTIONS;
   Button statusButton=a.button("상태 · "+selected+"  (GPS 구간 "+counts.getOrDefault(selected,0)+"개)  ▾",false,()->
    new AlertDialog.Builder(a).setTitle("라이딩 상태")
     .setSingleChoiceItems(statusOptions,Math.max(0,Arrays.asList(statusOptions).indexOf(selected)),
      (dialog,index)->{dialog.dismiss();a.selectRideHistoryFilter(statusOptions[index]);})
     .setNegativeButton("취소",null).show());
   statusButton.setContentDescription("라이딩 상태 필터 "+selected+", 변경하기");
   statusButton.setMinHeight(a.dp(48));
   historyPanel.addView(statusButton);a.gap(historyPanel,8);
   LinearLayout searchBar=new LinearLayout(a);searchBar.setGravity(Gravity.CENTER_VERTICAL);
   EditText search=new EditText(a);search.setSingleLine(true);search.setTextSize(15);
   search.setTextColor(a.fg);search.setHintTextColor(a.muted);
   search.setHint("날짜·바이크·상태 검색 (예: 와인딩)");
   search.setContentDescription("라이딩 기록 검색");
   search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
   search.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
   search.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(RideHistorySearch.MAX_LENGTH)});
   search.setPadding(a.dp(14),a.dp(10),a.dp(14),a.dp(10));
   search.setBackground(a.shape(a.panel,a.line,17));
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
   final String selectedCategory=RideCategoryFilter.normalize(a.getSharedPreferences("ride_history_ui",0)
     .getString("category_filter",RideCategoryFilter.ALL));
   Button categoryButton=a.button("종류: "+selectedCategory+"  ▾",false,()->
    new AlertDialog.Builder(a).setTitle("저장한 라이딩 종류")
     .setMessage("저장된 종류만 정확하게 필터링합니다. 제목·메모 검색어는 그대로 유지되고 자동 분류는 추측하지 않습니다.")
     .setSingleChoiceItems(RideCategoryFilter.OPTIONS,
      Math.max(0,Arrays.asList(RideCategoryFilter.OPTIONS).indexOf(selectedCategory)),(dialog,index)->{
       dialog.dismiss();
       a.getSharedPreferences("ride_history_ui",0).edit()
        .putString("category_filter",RideCategoryFilter.OPTIONS[index]).apply();
       a.render();
      }).setNegativeButton("취소",null).show());
   categoryButton.setMinHeight(a.dp(44));
   LinearLayout.LayoutParams categoryLp=new LinearLayout.LayoutParams(-2,a.dp(44));categoryLp.leftMargin=a.dp(6);
   historyTools.addView(categoryButton,categoryLp);
   final String distancePref=RideHistoryDistanceFilter.normalize(a.getSharedPreferences("ride_history_ui",0)
     .getString("distance_filter",RideHistoryDistanceFilter.ALL));
   Button distanceButton=a.button("거리: "+distancePref+"  ▾",false,()->
    new AlertDialog.Builder(a).setTitle("GPS 주행거리 범위")
     .setMessage("완료된 GPS 기록의 실제 저장 좌표로만 계산해요. 많은 기록은 성능 보호를 위해 거리 필터를 적용하지 않고 안내합니다.")
     .setSingleChoiceItems(RideHistoryDistanceFilter.OPTIONS,
      Math.max(0,Arrays.asList(RideHistoryDistanceFilter.OPTIONS).indexOf(distancePref)),(dialog,index)->{
       dialog.dismiss();
       a.getSharedPreferences("ride_history_ui",0).edit().putString("distance_filter",
        RideHistoryDistanceFilter.OPTIONS[index]).apply();
       a.render();
      }).setNegativeButton("취소",null).show());
   distanceButton.setMinHeight(a.dp(44));
   LinearLayout.LayoutParams distanceLp=new LinearLayout.LayoutParams(-2,a.dp(44));
   distanceLp.leftMargin=a.dp(6);historyTools.addView(distanceButton,distanceLp);
   // Show only search and status by default, but keep the user's expanded state on refresh.
   historyPanel.addView(summary);
   final android.content.SharedPreferences historyUiPrefs=a.getSharedPreferences("ride_history_ui",0);
   final boolean wasExpanded=historyUiPrefs.getBoolean("advanced_expanded",false);
   final int applied=(RideHistoryPeriod.ALL.equals(a.rideHistoryPeriod)?0:1)
    +(RideHistoryBikeFilter.ALL.equals(a.rideHistoryBike)?0:1)
    +(RideHistorySort.NEWEST.equals(a.rideHistorySort)?0:1)
    +(RideCategoryFilter.ALL.equals(selectedCategory)?0:1)
    +(RideHistoryDistanceFilter.ALL.equals(distancePref)?0:1);
   final String summaryFilters="상세 필터"+(applied==0?"":" · "+applied+"개 적용");
   HorizontalScrollView historyControlStrip=new HorizontalScrollView(a);
   Button filterToggle=a.button(summaryFilters+(wasExpanded?"  ▴":"  ▾"),false,()->{});
   filterToggle.setOnClickListener(view->{
    boolean expand=historyControlStrip.getVisibility()!=View.VISIBLE;
    historyControlStrip.setVisibility(expand?View.VISIBLE:View.GONE);
    historyUiPrefs.edit().putBoolean("advanced_expanded",expand).apply();
    filterToggle.setText(summaryFilters+(expand?"  ▴":"  ▾"));
   });
   filterToggle.setContentDescription("날짜, 바이크, 종류, GPS 거리, 정렬 상세 필터 "+applied+"개 적용. 열기 또는 접기");
   historyPanel.addView(filterToggle);
   if(damagedBindings>0){
    TextView warning=a.text("주의: 바이크 연결 정보 손상 "+damagedBindings+"건 · 미지정으로 분류하지 않음",12,0xffe7a04e,false);
    historyPanel.addView(warning);
   }
   historyControlStrip.setHorizontalScrollBarEnabled(false);
   historyControlStrip.addView(historyTools);
   historyControlStrip.setVisibility(wasExpanded?View.VISIBLE:View.GONE);
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
  final Set<String> alreadyApplied=new HashSet<>();
  org.json.JSONObject appliedLedger=a.settings().optJSONObject("appliedRides");
  if(appliedLedger!=null)for(java.util.Iterator<String> keys=appliedLedger.keys();keys.hasNext();)alreadyApplied.add(keys.next());
  a.toast("통합 가능한 라이딩을 확인 중이에요.");
  HISTORY_EXECUTOR.execute(()->{
   try{
    final List<File> candidates=new ArrayList<>();
    for(File file:TrackFile.tracks(a.getFilesDir())){
     if(request!=MERGE_GENERATION.get())return;
     if(candidates.size()>=80)break;
     if(!file.isFile()||alreadyApplied.contains(file.getName())||file.length()>TrackFile.MAX_BYTES/2||RideTrackMerge.mergedSource(file)||RideTrackMerge.archived(file)||
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
    org.json.JSONObject latestApplied=a.settings().optJSONObject("appliedRides");
    if(latestApplied!=null&&(latestApplied.has(left.getName())||latestApplied.has(right.getName()))){
     a.toast("이미 계기판 누적거리에 반영한 라이딩은 통합할 수 없어요.");return;
    }
    new AlertDialog.Builder(a).setTitle("라이딩 2건 통합")
     .setMessage("휴식 간격이 30분 이내이고 시간이 겹치지 않는 기록만 통합합니다. 원본 2건은 그대로 보존하며, 통계는 통합본 1건만 계산해요. 필요하면 통합본에서 취소할 수 있습니다.")
     .setNegativeButton("취소",null).setPositiveButton("통합",(confirm,button)->new Thread(()->{
      try{
       if(RideService.running||RideService.autoArmed)throw new IOException("주행 중에는 통합할 수 없습니다.");
       RideTrackMerge.Result result=RideTrackMerge.merge(a.getFilesDir(),left,right);
       a.runOnUiThread(()->{if(!a.isFinishing()&&!a.isDestroyed()){a.render();a.rebuildGpsEstimateAsync();a.toast("통합 완료 · "+result.points+"개 GPS 좌표 · 원본 보존");}});
      }catch(Exception ex){a.runOnUiThread(()->{if(!a.isFinishing()&&!a.isDestroyed())a.error("라이딩 통합 실패: "+ex.getMessage());});}
     },"moto-ride-merge").start()).show();
   }).setNegativeButton("취소",null).show();
  }).setNegativeButton("취소",null).show();
 }
 /** Recovery mutates the original GPS file: reserve it against exports, restores,
  * form writes and new/live rides, and honor the existing protection state. */
 void recoverInterrupted(File f){
  if(a.readOnly){a.error("원본 보호 상태에서는 GPS 기록을 변경할 수 없어요.");return;}
  new Thread(()->{
   try{
    if(!RestoreRideGate.begin(RideService.running||RideService.saving(),RideService.autoArmed))
     throw new IOException("라이딩·저장·백업·복원 중에는 GPS를 복구할 수 없어요.");
    TrackRecovery.Result result;
    try{
     if(a.readOnly)throw new IOException("원본 보호 상태에서는 GPS 기록을 변경할 수 없어요.");
     if(f==null||!TrackFile.directory(a.getFilesDir()).getCanonicalFile().equals(f.getCanonicalFile().getParentFile()))
      throw new IOException("앱에 저장된 GPS 원본만 복구할 수 있어요.");
     result=TrackRecovery.recover(f);
    }finally{RestoreRideGate.end();}
    a.runOnUiThread(()->{
     if(a.isFinishing()||a.isDestroyed())return;
     a.toast("복구 완료 · "+result.points+"개 좌표 · 제거 "+result.discardedBytes+"바이트");
     a.render();a.rebuildGpsEstimateAsync();a.openRideInsights(f);
    });
   }catch(Exception e){a.runOnUiThread(()->{if(!a.isFinishing()&&!a.isDestroyed())a.error("GPS 복구 실패: "+e.getMessage());});}
  },"MOTO-ride-recovery").start();
 }
 void showHistoryResults(LinearLayout results,List<HistoryEntry> entries,int total,String selected,TextView summary){
  results.removeAllViews();
  final String[] terms=RideHistorySearch.terms(a.rideHistoryQuery);
  final List<HistoryEntry> matched=new ArrayList<>();
  for(HistoryEntry entry:entries)if(RideHistorySearch.matchesIndex(entry.searchKey,terms))matched.add(entry);
  final List<RideHistorySessionRows.Row<HistoryEntry>> rows=
   RideHistorySessionRows.group(matched,entry->entry.groupKey);
  final int count=rows.size();
  final int shown=Math.min(count,historyVisibleLimit);
  final String bikeLabel=RideHistoryBikeFilter.ALL.equals(a.rideHistoryBike)?"전체":
   RideHistoryBikeFilter.UNASSIGNED.equals(a.rideHistoryBike)?"미지정":a.bikeName(a.rideHistoryBike);
  summary.setText("검색 라이딩 "+count+"회 · GPS 구간 "+matched.size()+"개 / 전체 파일 "+total+"개 · 바이크 "+bikeLabel+
   " · "+a.rideHistoryPeriod+(count>historyVisibleLimit?" · 처음 "+historyVisibleLimit+"회 표시":""));
  if(count==0){
   String message=total==0?"아직 저장된 주행이 없어요. GPS를 시작하거나 GPX 파일을 가져와 주세요.":
    entries.isEmpty()?"선택한 바이크·상태·기간에 라이딩이 없어요. 필터를 바꿔 주세요.":
    "검색 결과가 없어요. 날짜·시간·상태를 바꾸거나 검색어를 지워 주세요.";
   TextView empty=a.text(message,14,a.muted,false);
   empty.setPadding(a.dp(15),a.dp(16),a.dp(15),a.dp(16));empty.setBackground(a.shape(a.panel,a.line,17));
   results.addView(empty);
   if(total>0){
    a.gap(results,8);
    Button reset=a.button("전체 기록 보기 · 필터 초기화",false,a::resetRideHistoryControls);
    reset.setMinHeight(a.dp(44));results.addView(reset);
   }
   return;
  }
  for(int i=0;i<shown;i++){
   RideHistorySessionRows.Row<HistoryEntry> row=rows.get(i);
   if(!row.grouped()){
    showHistoryEntry(results,row.segments.get(0),entries,total,selected,summary);
    continue;
   }
   // A grouped ride's display time is its FIRST GPS section, never
   // whichever end section happens to sort to the top.
   HistoryEntry earliest=row.segments.get(0);
   Long firstTime=null;
   for(HistoryEntry part:row.segments){
    Long timestamp=RideHistoryLabel.filenameTime(part.file.getName());
    if(timestamp!=null&&(firstTime==null||timestamp<firstTime)){
     firstTime=timestamp;earliest=part;
    }
   }
   final boolean expanded=expandedRideSessions.contains(row.key);
   new HubScreens(a).line(results,
    earliest.date+" · 라이딩 1회",
    "GPS "+row.segments.size()+"개 구간 · 원본 그대로 보존",
    expanded?"구간 접기  ›":"구간 펼치기  ›",
    ()->{
     if(!expandedRideSessions.add(row.key))expandedRideSessions.remove(row.key);
     showHistoryResults(results,entries,total,selected,summary);
    },null,false);
   if(expanded)for(HistoryEntry segment:row.segments)
    showHistoryEntry(results,segment,entries,total,selected,summary);
  }
  final int remaining=RideHistoryPage.remaining(count,shown);
  if(remaining>0){
   a.gap(results,8);
   Button more=a.button("라이딩 더 보기 · 남은 "+remaining+"회",false,()->{
    historyVisibleLimit=RideHistoryPage.nextLimit(historyVisibleLimit);
    showHistoryResults(results,entries,total,selected,summary);
   });
   more.setContentDescription("라이딩 기록 "+Math.min(RideHistoryPage.PAGE_SIZE,remaining)+"회 더 보기");
   more.setMinHeight(a.dp(50));
   results.addView(more);
  }
 }
 private void showHistoryEntry(LinearLayout results,HistoryEntry entry,
                               List<HistoryEntry> entries,int total,String selected,TextView summary){

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
       .setPositiveButton("복구",(d,w)->recoverInterrupted(f)).show();
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
  final android.app.AlertDialog progress=new android.app.AlertDialog.Builder(a)
   .setTitle("최근 60일 GPS 분석")
   .setMessage("분석할 기록을 확인 중…")
   .setNegativeButton("취소",(dialog,which)->REPORT_GENERATION.incrementAndGet())
   .create();
  progress.setOnCancelListener(dialog->REPORT_GENERATION.incrementAndGet());
  progress.show();
  progress.setCanceledOnTouchOutside(false);
  final long request=REPORT_GENERATION.incrementAndGet();
  REPORT_EXECUTOR.execute(()->{
   if(request!=REPORT_GENERATION.get())return;
   RideMonthlyReport report=new RideMonthlyReport();
   RideTypeComparison categoryComparison=new RideTypeComparison();
   int scanned=0;
   try{
    long now=System.currentTimeMillis();
    // Imported GPX uses actual first point time, not the date it was imported.
    // Include EVERY eligible record over the 60-day window, on a worker.
    for(File track:RideHistorySort.order(TrackFile.tracks(a.getFilesDir()),RideHistorySort.NEWEST)){
     if(request!=REPORT_GENERATION.get()||a.isFinishing()||a.isDestroyed())return;
     if(!RideMonthlyReport.recent(track,now,60))continue;
     if(RideActiveSessionPolicy.exclude(track.getName(),RideService.running,RideService.active))continue;
     if(RideTrackMerge.mergedSource(track)||RideTrackMerge.archived(track))continue;
     try{if(!RideReportScope.include(track,bikeScope))continue;}
     catch(IOException invalidBinding){report.skipped++;continue;}
     if(a.isFinishing()||a.isDestroyed())return;
     scanned++;
     if(scanned%25==0){
      final int examined=scanned,excluded=report.skipped;
      a.runOnUiThread(()->{
       if(request==REPORT_GENERATION.get()&&!a.isFinishing()&&!a.isDestroyed()&&progress.isShowing())
        progress.setMessage("GPS "+examined+"건 확인 · 손상/제외 "+excluded+"건 · 취소 가능");
      });
     }
     try{
      RideDistanceCache.Summary data=RideDistanceCache.read(track);
      report.add(track,now,data.distanceKm,data.durationMillis);
      categoryComparison.add(track,now,data);
     }catch(Exception ignored){report.skipped++;}
    }
   }catch(Exception ignored){report.skipped++;}
   final int analyzedCandidates=scanned;
   a.runOnUiThread(()->{
    if(request!=REPORT_GENERATION.get()||a.isFinishing()||a.isDestroyed()||!"라이딩".equals(a.tab)||!progress.isShowing())return;
    progress.dismiss();
    String text="대상: "+scopeLabel+"\n최근 30일: "+report.current.count+"회 · "+String.format(Locale.KOREA,"%.1f km",report.current.km)
     +" · "+(report.current.minutes/60)+"시간 "+(report.current.minutes%60)+"분"
     +"\n이전 30일: "+report.previous.count+"회 · "+String.format(Locale.KOREA,"%.1f km",report.previous.km)
     +"\n"+RideTrend.compare(report.current.km,report.previous.km)
     +"\n최근   "+RideReportBars.render(report.current.km,Math.max(report.current.km,report.previous.km))
     +"\n이전   "+RideReportBars.render(report.previous.km,Math.max(report.current.km,report.previous.km))
     +"\n"+categoryComparison.report()+
     "\n최근 60일 대상 "+analyzedCandidates+"건 검사 · 손상·미완료·크기 초과 "+report.skipped+"건 제외";
    new AlertDialog.Builder(a).setTitle("내 라이딩 통계").setMessage(text+"\n\nGPS 계산값이며 현재 기록 중인 라이딩은 제외합니다. GPX는 가져온 날짜가 아닌 실제 기록 날짜를 사용합니다.").setPositiveButton("확인",null).show();
   });
  });
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
 void requestStart(){requestStart(false);}
 void requestStart(boolean openNavigatorAfterStart){
  a.suppressUpdatePromptUntilElapsed=android.os.SystemClock.elapsedRealtime()+120_000L;
  RideReadiness.State state=readiness();
  if(state==RideReadiness.State.GPS_DISABLED||state==RideReadiness.State.NOTIFICATIONS_DISABLED){handleReadiness(state);return;}
  final org.json.JSONObject selectedBike=a.bike();
  final String approvedBike=selectedBike==null?"":selectedBike.optString("id");
  final String displayBike=selectedBike==null?"바이크 미지정 (차고에서 선택 가능)":selectedBike.optString("name","바이크 이름 없음");
  MotoDialog.rideStart(a,displayBike,openNavigatorAfterStart,()->{
     String liveBike=a.bike()==null?"":a.bike().optString("id");
     if(!approvedBike.equals(liveBike)){
      a.rideStartConsent.cancel();a.approvedRideBikeId="";a.launchNavigationAfterRide=false;
      a.toast("바이크 선택이 변경됐어요. 다시 확인해 주세요.");return;
     }
     a.approvedRideBikeId=approvedBike;
     a.launchNavigationAfterRide=openNavigatorAfterStart;a.rideStartConsent.approve();start();
    },()->{
     a.rideStartConsent.cancel();
     a.launchNavigationAfterRide=false;a.approvedRideBikeId="";
    });
 }
 void start(){
  if(!a.rideStartConsent.pending()||!"라이딩".equals(a.tab))return;
  RideReadiness.State state=readiness();
  if(state==RideReadiness.State.GPS_DISABLED||state==RideReadiness.State.NOTIFICATIONS_DISABLED){a.rideStartConsent.cancel();handleReadiness(state);return;}
  if(a.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){a.requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,android.Manifest.permission.ACCESS_COARSE_LOCATION},70);return;}
  if(Build.VERSION.SDK_INT>=33&&a.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){a.requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},71);return;}
  if(readiness()!=RideReadiness.State.READY){a.rideStartConsent.cancel();a.toast("GPS 준비 상태가 변경됐어요. 확인 후 다시 시작해 주세요.");return;}
  String liveBike=a.bike()==null?"":a.bike().optString("id");
  if(!a.approvedRideBikeId.equals(liveBike)){
   a.rideStartConsent.cancel();a.launchNavigationAfterRide=false;
   a.toast("바이크가 변경돼 GPS 시작을 취소했어요. 다시 확인해 주세요.");return;
  }
  // A fresh storage check precedes consent consumption; service repeats the check at file creation.
  try{GpsStorageSpacePolicy.require(a.getFilesDir());}
  catch(IOException storage){a.rideStartConsent.cancel();a.approvedRideBikeId="";a.launchNavigationAfterRide=false;
   a.toast(storage.getMessage());return;}
  if(!a.rideStartConsent.consume("라이딩".equals(a.tab),true))return;
  final String fixedBikeId=a.approvedRideBikeId;
  a.approvedRideBikeId="";
  a.attempt(()->{
   RideCaptureHealth.started(a);
   try{
    a.startForegroundService(new Intent(a,RideService.class).putExtra(RideService.EXTRA_BIKE_ID,fixedBikeId));
   }catch(RuntimeException startFailure){
    RideCaptureHealth.failedIfActive(a,startFailure.getMessage());
    throw startFailure;
   }
   boolean nav=a.launchNavigationAfterRide;a.launchNavigationAfterRide=false;
   a.toast("GPS 기록을 시작하는 중…");
   if(!nav){
    final android.os.Handler statusCheck=new android.os.Handler(android.os.Looper.getMainLooper());
    final long started=android.os.SystemClock.elapsedRealtime();
    statusCheck.postDelayed(new Runnable(){@Override public void run(){
     if(a.isFinishing()||a.isDestroyed())return;
     if(RideService.running){
      a.toast(RideService.gpsFresh()?"GPS 기록 시작됨 · 위치 수신 정상":
       "GPS 기록 시작됨 · 첫 위치 수신 대기");
      if(a.hasWindowFocus()&&"라이딩".equals(a.tab))a.renderPreservingPosition();
      return;
     }
     if(android.os.SystemClock.elapsedRealtime()-started<12_000L)
      statusCheck.postDelayed(this,650);
     else {
      a.toast("GPS 기록 시작을 확인하지 못했어요. 라이딩 상태를 확인해 주세요.");
      if(a.hasWindowFocus()&&"라이딩".equals(a.tab))a.renderPreservingPosition();
     }
    }},700);
   }
   a.root.postDelayed(()->{if(!a.isFinishing()&&!a.isDestroyed()&&a.hasWindowFocus())a.render();},800);
   if(nav){
    final android.os.Handler starter=new android.os.Handler(android.os.Looper.getMainLooper());
    final long deadline=android.os.SystemClock.elapsedRealtime()+15_000L;
    starter.postDelayed(new Runnable(){@Override public void run(){
     if(a.isFinishing()||a.isDestroyed())return;
     if(RideService.running){
      a.toast("GPS 기록 시작됨 · 선택한 내비를 엽니다.");
      NavigationApps.openPreferred(a);
      return;
     }
     if(android.os.SystemClock.elapsedRealtime()<deadline)
      starter.postDelayed(this,600);
     else a.toast("GPS 기록을 시작하지 못해 내비를 자동으로 열지 않았어요.");
    }},600);
   }
  });
 }
}
