package com.motohub.next;
import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;

/** Cached selected-bike label only. Widgets never read private JSON, GPS or ECU data. */
final class WidgetBikeSnapshot {
 private WidgetBikeSnapshot(){}
 private static final String KEY="selected_bike_label";
 static String label(Context c){return c.getSharedPreferences("widget_bike",0).getString(KEY,"바이크 선택");}
 static boolean update(MainActivity a){
  JSONObject bike=a.bike();
  String label=bike==null?"바이크 선택":bike.optString("name","바이크");
  label=label.replace('\n',' ').replace('\r',' ').trim();
  if(label.isEmpty())label="바이크 선택";
  if(label.length()>13)label=label.substring(0,13)+"…";
  SharedPreferences prefs=a.getSharedPreferences("widget_bike",0);
  if(label.equals(prefs.getString(KEY,"")))return false;
  prefs.edit().putString(KEY,label).apply();
  return true;
 }
}