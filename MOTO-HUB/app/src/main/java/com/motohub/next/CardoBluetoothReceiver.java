package com.motohub.next;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;

/** Android Bluetooth ACL broadcasts are an implicit-broadcast exception. */
public final class CardoBluetoothReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent intent){
  if(intent==null||!CardoAutoRidePrefs.enabled(c))return;
  String action=intent.getAction();
  if(!BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)&&
     !BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action))return;
  if(Build.VERSION.SDK_INT>=31&&
     c.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)return;
  try{
   BluetoothDevice device=Build.VERSION.SDK_INT>=33?
     intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE,BluetoothDevice.class):
     (BluetoothDevice)intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
   if(device==null)return;
   String address=device.getAddress();
   if(!CardoAutoRidePrefs.matches(c,address))return;
   CardoRideStarter.cardoEvent(c,address,BluetoothDevice.ACTION_ACL_CONNECTED.equals(action));
  }catch(SecurityException ignored){}
 }
}