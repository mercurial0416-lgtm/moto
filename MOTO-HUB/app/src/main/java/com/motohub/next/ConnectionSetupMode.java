package com.motohub.next;
import android.content.Context;
import android.content.SharedPreferences;
/** User-picked setup route only. Persisting a choice NEVER starts a ride or changes ECU. */
final class ConnectionSetupMode {
 enum Mode {
  NONE("선택 전",false,false),
  CARDO_OBD("카르도 + OBD",true,true),
  OBD_ONLY("OBD만 사용",true,false),
  CARDO_GPS("카르도 + GPS",false,true);
  final String label;final boolean obd,cardo;
  Mode(String label,boolean obd,boolean cardo){this.label=label;this.obd=obd;this.cardo=cardo;}
  boolean needsObd(){return obd;}
  boolean needsCardo(){return cardo;}
 }
 private ConnectionSetupMode(){}
 private static SharedPreferences p(Context c){return c.getSharedPreferences("moto_connection_setup_v603",0);}
 static Mode get(Context c,String bike){
  if(!RideBikeBinding.valid(bike))return Mode.NONE;
  String raw=p(c).getString(bike+".mode","");
  if(!raw.isEmpty())try{return Mode.valueOf(raw);}catch(IllegalArgumentException ignore){}
  // Preserve the legacy session's actual behavior until the user chooses a mode.
  boolean obd=!BikeObdPreferences.address(c,bike).isEmpty();
  boolean cardo=CardoAutoRidePrefs.forBike(c,bike);
  if(cardo&&obd)return Mode.CARDO_OBD;
  if(cardo&&BikeObdPreferences.gpsFallbackEnabled(c,bike))return Mode.CARDO_GPS;
  if(BikeObdPreferences.autoRideEnabled(c,bike))return Mode.OBD_ONLY;
  return Mode.NONE;
 }
 static boolean explicit(Context c,String bike){
  return RideBikeBinding.valid(bike)&&p(c).contains(bike+".mode");
 }
 static void set(Context c,String bike,Mode mode){
  if(!RideBikeBinding.valid(bike)||mode==null||mode==Mode.NONE)
   throw new IllegalArgumentException("유효한 바이크와 연결 방식을 선택해 주세요.");
  if(!p(c).edit().putString(bike+".mode",mode.name()).commit())
   throw new IllegalStateException("연결 방식 저장 실패");
 }
}
