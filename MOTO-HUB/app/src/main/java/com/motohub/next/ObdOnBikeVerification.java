package com.motohub.next;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.Locale;
/** Manually confirmed vehicle checks, separate from read-only ECU observations. */
final class ObdOnBikeVerification {
 private static final String KEY="obd_on_bike_verified";
 private static final String[] ITEMS={
  "정차 상태에서 속도 0km/h 표시 확인",
  "계기판 RPM과 ECU RPM 수동 비교",
  "실차 계기판 속도와 ECU 속도 비교 (동승자 확인 등 안전한 조건)",
  "시동 OFF/ON 후 장치 재연결 확인",
  "화면 잠금·내비 전환 후 GPS 원본 보존 확인",
  "주행 종료·GPS 경로 및 백업 직접 확인"
 };
 private ObdOnBikeVerification(){}
 static void show(MainActivity a){
  SharedPreferences prefs=a.getSharedPreferences(KEY,0);
  boolean[] values=new boolean[ITEMS.length];
  for(int i=0;i<values.length;i++)values[i]=prefs.getBoolean("item_"+i,false);
  String observed="현재 수신 샘플 "+RideService.obdSpeedSamples+
    "건 · 재연결 "+RideService.obdReconnects+"회 · 요청 실패 "+RideService.obdQueryFailures+
    "회 · 평균 응답 "+(RideService.obdAverageLatencyMs<0?"미측정":RideService.obdAverageLatencyMs+"ms")+
    "\n속도: "+ObdTelemetryFreshness.speedLabel(RideService.obdSpeedKmh,
      RideService.obdLastSampleElapsed,android.os.SystemClock.elapsedRealtime())+
    "\nRPM: "+(ObdTelemetryFreshness.fresh(RideService.obdRpmAt,android.os.SystemClock.elapsedRealtime())&&
      Double.isFinite(RideService.obdRpm)?
      String.format(Locale.KOREA,"%.0f",RideService.obdRpm):"미수신 또는 신호 만료");
  new AlertDialog.Builder(a).setTitle("듀크 390 실차 검증 · 미검증")
   .setMessage("실시간 수신 진단\n"+observed+
    "\n\n아래 체크는 사용자가 실제 계기판을 대조한 경우에만 직접 선택하세요. 소프트웨어 테스트 통과를 실차 호환 확인으로 표시하지 않습니다. 이동 중 화면 조작은 금지합니다.")
   .setMultiChoiceItems(ITEMS,values,(d,index,on)->values[index]=on)
   .setNegativeButton("취소",null)
   .setNeutralButton("확인표시 초기화",(d,w)->{
    if(prefs.edit().clear().commit())a.toast("실차 검증 표시를 초기화했습니다.");
   })
   .setPositiveButton("수동 확인 내용 저장",(d,w)->{
    SharedPreferences.Editor edit=prefs.edit();
    for(int i=0;i<values.length;i++)edit.putBoolean("item_"+i,values[i]);
    edit.putLong("checked_at",System.currentTimeMillis());
    if(edit.commit())a.toast("실차 직접 확인 결과만 보관했습니다.");
    else a.toast("실차 확인 내용을 저장하지 못했습니다.");
   }).show();
 }
}
