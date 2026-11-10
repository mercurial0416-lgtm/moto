package com.motohub.next;

import android.companion.AssociationInfo;
import android.companion.CompanionDeviceService;
import android.companion.CompanionDeviceManager;
import android.companion.DevicePresenceEvent;
import android.os.Build;

/** System-managed low-energy presence observer (requires system association). */
@android.annotation.TargetApi(31)
public final class CardoCompanionService extends CompanionDeviceService {
 @android.annotation.TargetApi(33)
 @Override public void onDeviceAppeared(AssociationInfo info){
  if(Build.VERSION.SDK_INT<36)onMatch(info,true);
 }
 @android.annotation.TargetApi(33)
 @Override public void onDeviceDisappeared(AssociationInfo info){
  if(Build.VERSION.SDK_INT<36)onMatch(info,false);
 }
 @android.annotation.TargetApi(36)
 @Override public void onDevicePresenceEvent(DevicePresenceEvent event){
  if(Build.VERSION.SDK_INT<36)return;
  if(event==null)return;
  int id=event.getAssociationId();
  int kind=event.getEvent();
  if(kind!=DevicePresenceEvent.EVENT_BT_CONNECTED&&
     kind!=DevicePresenceEvent.EVENT_BT_DISCONNECTED)return;
  CompanionDeviceManager mgr=getSystemService(CompanionDeviceManager.class);
  if(mgr==null)return;
  try{for(AssociationInfo info:mgr.getMyAssociations())
    if(info.getId()==id){onMatch(info,kind==DevicePresenceEvent.EVENT_BT_CONNECTED);break;}}
  catch(RuntimeException ignored){}
 }
 @android.annotation.TargetApi(33)
 private void onMatch(AssociationInfo info,boolean connected){
  if(info==null||info.getDeviceMacAddress()==null)return;
  String mac=info.getDeviceMacAddress().toString();
  if(CardoAutoRidePrefs.matches(this,mac))CardoRideStarter.cardoEvent(this,mac,connected);
 }
}