package com.motohub.next;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.view.View;
import android.view.WindowManager;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import java.util.Locale;

/** Experimental, local-only telemetry view. No interactive riding controls. */
public final class ObdDashboardActivity extends Activity {
 private final Handler handler=new Handler(Looper.getMainLooper());
 private TextView speed,rpm,temperature,voltage,ride,status,advisory,linkStatus;
 private boolean compactMode;
 private ProgressBar rpmMeter;
 private boolean visible;
 private static final String PREFS="obd_dashboard_preferences";
 private final Runnable update=new Runnable(){@Override public void run(){
  if(!visible)return;
  if(!getSharedPreferences("developer_experiments",0).getBoolean("enabled",false)){finish();return;}
  long now=android.os.SystemClock.elapsedRealtime();
  boolean speedFresh=RideService.autoArmed&&ObdTelemetryFreshness.speedFresh(RideService.obdLastSampleElapsed,now);
  speed.setText(speedFresh&&RideService.obdSpeedKmh>=0?RideService.obdSpeedKmh+"":"—");
  speed.setTextColor(speedFresh?Color.WHITE:0xffffc78d);
  speed.setContentDescription(speedFresh?"차량 속도 "+RideService.obdSpeedKmh+"킬로미터 매시":"OBD2 속도 지연, 마지막 값 숨김");
  linkStatus.setText(ObdTelemetryFreshness.connectionLabel(RideService.autoArmed,RideService.running,speedFresh)+" · "+RideService.obdLink);
  boolean rpmFresh=RideService.autoArmed&&ObdTelemetryFreshness.fresh(RideService.obdRpmAt,now);
  rpm.setText(rpmFresh?format(RideService.obdRpm,"%.0f"):"—");
  rpmMeter.setProgress(rpmFresh?ObdGaugeWarnings.rpmBar(RideService.obdRpm):0);
  temperature.setText(RideService.autoArmed&&ObdTelemetryFreshness.fresh(RideService.obdCoolantAt,now)?format(RideService.obdCoolantC,"%.0f °C"):"—");
  boolean tempFresh=RideService.autoArmed&&ObdTelemetryFreshness.fresh(RideService.obdCoolantAt,now);
  boolean voltFresh=RideService.autoArmed&&ObdTelemetryFreshness.fresh(RideService.obdVoltageAt,now);
  voltage.setText(voltFresh?format(RideService.obdVoltage,"%.1f V"):"—");
  advisory.setText(ObdGaugeWarnings.advisory(
   rpmFresh?RideService.obdRpm:Double.NaN,
   tempFresh?RideService.obdCoolantC:Double.NaN,
   voltFresh?RideService.obdVoltage:Double.NaN));
  ride.setText(RideService.running?(RideService.gpsFresh()?
    String.format(Locale.KOREA,"GPS %.2f km · 최고 %.0f km/h",RideService.liveDistanceKm,RideService.livePeakKmh):
    "GPS 신호 대기 · 기존 기록 보존"):
    RideService.autoArmed?"자동 기록 대기 · GPS 기록 시작 전":"라이딩 미작동");
  status.setText("OBD2 · "+RideService.obdLink+" · 재연결 "+RideService.obdReconnects+"회\n"+
   ObdTelemetryFreshness.connectionLabel(RideService.autoArmed,RideService.running,speedFresh)+
   "\n"+RideService.engineEstimate()+"\n지원 PID: "+RideService.obdPidSupport+
   "\n"+ObdRestStatus.label(RideService.autoArmed,RideService.running,RideService.obdStationaryRemainingMs)+
   "\n보조 센서 진단: "+RideService.obdAuxDiagnostics+"\n최근 OBD 오류: "+RideService.obdLastError);
  handler.postDelayed(this,RideService.autoArmed&&speedFresh?1300:3000);
 }};
 static String format(double x,String fmt){return Double.isFinite(x)?String.format(Locale.KOREA,fmt,x):"—";}
 private LinearLayout base;
 private TextView value(String caption,int size){
  TextView label=new TextView(this);label.setText(caption);label.setTextSize(14);label.setTextColor(0xffb8c5d3);label.setGravity(Gravity.CENTER);base.addView(label);
  TextView v=new TextView(this);v.setText("—");v.setTextSize(compactMode?Math.min(size,caption.startsWith("차량 속도")?54:24):size);v.setTypeface(null,android.graphics.Typeface.BOLD);v.setTextColor(Color.WHITE);v.setGravity(Gravity.CENTER);
  v.setPadding(0,compactMode?2:7,0,compactMode?10:24);base.addView(v);return v;
 }
 @Override public void onCreate(Bundle state){super.onCreate(state);
  if(!getSharedPreferences("developer_experiments",0).getBoolean("enabled",false)){finish();return;}
  compactMode=getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;
  ScrollView scroll=new ScrollView(this);
  scroll.setFillViewport(true);
  base=new LinearLayout(this);base.setBackgroundColor(0xff0c131a);
  int densityPad=(int)(getResources().getDisplayMetrics().density*(compactMode?10:18));
  base.setPadding(densityPad,densityPad,densityPad,densityPad);
  base.setOrientation(LinearLayout.VERTICAL);
  scroll.addView(base);
  setContentView(scroll);
  TextView title=new TextView(this);title.setText("MOTO HUB · DEV DASH");title.setTextColor(0xffff8641);title.setTextSize(20);title.setGravity(Gravity.CENTER);title.setPadding(0,0,0,18);base.addView(title);
  speed=value("차량 속도  ·  km/h",60);
  rpm=value("엔진 회전수  ·  RPM",29);
  rpmMeter=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
  rpmMeter.setMax(15000);rpmMeter.setProgress(0);
  rpmMeter.setContentDescription("RPM 참조 막대 · 전체 범위 0~15000 · KTM 제조사 레드라인 아님");
  base.addView(rpmMeter,new LinearLayout.LayoutParams(-1,24));
  temperature=value("냉각수 온도",23);
  voltage=value("ECU 전압",23);
  advisory=new TextView(this);
  advisory.setTextSize(14);advisory.setTextColor(0xffffc78d);
  advisory.setPadding(12,8,12,20);
  advisory.setText("센서 응답 대기 · 참고용 정보");
  advisory.setGravity(Gravity.CENTER);
  base.addView(advisory);
  linkStatus=new TextView(this);linkStatus.setText("OBD2 연결 상태 확인 중");
  linkStatus.setTextSize(15);linkStatus.setTextColor(0xffced8e2);
  linkStatus.setPadding(0,4,0,compactMode?8:13);
  linkStatus.setGravity(Gravity.CENTER);base.addView(linkStatus);
  ride=value("GPS 라이딩 기록",18);
  status=new TextView(this);status.setTextColor(0xffb8c5d3);status.setTextSize(13);status.setGravity(Gravity.CENTER);base.addView(status);
  SharedPreferences statusPrefs=getSharedPreferences(PREFS,0);
  status.setVisibility(statusPrefs.getBoolean("details",false)?View.VISIBLE:View.GONE);
  Button toggleDetails=new Button(this);
  toggleDetails.setText(status.getVisibility()==View.VISIBLE?"진단 상세 접기":"진단 상세 보기");
  toggleDetails.setContentDescription("OBD2 지원 PID와 연결 오류 상세 보기 또는 접기");
  toggleDetails.setOnClickListener(v->{
   boolean show=status.getVisibility()!=View.VISIBLE;
   status.setVisibility(show?View.VISIBLE:View.GONE);
   statusPrefs.edit().putBoolean("details",show).apply();
   toggleDetails.setText(show?"진단 상세 접기":"진단 상세 보기");
  });
  base.addView(toggleDetails);
  TextView warning=new TextView(this);warning.setText("실험용 읽기 전용 · ODO 및 연료소모량 미검증\nRPM 막대 최대값은 앱 표시 범위이며 레드라인이 아닙니다.\n순정 계기판 경고를 우선하고 주행 중 화면을 조작하지 마세요.");warning.setGravity(Gravity.CENTER);warning.setTextColor(0xffa3a9b0);warning.setTextSize(12);warning.setPadding(0,14,0,14);base.addView(warning);
  SharedPreferences prefs=getSharedPreferences(PREFS,0);
  Button landscape=new Button(this);
  landscape.setText(prefs.getBoolean("landscape",false)?"세로 화면으로 전환":"가로 화면으로 전환");
  landscape.setOnClickListener(v->{
   boolean next=!prefs.getBoolean("landscape",false);
   prefs.edit().putBoolean("landscape",next).apply();
   setRequestedOrientation(next?ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
   landscape.setText(next?"세로 화면으로 전환":"가로 화면으로 전환");
  });base.addView(landscape);
  Button keepAwake=new Button(this);
  keepAwake.setText(prefs.getBoolean("awake",false)?"화면 자동 꺼짐 허용":"이 화면에서만 켜짐 유지");
  keepAwake.setOnClickListener(v->{
   boolean next=!prefs.getBoolean("awake",false);
   prefs.edit().putBoolean("awake",next).apply();
   if(next)getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
   else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
   keepAwake.setText(next?"화면 자동 꺼짐 허용":"이 화면에서만 켜짐 유지");
  });base.addView(keepAwake);
  if(prefs.getBoolean("landscape",false))setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
  if(prefs.getBoolean("awake",false))getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  Button close=new Button(this);close.setText("닫기");close.setOnClickListener(v->finish());base.addView(close);
 }
 @Override protected void onResume(){super.onResume();visible=true;if(getSharedPreferences(PREFS,0).getBoolean("awake",false))getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);handler.post(update);}
 @Override protected void onPause(){visible=false;handler.removeCallbacks(update);getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);super.onPause();}
}
