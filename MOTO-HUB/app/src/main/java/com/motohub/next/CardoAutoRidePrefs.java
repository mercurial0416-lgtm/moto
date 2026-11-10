package com.motohub.next;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import java.util.Locale;

/**
 * Explicit helmet-trigger consent. Only one nominated bike+exact MAC is
 * eligible, and enabling this mode disables the v594 polling-all-night mode.
 * No GPS track is started or modified by storing preferences.
 */
final class CardoAutoRidePrefs {
 private static final String FILE="cardo_autoride_v595";
 static final long PRESENCE_TTL_MS=900_000L;
 private CardoAutoRidePrefs(){}
 private static SharedPreferences p(Context c){return c.getSharedPreferences(FILE,Context.MODE_PRIVATE);}
 static boolean enabled(Context c){return p(c).getBoolean("enabled",false);}
 static String bikeId(Context c){return p(c).getString("bike","");}
 static String address(Context c){return p(c).getString("mac","");}
 static String name(Context c){return p(c).getString("name","Cardo");}
 static boolean forBike(Context c,String bike){
  if(!enabled(c)||!RideBikeBinding.valid(bike)||!bike.equals(bikeId(c)))return false;
  String obd=BikeObdPreferences.address(c,bike),cardo=address(c);
  return BikeObdPreferences.validAddress(cardo)&&BikeObdPreferences.validAddress(obd)&&
    !cardo.equalsIgnoreCase(obd);
 }
 static boolean matches(Context c,String mac){
  return enabled(c)&&BikeObdPreferences.validAddress(mac)&&
   mac.equalsIgnoreCase(address(c));
 }
 static synchronized boolean enable(Context c,String bikeId,String mac,String name){
  if(!RideBikeBinding.valid(bikeId)||!BikeObdPreferences.validAddress(mac)||
     !BikeObdPreferences.validAddress(BikeObdPreferences.address(c,bikeId))||
     mac.equalsIgnoreCase(BikeObdPreferences.address(c,bikeId)))return false;
  String safe=name==null?"Cardo":name.trim();
  if(safe.length()>64)safe=safe.substring(0,64);
  if(safe.isEmpty())safe="Cardo";
  // An old Android association must not keep observing a replaced headset.
  if(enabled(c)&&!mac.equalsIgnoreCase(address(c))){
   CardoCompanionPairing.stopObserving(c);
   String oldBike=bikeId(c);
   if(RideBikeBinding.valid(oldBike))
    BikeObdPreferences.setGpsFallbackEnabled(c,oldBike,false);
  }
  boolean saved=p(c).edit()
    .putBoolean("enabled",true).putString("bike",bikeId)
    .putString("mac",mac.toUpperCase(Locale.ROOT))
    .putString("name",safe).putLong("triggerAt",0).putBoolean("connected",false).remove("presenceAt").commit();
  if(saved)BikeObdPreferences.setAutoRideEnabled(c,bikeId,false);
  return saved;
 }
 static synchronized void disable(Context c){
  String oldBike=bikeId(c);
  if(RideBikeBinding.valid(oldBike))
   BikeObdPreferences.setGpsFallbackEnabled(c,oldBike,false);
  p(c).edit().clear().commit();
 }
 static synchronized void clearBike(Context c,String bike){
  if(bike!=null&&bike.equals(bikeId(c))){CardoCompanionPairing.stopObserving(c);disable(c);}
 }
 /** A real headset disconnect ends the prior trigger window.
  * A quick helmet restart must be eligible again rather than blocked for 90s. */
 static synchronized void noteDisconnect(Context c,String mac){notePresence(c,mac,false);}
 static synchronized void notePresence(Context c,String mac,boolean connected){
  if(!matches(c,mac))return;
  SharedPreferences.Editor e=p(c).edit().putBoolean("connected",connected)
    .putLong("presenceAt",SystemClock.elapsedRealtime());
  if(!connected)e.remove("triggerAt");
  e.commit();
 }
 static boolean recentlyConnected(Context c){
  SharedPreferences x=p(c);
  if(!enabled(c)||!x.getBoolean("connected",false))return false;
  long at=x.getLong("presenceAt",0),now=SystemClock.elapsedRealtime();
  return at>0&&now>=at&&now-at<=PRESENCE_TTL_MS;
 }
 static synchronized void releaseClaim(Context c,String mac){
  if(matches(c,mac)&&!RideService.running&&!RideService.autoArmed)
   p(c).edit().remove("triggerAt").commit();
 }
 static synchronized boolean claim(Context c,String mac,boolean rideActive){
  if(!matches(c,mac)||!forBike(c,bikeId(c)))return false;
  SharedPreferences x=p(c);
  long now=SystemClock.elapsedRealtime(),last=x.getLong("triggerAt",0);
  if(!CardoAutoRidePolicy.trigger(true,true,rideActive,last,now))return false;
  return x.edit().putLong("triggerAt",now).commit();
 }
 static void status(Context c,String status){
  if(enabled(c))p(c).edit().putString("status",status==null?"":status).apply();
 }
 static String status(Context c){return p(c).getString("status","아직 카르도 연결 감지 전");}
}