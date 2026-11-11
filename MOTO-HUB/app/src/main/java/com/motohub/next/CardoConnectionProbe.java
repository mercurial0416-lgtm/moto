package com.motohub.next;
import android.Manifest;
import android.bluetooth.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
/** Foreground-only exact-MAC live connection probe; never triggers a ride. */
final class CardoConnectionProbe {
 enum Status { CONNECTED, DISCONNECTED, UNKNOWN, BLUETOOTH_OFF, PERMISSION }
 private CardoConnectionProbe(){}
 static void probe(Context c,String mac,Consumer<Status> callback){
  if(callback==null)return;
  if(!BikeObdPreferences.validAddress(mac)){callback.accept(Status.UNKNOWN);return;}
  if(Build.VERSION.SDK_INT>=31&&
     c.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){
   callback.accept(Status.PERMISSION);return;
  }
  BluetoothManager manager=c.getSystemService(BluetoothManager.class);
  BluetoothAdapter adapter=manager==null?null:manager.getAdapter();
  if(adapter==null){callback.accept(Status.UNKNOWN);return;}
  try{
   if(!adapter.isEnabled()){callback.accept(Status.BLUETOOTH_OFF);return;}
   final AtomicInteger done=new AtomicInteger(),readable=new AtomicInteger();
   final AtomicBoolean reported=new AtomicBoolean(),found=new AtomicBoolean();
   Runnable maybeFinish=()->{
    if(done.get()!=2||!reported.compareAndSet(false,true))return;
    callback.accept(found.get()?Status.CONNECTED:readable.get()==2?Status.DISCONNECTED:Status.UNKNOWN);
   };
   for(int type:new int[]{BluetoothProfile.HEADSET,BluetoothProfile.A2DP}){
    try{
     boolean started=adapter.getProfileProxy(c.getApplicationContext(),
      new BluetoothProfile.ServiceListener(){
       @Override public void onServiceConnected(int profile,BluetoothProfile proxy){
        try{
         readable.incrementAndGet();
         for(BluetoothDevice device:proxy.getConnectedDevices())
          if(mac.equalsIgnoreCase(device.getAddress())){found.set(true);break;}
        }catch(SecurityException error){readable.decrementAndGet();}
        finally{
         try{adapter.closeProfileProxy(profile,proxy);}catch(RuntimeException ignored){}
         done.incrementAndGet();
         if(found.get()&&reported.compareAndSet(false,true))callback.accept(Status.CONNECTED);
         else maybeFinish.run();
        }
       }
       @Override public void onServiceDisconnected(int profile){
        done.incrementAndGet();maybeFinish.run();
       }
      },type);
     if(!started){done.incrementAndGet();maybeFinish.run();}
    }catch(SecurityException denied){done.incrementAndGet();maybeFinish.run();}
   }
  }catch(SecurityException denied){callback.accept(Status.PERMISSION);}
 }
}
