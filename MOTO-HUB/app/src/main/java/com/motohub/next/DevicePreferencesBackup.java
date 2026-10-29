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
 static void restore(Context c,String input)throws IOException{
  if(input==null||input.isEmpty())return;
  validate(input);
  try{
   JSONObject json=new JSONObject(input);
   SharedPreferences.Editor home=c.getSharedPreferences("home_shortcuts",0).edit()
     .putString("order",json.getString("order"));
   for(String key:new String[]{"ride","nav","fuel","service"})home.putBoolean(key,json.getBoolean(key));
   if(!home.commit())throw new IOException("홈 버튼 배치 복원 실패");
   // Backups can come from another phone with different navigator apps.
   // Never replace a working default with an app that cannot be launched here.
   NavigationChoice requested=NavigationChoice.fromId(json.getString("navigator"));
   if(NavigationApps.installedLaunchIntent(c,requested)!=null){
    if(!c.getSharedPreferences("navigation_choice",0).edit()
      .putString("preferred",requested.id).commit())
     throw new IOException("기본 내비 복원 실패");
   }
   if(!c.getSharedPreferences("notifications",0).edit()
     .putBoolean("maintenance_opt_in",json.getBoolean("maintenanceNotifications")).commit())
    throw new IOException("정비 알림 설정 복원 실패");
   DailyMaintenanceJob.schedule(c);
   MotoWidgetProvider.refresh(c);
  }catch(org.json.JSONException invalid){throw new IOException("설정 복원 중 JSON 오류",invalid);}
 }
}
