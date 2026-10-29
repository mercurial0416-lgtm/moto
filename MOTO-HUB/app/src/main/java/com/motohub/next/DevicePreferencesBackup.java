package com.motohub.next;
import android.content.Context;
import android.content.SharedPreferences;
import java.io.IOException;
import org.json.JSONObject;
/** Explicitly allowlisted UI choices only; no credentials, IDs, access tokens or device secrets. */
final class DevicePreferencesBackup {
 private DevicePreferencesBackup(){}
 static String snapshot(Context c)throws IOException{
  try{
   SharedPreferences home=c.getSharedPreferences("home_shortcuts",0);
   JSONObject json=new JSONObject();
   json.put("schema",1).put("order",String.join(",",HomeShortcutOrder.parse(home.getString("order",""))));
   for(String key:new String[]{"ride","nav","fuel","service"})json.put(key,home.getBoolean(key,true));
   json.put("navigator",NavigationApps.preferred(c).id);
   json.put("maintenanceNotifications",DailyMaintenanceJob.remindersEnabled(c));
   return json.toString();
  }catch(Exception failure){throw new IOException("홈·내비 설정 백업을 읽을 수 없습니다.",failure);}
 }
 static void validate(String input)throws IOException{
  if(input==null||input.isEmpty())return; // Old ZIPs have no UI snapshot.
  if(input.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>4096)
   throw new IOException("설정 백업 크기가 유효하지 않습니다.");
  try{
   JSONObject json=new JSONObject(input);
   if(json.getInt("schema")!=1)throw new IOException("지원하지 않는 설정 백업 버전입니다.");
   String order=json.getString("order");
   if(order.length()>40||!order.matches("(ride|nav|fuel|service)(,(ride|nav|fuel|service)){3}"))
    throw new IOException("홈 버튼 순서가 유효하지 않습니다.");
   java.util.HashSet<String> unique=new java.util.HashSet<>(java.util.Arrays.asList(order.split(",")));
   if(unique.size()!=4)throw new IOException("홈 버튼이 중복돼 있습니다.");
   int count=0;
   for(String key:new String[]{"ride","nav","fuel","service"})if(json.getBoolean(key))count++;
   if(count==0)throw new IOException("홈 버튼을 전부 숨긴 설정은 가져오지 않습니다.");
   String nav=json.getString("navigator");
   if(!nav.equals(NavigationChoice.KAKAO.id)&&
      !nav.equals(NavigationChoice.NAVER.id)&&!nav.equals(NavigationChoice.TMAP.id))
    throw new IOException("지원하지 않는 내비 설정입니다.");
   json.getBoolean("maintenanceNotifications");
  }catch(org.json.JSONException invalid){throw new IOException("설정 백업 JSON이 유효하지 않습니다.",invalid);}
 }
 private static boolean restoreKeys(SharedPreferences prefs,
   java.util.Map<String,?> previous,String... keys){
  SharedPreferences.Editor editor=prefs.edit();
  for(String key:keys){
   if(!previous.containsKey(key)){editor.remove(key);continue;}
   Object value=previous.get(key);
   if(value instanceof String)editor.putString(key,(String)value);
   else if(value instanceof Boolean)editor.putBoolean(key,(Boolean)value);
   else return false; // Never guess the type of an existing preference.
  }
  return editor.commit();
 }
 static void restore(Context c,String input)throws IOException{
  if(input==null||input.isEmpty())return;
  validate(input);
  final JSONObject json;
  final String order;
  final NavigationChoice requested;
  final boolean notices;
  final java.util.Map<String,Boolean> shortcuts=new java.util.LinkedHashMap<>();
  try{
   json=new JSONObject(input);
   order=json.getString("order");
   requested=NavigationChoice.fromId(json.getString("navigator"));
   notices=json.getBoolean("maintenanceNotifications");
   for(String key:new String[]{"ride","nav","fuel","service"})
    shortcuts.put(key,json.getBoolean(key));
  }catch(org.json.JSONException invalid){throw new IOException("설정 복원 중 JSON 오류",invalid);}
  SharedPreferences homePrefs=c.getSharedPreferences("home_shortcuts",0);
  SharedPreferences navPrefs=c.getSharedPreferences("navigation_choice",0);
  SharedPreferences notifyPrefs=c.getSharedPreferences("notifications",0);
  java.util.Map<String,?> oldHome=new java.util.HashMap<>(homePrefs.getAll());
  java.util.Map<String,?> oldNav=new java.util.HashMap<>(navPrefs.getAll());
  java.util.Map<String,?> oldNotice=new java.util.HashMap<>(notifyPrefs.getAll());
  // A restore from another phone may name a navigator not installed here.
  boolean navAvailable=NavigationApps.installedLaunchIntent(c,requested)!=null;
  try{
   SharedPreferences.Editor editor=homePrefs.edit().putString("order",order);
   for(java.util.Map.Entry<String,Boolean> option:shortcuts.entrySet())
    editor.putBoolean(option.getKey(),option.getValue());
   if(!editor.commit())throw new IOException("홈 버튼 배치 복원 실패");
   if(navAvailable&&!navPrefs.edit().putString("preferred",requested.id).commit())
    throw new IOException("기본 내비 복원 실패");
   if(!notifyPrefs.edit().putBoolean("maintenance_opt_in",notices).commit())
    throw new IOException("정비 알림 설정 복원 실패");
  }catch(Exception failure){
   // A commit returning false can still have changed in-memory values.
   // Undo only the allowlisted keys; unrelated local settings are untouched.
   boolean homeOk=restoreKeys(homePrefs,oldHome,"order","ride","nav","fuel","service");
   boolean navOk=restoreKeys(navPrefs,oldNav,"preferred");
   boolean notifyOk=restoreKeys(notifyPrefs,oldNotice,"maintenance_opt_in");
   IOException error=new IOException("기기 설정 일부를 저장하지 못해 이전 값을 복원했습니다.",failure);
   if(!homeOk||!navOk||!notifyOk)
    error.addSuppressed(new IOException("이전 설정 자동 복구도 일부 실패했습니다. 설정 화면에서 확인하세요."));
   throw error;
  }
  // These are follow-up jobs, not part of a committed preference transaction.
  try{DailyMaintenanceJob.schedule(c);}catch(RuntimeException ignored){}
  try{MotoWidgetProvider.refresh(c);}catch(RuntimeException ignored){}
 }
}
