package com.motohub.next;
import android.Manifest;
import android.bluetooth.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import java.util.*;
import java.util.function.Consumer;
/**
 * Read-only, exact-MAC Cardo audio connection evidence. One in-flight query
 * per MAC; all waiters receive one result, even if Android never calls back.
 * Does not start Bluetooth discovery, a GPS service, or a riding session.
 */
final class CardoConnectionProbe {
 enum Status { CONNECTED, DISCONNECTED, UNKNOWN, BLUETOOTH_OFF, PERMISSION }
 static final long TIMEOUT_MS=4000L;
 private static final Handler MAIN=new Handler(Looper.getMainLooper());
 private static final Map<String,Probe> PENDING=new HashMap<>();
 private CardoConnectionProbe(){}
 private static final class Probe {
  final String mac;final ArrayList<Consumer<Status>> waiters=new ArrayList<>();
  int completed=0,readable=0;boolean found=false,finished=false;
  Probe(String mac){this.mac=mac;}
 }
 static boolean inFlight(String mac){synchronized(PENDING){return PENDING.containsKey(mac);}}
 static void probe(Context c,String mac,Consumer<Status> callback){
  if(callback==null)return;
  if(!BikeObdPreferences.validAddress(mac)){onMain(callback,Status.UNKNOWN);return;}
  if(Build.VERSION.SDK_INT>=31&&
    c.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){
   onMain(callback,Status.PERMISSION);return;
  }
  BluetoothManager manager=c.getSystemService(BluetoothManager.class);
  BluetoothAdapter adapter=manager==null?null:manager.getAdapter();
  if(adapter==null){onMain(callback,Status.UNKNOWN);return;}
  try{
   if(!adapter.isEnabled()){onMain(callback,Status.BLUETOOTH_OFF);return;}
  }catch(SecurityException denied){onMain(callback,Status.PERMISSION);return;}
  final Probe p;
  synchronized(PENDING){
   Probe existing=PENDING.get(mac);
   if(existing!=null){
    // Avoid an unbounded queue if the system repeatedly misses a callback.
    if(existing.waiters.size()<3)existing.waiters.add(callback);
    return;
   }
   p=new Probe(mac);p.waiters.add(callback);PENDING.put(mac,p);
  }
  MAIN.postDelayed(()->finish(p,Status.UNKNOWN),TIMEOUT_MS);
  Context app=c.getApplicationContext();
  for(int profile:new int[]{BluetoothProfile.HEADSET,BluetoothProfile.A2DP}){
   try{
    boolean launched=adapter.getProfileProxy(app,new BluetoothProfile.ServiceListener(){
     @Override public void onServiceConnected(int type,BluetoothProfile proxy){
      boolean read=false,found=false;
      try{
       for(BluetoothDevice device:proxy.getConnectedDevices())
        if(p.mac.equalsIgnoreCase(device.getAddress()))found=true;
       read=true;
      }catch(SecurityException ignored){}
      finally{
       try{adapter.closeProfileProxy(type,proxy);}catch(RuntimeException ignored){}
      }
      answer(p,read,found);
     }
     @Override public void onServiceDisconnected(int type){answer(p,false,false);}
    },profile);
    if(!launched)answer(p,false,false);
   }catch(SecurityException ignored){answer(p,false,false);}
  }
 }
 private static void answer(Probe p,boolean readable,boolean found){
  Status result=null;
  synchronized(PENDING){
   if(p.finished)return;
   p.completed++;if(readable)p.readable++;
   if(found)p.found=true;
   if(p.found)result=Status.CONNECTED;
   else if(p.completed>=2)result=p.readable==2?Status.DISCONNECTED:Status.UNKNOWN;
  }
  if(result!=null)finish(p,result);
 }
 private static void finish(Probe p,Status result){
  final ArrayList<Consumer<Status>> callbacks;
  synchronized(PENDING){
   if(p.finished)return;
   p.finished=true;PENDING.remove(p.mac);
   callbacks=new ArrayList<>(p.waiters);p.waiters.clear();
  }
  for(Consumer<Status> callback:callbacks)onMain(callback,result);
 }
 private static void onMain(Consumer<Status> cb,Status status){
  if(Looper.myLooper()==Looper.getMainLooper())cb.accept(status);
  else MAIN.post(()->cb.accept(status));
 }
}
