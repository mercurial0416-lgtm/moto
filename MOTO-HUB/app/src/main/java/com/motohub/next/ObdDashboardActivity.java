package com.motohub.next;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.os.Handler;
import android.os.Looper;
import java.util.Locale;

/** Experimental, local-only telemetry view. No interactive riding controls. */
public final class ObdDashboardActivity extends Activity {
 private final Handler handler=new Handler(Looper.getMainLooper());
 private TextView speed,rpm,temperature,voltage,ride,status;
 private boolean visible;
 private final Runnable update=new Runnable(){@Override public void run(){
  if(!visible)return;
  if(!getSharedPreferences("developer_experiments",0).getBoolean("enabled",false)){finish();return;}
  long now=android.os.SystemClock.elapsedRealtime();
  boolean speedFresh=RideService.autoArmed&&ObdTelemetryFreshness.speedFresh(RideService.obdLastSampleElapsed,now);
  speed.setText(speedFresh&&RideService.obdSpeedKmh>=0?RideService.obdSpeedKmh+"":"—");
  rpm.setText(RideService.autoArmed&&ObdTelemetryFreshness.fresh(RideService.obdRpmAt,now)?format(RideService.obdRpm,"%.0f"):"—");
  temperature.setText(RideService.autoArmed&&ObdTelemetryFreshness.fresh(RideService.obdCoolantAt,now)?format(RideService.obdCoolantC,"%.0f °C"):"—");
  voltage.setText(RideService.autoArmed&&ObdTelemetryFreshness.fresh(RideService.obdVoltageAt,now)?format(RideService.obdVoltage,"%.1f V"):"—");
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
  base=new LinearLayout(this);base.setBackgroundColor(0xff0c131a);base.setPadding(24,36,24,20);base.setOrientation(LinearLayout.VERTICAL);
  setContentView(base);
  TextView title=new TextView(this);title.setText("MOTO HUB · DEV DASH");title.setTextColor(0xffff8641);title.setTextSize(20);title.setGravity(Gravity.CENTER);title.setPadding(0,0,0,18);base.addView(title);
  speed=value("차량 속도  ·  km/h",52);
  rpm=value("엔진 회전수  ·  RPM",29);
  temperature=value("냉각수 온도",23);
  voltage=value("ECU 전압",23);
  ride=value("GPS 라이딩 기록",18);
  status=new TextView(this);status.setTextColor(0xffb8c5d3);status.setTextSize(13);status.setGravity(Gravity.CENTER);base.addView(status);
  TextView warning=new TextView(this);warning.setText("실험용 읽기 전용 · ODO 및 연료소모량 미검증\n카카오내비 길 안내와 독립 동작 · 주행 중 화면 조작 금지");warning.setGravity(Gravity.CENTER);warning.setTextColor(0xffa3a9b0);warning.setTextSize(12);warning.setPadding(0,14,0,14);base.addView(warning);
  Button close=new Button(this);close.setText("닫기");close.setOnClickListener(v->finish());base.addView(close);
 }
 @Override protected void onResume(){super.onResume();visible=true;handler.post(update);}
 @Override protected void onPause(){visible=false;handler.removeCallbacks(update);super.onPause();}
}
