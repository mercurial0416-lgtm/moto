package com.motohub.next;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.SystemClock;
import org.json.JSONObject;

/** Explicit one-time consent persisted for the selected bike. No receiver starts GPS on boot. */
final class ObdHandsFreeSession {
 private static final String PREFS="obd_handsfree_session";
 private static final String ENABLED="enabled",BIKE="bike_id",DEVICE="device";
 private static final String ATTEMPT="last_resume_elapsed";
 private ObdHandsFreeSession(){}
 static boolean enabled(Context c){
  return c.getSharedPreferences(PREFS,0).getBoolean(ENABLED,false);
 }
 static String bikeId(Context c){
  return c.getSharedPreferences(PREFS,0).getString(BIKE,"");
 }
 static boolean enable(Context c,String bikeId,String address){
  if(!RideBikeBinding.valid(bikeId)||address==null||!BluetoothAdapter.checkBluetoothAddress(address))
   return false;
  SharedPreferences.Editor edit=c.getSharedPreferences(PREFS,0).edit();
  return edit.putString(BIKE,bikeId).putString(DEVICE,address).putLong(ATTEMPT,0)
   .putBoolean(ENABLED,true).commit();
 }
 static void disable(Context c){
  c.getSharedPreferences(PREFS,0).edit().clear().commit();
 }
 /** Called only with a visible Activity; a boot receiver must NEVER call this. */
 static void resumeWhenVisible(MainActivity a){
  if(a.isFinishing()||a.isDestroyed()||a.readOnly||RideService.running||RideService.autoArmed||
     RestoreRideGate.isRestoring()||RestoreRideGate.isFormWriting()||!enabled(a))return;
  SharedPreferences prefs=a.getSharedPreferences(PREFS,0);
  String bike=prefs.getString(BIKE,""),device=prefs.getString(DEVICE,"");
  JSONObject registered=null;
  for(JSONObject vehicle:a.items("vehicles"))
   if(bike.equals(vehicle.optString("id"))){registered=vehicle;break;}
  if(!RideBikeBinding.valid(bike)||registered==null||GarageBikeStatus.sold(registered.optString("soldDate"))||
     device==null||!BluetoothAdapter.checkBluetoothAddress(device))return;
  if(a.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED ||
     Build.VERSION.SDK_INT>=31&&a.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED ||
     Build.VERSION.SDK_INT>=33&&a.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
  LocationManager location=a.getSystemService(LocationManager.class);
  if(location==null||!location.isProviderEnabled(LocationManager.GPS_PROVIDER))return;
  android.app.NotificationManager notifications=a.getSystemService(android.app.NotificationManager.class);
  if(notifications==null||!notifications.areNotificationsEnabled())return;
  long now=SystemClock.elapsedRealtime(),last=prefs.getLong(ATTEMPT,0);
  if(last>0&&now>=last&&now-last<60_000L)return;
  prefs.edit().putLong(ATTEMPT,now).apply();
  Intent start=new Intent(a,RideService.class).setAction(RideService.AUTO_ARM)
    .putExtra(RideService.EXTRA_DEVICE,device).putExtra(RideService.EXTRA_BIKE_ID,bike);
  try{a.startForegroundService(start);}
  catch(RuntimeException failure){a.toast("OBD 자동 감지 재개 실패 · 바이크 설정에서 권한을 확인해 주세요.");}
 }
}
