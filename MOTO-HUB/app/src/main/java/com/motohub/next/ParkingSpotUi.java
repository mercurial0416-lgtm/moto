package com.motohub.next;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.SystemClock;
import android.provider.Settings;
import android.widget.Button;
import org.json.JSONObject;
import java.util.Locale;

/** One-shot, opt-in parking point stored on the bike record for local ZIP backup. */
final class ParkingSpotUi {
 private final MainActivity a;
 ParkingSpotUi(MainActivity activity){a=activity;}
 static boolean stored(JSONObject bike){
  if(bike==null)return false;
  double lat=bike.optDouble("parkingLat",Double.NaN),lon=bike.optDouble("parkingLon",Double.NaN);
  return Double.isFinite(lat)&&Double.isFinite(lon)&&Math.abs(lat)<=90&&Math.abs(lon)<=180
    &&bike.optLong("parkingSavedAt",0)>0;
 }
 static String parkedInfo(JSONObject bike){
  if(!stored(bike))return "저장된 주차 위치가 없어요.";
  long saved=bike.optLong("parkingSavedAt",0),age=System.currentTimeMillis()-saved;
  String when=new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.KOREA).format(new java.util.Date(saved));
  float accuracy=(float)bike.optDouble("parkingAccuracyM",Double.NaN);
  String quality=Float.isFinite(accuracy)?" · GPS 오차 약 "+Math.round(accuracy)+"m":" · GPS 정확도 미기록";
  String stale=age<0?" · 기기 시계를 확인하세요":age>24L*60*60*1000?" · 하루 이상 지난 저장 위치":"";
  return "저장 시각: "+when+quality+stale+"\\n직접 저장한 위치만 보관하며 자동 추적하지 않습니다.";
 }
 void show(JSONObject bike){
  if(bike==null)return;
  String bikeId=bike.optString("id"),bikeName=bike.optString("name");
  String[] actions=stored(bike)?
   new String[]{"저장한 주차 위치 지도에서 보기","현재 위치를 주차 위치로 다시 저장","저장된 주차 위치 삭제"}:
   new String[]{"현재 위치를 주차 위치로 저장"};
  new AlertDialog.Builder(a).setTitle(bikeName+" · 주차 위치")
   .setMessage(stored(bike)?parkedInfo(bike):"주차한 곳에서 저장을 눌러 현재 GPS 위치를 한 번만 확인합니다.")
   .setItems(actions,(d,index)->{
    if(stored(bike)){if(index==0)view(bike);else if(index==1)capture(bikeId);else clear(bikeId);}
    else capture(bikeId);
   }).setNegativeButton("닫기",null).show();
 }
 private void capture(String id){
  if(a.readOnly){a.toast("원본 보호 중이므로 위치 저장이 차단됐어요.");return;}
  if(RideService.running){a.toast("GPS 기록 중에는 주차 위치를 저장하지 않아요. 종료 후 다시 시도하세요.");return;}
  if(a.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
   new AlertDialog.Builder(a).setTitle("정확한 위치 권한 필요")
    .setMessage("주차 위치 저장은 현재 위치 권한이 있어야 해요. 권한을 켠 뒤 주차한 자리에서 다시 눌러 주세요.")
    .setNegativeButton("닫기",null)
    .setPositiveButton("앱 권한 설정",(d,w)->a.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+a.getPackageName())))).show();
   return;
  }
  LocationManager manager=(LocationManager)a.getSystemService(android.content.Context.LOCATION_SERVICE);
  if(manager==null||!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
   a.toast("GPS 위치가 꺼져 있어요. 위치를 켜고 다시 눌러 주세요.");return;
  }
  try{
   Location recent=manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
   if(accurate(recent)){save(id,recent);return;}
   if(Build.VERSION.SDK_INT<30){a.toast("현재 위치를 확인하지 못했어요. GPS를 켜고 다시 시도하세요.");return;}
   a.toast("GPS 현재 위치 확인 중 · 위치를 찾으면 저장해요.");
   CancellationSignal cancel=new CancellationSignal();
   final boolean[] finished={false};
   android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
   handler.postDelayed(()->{if(!finished[0]){finished[0]=true;cancel.cancel();if(!a.isFinishing())a.toast("GPS 위치 확인 실패 · 열린 공간에서 재시도해 주세요.");}},15000);
   manager.getCurrentLocation(LocationManager.GPS_PROVIDER,cancel,a.getMainExecutor(),location->{
    if(finished[0])return;finished[0]=true;
    if(a.isFinishing()||a.isDestroyed())return;
    if(!accurate(location)){a.toast("위치 정확도가 부족해요. 잠시 뒤 다시 시도하세요.");return;}
    save(id,location);
   });
  }catch(SecurityException denied){a.toast("위치 권한을 다시 확인해 주세요.");}
   catch(RuntimeException unavailable){a.toast("GPS 위치를 확인할 수 없어요.");}
 }
 static boolean accurate(Location location){
  if(location==null||!location.hasAccuracy()||location.getAccuracy()>100||location.getAccuracy()<0)return false;
  long age=SystemClock.elapsedRealtimeNanos()-location.getElapsedRealtimeNanos();
  return age>=0&&age<=60L*1000*1000*1000
    &&Double.isFinite(location.getLatitude())&&Double.isFinite(location.getLongitude())
    &&Math.abs(location.getLatitude())<=90&&Math.abs(location.getLongitude())<=180;
 }
 private void save(String id,Location point){
  a.attempt(()->{
   if(a.readOnly||RideService.running)throw new IllegalStateException("라이딩 종료 후 다시 시도하세요.");
   JSONObject next=new JSONObject(a.db.toString());
   JSONObject bike=Store.find(next,"vehicles",id);
   if(bike==null)throw new IllegalArgumentException("등록된 바이크를 찾지 못했어요.");
   if(!id.equals(a.bike()==null?"":a.bike().optString("id")))
     throw new IllegalArgumentException("선택 바이크가 바뀌었어요. 다시 저장해 주세요.");
   bike.put("parkingLat",point.getLatitude()).put("parkingLon",point.getLongitude())
    .put("parkingSavedAt",System.currentTimeMillis()).put("parkingAccuracyM",point.getAccuracy());
   a.write(next);a.renderPreservingPosition();a.toast("주차 위치를 이 바이크에 저장했어요.");
  });
 }
 private void clear(String id){
  new AlertDialog.Builder(a).setTitle("주차 위치 삭제")
   .setMessage("이 바이크에 저장한 주차 좌표만 지웁니다. GPS 라이딩 원본은 유지해요.")
   .setNegativeButton("취소",null).setPositiveButton("삭제",(d,w)->a.attempt(()->{
    JSONObject next=new JSONObject(a.db.toString());
    JSONObject bike=Store.find(next,"vehicles",id);
    if(bike==null)throw new IllegalArgumentException("바이크가 없어요.");
    bike.remove("parkingLat");bike.remove("parkingLon");bike.remove("parkingSavedAt");bike.remove("parkingAccuracyM");
    a.write(next);a.renderPreservingPosition();a.toast("주차 위치를 삭제했어요.");
   })).show();
 }
 private void view(JSONObject bike){
  if(!stored(bike)){a.toast("저장된 주차 위치가 없어요.");return;}
  double lat=bike.optDouble("parkingLat"),lon=bike.optDouble("parkingLon");
  String coord=String.format(Locale.US,"%.7f,%.7f",lat,lon);
  Uri uri=Uri.parse("geo:"+coord+"?q="+Uri.encode(coord+" (바이크 주차 위치)"));
  try{a.startActivity(new Intent(Intent.ACTION_VIEW,uri));}
  catch(Exception noMap){a.toast("위치를 표시할 지도 앱을 찾을 수 없어요.");}
 }
}
