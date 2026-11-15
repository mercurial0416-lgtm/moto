package com.motohub.next;

/** Stable, device-local diagnostics namespaces. No Bluetooth address or GPS fix. */
final class ObdBikeDiagnosticScope {
 private ObdBikeDiagnosticScope(){}
 static String key(String bikeId,String field){
  if(!RideBikeBinding.valid(bikeId)||field==null||!field.matches("[A-Za-z0-9_]{1,32}"))
   throw new IllegalArgumentException("invalid local OBD diagnostic key");
  return "bike_obd_"+bikeId+"_"+field;
 }
 static boolean shouldRecord(boolean autoArmed,String code){
  return autoArmed||"AUTO_OFF".equals(code)||"GPS_SAVED".equals(code);
 }
}
