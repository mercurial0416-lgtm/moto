package com.motohub.next;

import android.companion.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.MacAddress;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

/**
 * A deliberate Android companion association for the user's bonded Cardo.
 * This gives Android a proper system-level presence source, not a perpetual
 * Bluetooth scan and not an invented Cardo SDK integration.
 */
final class CardoCompanionPairing {
 static final int REQUEST=595;
 private CardoCompanionPairing(){}

 static void associate(MainActivity a,String mac){
  if(Build.VERSION.SDK_INT<26||!BikeObdPreferences.validAddress(mac)||
     !a.getPackageManager().hasSystemFeature(PackageManager.FEATURE_COMPANION_DEVICE_SETUP)){
   a.toast("이 Android 기기는 동반 기기 감지를 지원하지 않습니다 · 일반 Bluetooth 감지 사용");
   return;
  }
  CompanionDeviceManager mgr=a.getSystemService(CompanionDeviceManager.class);
  if(mgr==null){a.toast("Android 동반 기기 관리자 사용 불가");return;}
  try{
   if(Build.VERSION.SDK_INT>=33){
    for(AssociationInfo info:mgr.getMyAssociations()){
     MacAddress got=info.getDeviceMacAddress();
     if(got!=null&&mac.equalsIgnoreCase(got.toString())){
      observe(a,info);
      a.toast("이미 승인된 Cardo 동반 기기 · 저전력 연결 감지 활성화");
      return;
     }
    }
   }
   BluetoothDeviceFilter filter=new BluetoothDeviceFilter.Builder()
    .setAddress(mac).build();
   AssociationRequest request=new AssociationRequest.Builder()
    .addDeviceFilter(filter).setSingleDevice(true).build();
   mgr.associate(request,new CompanionDeviceManager.Callback(){
    @Override public void onAssociationPending(IntentSender sender){launch(a,sender);}
    @Override public void onDeviceFound(IntentSender sender){launch(a,sender);}
    @Override public void onAssociationCreated(AssociationInfo info){observe(a,info);}
    @Override public void onFailure(CharSequence error){
     a.runOnUiThread(()->a.toast("시스템 카르도 동반 기기 등록 실패 · Bluetooth 감지는 유지"));
    }
   },new Handler(Looper.getMainLooper()));
  }catch(RuntimeException ex){a.toast("동반 기기 권한·연결 확인 필요 · Bluetooth 감지는 유지");}
 }
 private static void launch(MainActivity a,IntentSender sender){
  try{a.startIntentSenderForResult(sender,REQUEST,null,0,0,0);}
  catch(IntentSender.SendIntentException e){a.toast("카르도 동반 기기 승인 화면 열기 실패");}
 }
 static void afterSystemApproval(MainActivity a){
  if(!CardoAutoRidePrefs.enabled(a)||Build.VERSION.SDK_INT<33)return;
  CompanionDeviceManager mgr=a.getSystemService(CompanionDeviceManager.class);
  if(mgr==null)return;
  try{for(AssociationInfo info:mgr.getMyAssociations()){
   if(info.getDeviceMacAddress()!=null&&
      CardoAutoRidePrefs.matches(a,info.getDeviceMacAddress().toString()))observe(a,info);
  }}catch(RuntimeException ignored){}
 }
 private static void observe(Context c,AssociationInfo info){
  if(info==null||info.getDeviceMacAddress()==null||
     !CardoAutoRidePrefs.matches(c,info.getDeviceMacAddress().toString()))return;
  CompanionDeviceManager mgr=c.getSystemService(CompanionDeviceManager.class);
  if(mgr==null)return;
  try{
   if(Build.VERSION.SDK_INT>=36){
    ObservingDevicePresenceRequest request=new ObservingDevicePresenceRequest.Builder()
     .setAssociationId(info.getId()).build();
    mgr.startObservingDevicePresence(request);
   }else{
    mgr.startObservingDevicePresence(info.getDeviceMacAddress().toString());
   }
   CardoAutoRidePrefs.status(c,"Android 동반 기기 연결 감지 활성화 · 카르도 연결 대기");
  }catch(RuntimeException ex){
   CardoAutoRidePrefs.status(c,"동반 기기 감지 미승인 · 일반 Bluetooth 이벤트 사용");
  }
 }
 static void stopObserving(Context c){
  if(Build.VERSION.SDK_INT<33)return;
  CompanionDeviceManager mgr=c.getSystemService(CompanionDeviceManager.class);
  if(mgr==null)return;
  try{for(AssociationInfo info:mgr.getMyAssociations()){
   if(info.getDeviceMacAddress()==null||!CardoAutoRidePrefs.matches(c,info.getDeviceMacAddress().toString()))continue;
   try{
    if(Build.VERSION.SDK_INT>=36)
     mgr.stopObservingDevicePresence(new ObservingDevicePresenceRequest.Builder()
      .setAssociationId(info.getId()).build());
    else mgr.stopObservingDevicePresence(info.getDeviceMacAddress().toString());
   }catch(RuntimeException ignored){}
  }}catch(RuntimeException ignored){}
 }
}
