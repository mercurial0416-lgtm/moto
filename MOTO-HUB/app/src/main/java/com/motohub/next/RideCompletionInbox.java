package com.motohub.next;
import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;
import java.util.Locale;
/** Durable, GPS-only post-commit recap. No raw coordinates, device IDs or uploads. */
final class RideCompletionInbox {
 private static final String PREFS="ride_completion_inbox";
 private RideCompletionInbox(){}
 static void record(Context context,File track){
  if(context==null||track==null||!RideMileageIntegrity.validCompleted(track))return;
  try{
   RideInsights.Result result=RideInsights.analyze(track);
   if(result.points<=0)return;
   String recap=String.format(Locale.KOREA,
    "저장된 GPS 좌표 %d개\n분석 이동거리 %.2f km\nGPS 첫·마지막 수신 간격 %d분\n관측 이동 %d분\nGPS 상태: %s\n\nGPS 수신 공백은 계산하지 않았으며 실제 계기판과 차이가 날 수 있습니다.",
    result.points,result.distanceKm,result.durationMillis/60000,
    result.movingMillis/60000,result.gpsQuality());
   SharedPreferences prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
   prefs.edit().putString("title","라이딩 저장 완료")
    .putString("summary",recap).commit();
  }catch(Exception ignored){/* Preserve original and completion marker for recovery. */}
 }
}