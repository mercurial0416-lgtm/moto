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
import android.view.WindowManager;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import java.util.Locale;

/** Experimental, local-only telemetry view. No interactive riding controls. */
public final class ObdDashboardActivity extends Activity {
 private final Handler handler=new Handler(Looper.getMainLooper());
 private TextView speed,rpm,temperature,voltage,ride,status,advisory;
 private ProgressBar rpmMeter;
 private boolean visible;
 private static final String PREFS="obd_dashboard_preferences";
 private final Runnable update=new Runnable(){@Override public void run(){
  if(!visible)return;
  if(!getSharedPreferences("developer_experiments",0).getBoolean("enabled",false)){finish();return;}
  long now=android.os.SystemClock.elapsedRealtime();
  boolean speedFresh=RideService.autoArmed&&ObdTelemetryFreshness.speedFresh(RideService.obdLastSampleElapsed,now);
  speed.setText(speedFresh&&RideService.obdSpeedKmh>=0?RideService.obdSpeedKmh+"":"—");
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
  ride.setText(RideService.running?String.format(Locale.KOREA,"GPS %.2f km · 최고 %.0f km/h",RideService.liveDistanceKm,RideService.livePeakKmh):
    RideService.autoArmed?"자동 기록 대기 · GPS 기록 시작 전":"라이딩 미작동");
  status.setText("OBD2 · "+RideService.obdLink+" · 재연결 "+RideService.obdReconnects+"회\n"+
   ObdTelemetryFreshness.connectionLabel(RideService.autoArmed,RideService.running,speedFresh)+
   "\n"+RideService.engineEstimate()+"\n지원 PID: "+RideService.obdPidSupport+
   "\n"+ObdRestStatus.label(RideService.autoArmed,RideService.running,RideService.obdStationaryRemainingMs)+
   "\n보조 센서 진단: "+RideService.obdAuxDiagnostics+"\n최근 OBD 오류: "+RideService.obdLastError);
  handler.postDelayed(this,1300);
 }};
 static String format(double x,String fmt){return Double.isFinite(x)?String.format(Locale.KOREA,fmt,x):"—";}
 private LinearLayout base;
 private TextView value(String caption,int size){
  TextView label=new TextView(this);label.setText(caption);label.setTextSize(14);label.setTextColor(0xffb8c5d3);label.setGravity(Gravity.CENTER);base.addView(label);
  TextView v=new TextView(this);v.setText("—");v.setTextSize(size);v.setTypeface(null,android.graphics.Typeface.BOLD);v.setTextColor(Color.WHITE);v.setGravity(Gravity.CENTER);
  v.setPadding(0,7,0,24);base.addView(v);return v;
 }
 @Override public void onCreate(Bundle state){super.onCreate(state);
  if(!getSharedPreferences("developer_experiments",0).getBoolean("enabled",false)){finish();return;}
  ScrollView scroll=new ScrollView(this);
  scroll.setFillViewport(true);
  base=new LinearLayout(this);base.setBackgroundColor(0xff0c131a);
  base.setPadding(24,36,24,30);base.setOrientation(LinearLayout.VERTICAL);
  scroll.addView(base);
  setContentView(scroll);
  TextView title=new TextView(this);title.setText("MOTO HUB · DEV DASH");title.setTextColor(0xffff8641);title.setTextSize(20);title.setGravity(Gravity.CENTER);title.setPadding(0,0,0,18);base.addView(title);
  speed=value("차량 속도  ·  km/h",52);
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
  ride=value("GPS 라이딩 기록",18);
  status=new TextView(this);status.setTextColor(0xffb8c5d3);status.setTextSize(13);status.setGravity(Gravity.CENTER);base.addView(status);
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
 @Override protected void onResume(){super.onResume();visible=true;handler.post(update);}
 @Override protected void onPause(){visible=false;handler.removeCallbacks(update);super.onPause();}
}
