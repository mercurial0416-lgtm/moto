package com.motohub.next;

/** Namespaces remembered OBD settings by stable local vehicle ID. */
final class BikeObdPreferences {
 private BikeObdPreferences(){}
 static String adapterKey(String bikeId){return "obd_remembered_device_"+id(bikeId);}
 static String stopAfterKey(String bikeId){return "obd_stop_after_ms_"+id(bikeId);}
 static String powerSaveKey(String bikeId){return "obd_save_battery_"+id(bikeId);}
 private static String id(String bikeId){
  if(bikeId==null||bikeId.trim().isEmpty())throw new IllegalArgumentException("바이크 ID가 필요합니다.");
  return bikeId;
 }
}
