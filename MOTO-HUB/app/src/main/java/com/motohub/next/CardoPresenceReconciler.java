package com.motohub.next;
import android.Manifest;
import android.bluetooth.*;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.SystemClock;
/** Foreground reconciliation of lost classic Bluetooth ACL events; no scanning. */
final class CardoPresenceReconciler {
 private static long lastCheck;
 private CardoPresenceReconciler(){}
 static synchronized void onForeground(Context c){
  if(!CardoAutoRidePrefs.enabled(c)||RideService.running||RideService.autoArmed)return;
  long now=SystemClock.elapsedRealtime();
  if(lastCheck>0&&now>=lastCheck&&now-lastCheck<15_000L)return;
  lastCheck=now;
  if(Build.VERSION.SDK_INT>=31&&
     c.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)return;
  BluetoothManager mgr=c.getSystemService(BluetoothManager.class);
  BluetoothAdapter adapter=mgr==null?null:mgr.getAdapter();
  if(adapter==null||!adapter.isEnabled())return;
  Context app=c.getApplicationContext();
  for(int kind:new int[]{BluetoothProfile.HEADSET,BluetoothProfile.A2DP}){
   try{
    adapter.getProfileProxy(app,new BluetoothProfile.ServiceListener(){
     @Override public void onServiceConnected(int profile,BluetoothProfile proxy){
      try{
       for(BluetoothDevice device:proxy.getConnectedDevices())
        if(CardoAutoRidePrefs.matches(app,device.getAddress())){
         CardoRideStarter.cardoEvent(app,device.getAddress(),true);break;
        }
      }catch(SecurityException ignored){}
      finally{adapter.closeProfileProxy(profile,proxy);}
     }
     @Override public void onServiceDisconnected(int profile){}
    },kind);
   }catch(SecurityException ignored){}
  }
 }
}
