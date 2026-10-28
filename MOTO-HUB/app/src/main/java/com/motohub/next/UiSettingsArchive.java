package com.motohub.next;
import android.content.Context;
import android.content.SharedPreferences;
import java.io.IOException;
import java.util.*;
import org.json.JSONObject;
/** Explicit allowlist of non-secret UI preferences for device migration. */
final class UiSettingsArchive {
 private UiSettingsArchive(){}
 static String capture(Context context)throws IOException {
  try{
   SharedPreferences home=context.getSharedPreferences("home_shortcuts",0);
   JSONObject shortcuts=new JSONObject().put("order",String.join(",",HomeShortcutOrder.parse(home.getString("order",""))));
   for(String key:new String[]{"ride","nav","fuel","service"})shortcuts.put(key,home.getBoolean(key,true));
   JSONObject root=new JSONObject().put("schema",1).put("shortcuts",shortcuts)
    .put("maintenanceReminders",context.getSharedPreferences("notifications",0)
      .getBoolean("maintenance_opt_in",false))
    .put("navigation",NavigationApps.preferred(context).id);
   return root.toString();
  }catch(Exception e){throw new IOException("화면 설정 백업 생성 실패",e);}
 }
 static void check(String json)throws IOException{
  if(json==null||json.length()>8192)throw new IOException("설정 백업 크기가 유효하지 않습니다.");
  try{
   JSONObject root=new JSONObject(json);
   if(root.getInt("schema")!=1)throw new IOException("지원하지 않는 설정 버전");
   JSONObject home=root.getJSONObject("shortcuts");
   String order=home.getString("order");
   if(!order.equals(String.join(",",HomeShortcutOrder.parse(order))))
    throw new IOException("홈 바로가기 순서가 손상됐습니다.");
   int visible=0;
   for(String key:new String[]{"ride","nav","fuel","service"})if(home.getBoolean(key))visible++;
   if(visible==0)throw new IOException("모든 홈 버튼을 숨기는 설정은 복원할 수 없습니다.");
   if(!root.has("maintenanceReminders")||!(root.get("maintenanceReminders") instanceof Boolean))
    throw new IOException("정비 알림 설정 형식 오류");
   String selected=root.getString("navigation");
   boolean known=false;for(NavigationChoice app:NavigationChoice.values())if(app.id.equals(selected))known=true;
   if(!known)throw new IOException("지원하지 않는 내비 앱 설정입니다.");
  }catch(IOException e){throw e;}
  catch(Exception e){throw new IOException("설정 백업이 손상됐습니다.",e);}
 }
 static void restore(Context context,String json)throws IOException{
  check(json);
  String previous=capture(context);
  try{
   apply(context,json);
  }catch(Exception failure){
   try{apply(context,previous);}
   catch(Exception rollback){failure.addSuppressed(rollback);}
   throw new IOException("설정 복원 중 오류 발생. 기존 설정을 되돌렸습니다.",failure);
  }
 }
 private static void apply(Context context,String json)throws Exception{
  JSONObject root=new JSONObject(json),home=root.getJSONObject("shortcuts");
  SharedPreferences.Editor shortcuts=context.getSharedPreferences("home_shortcuts",0).edit()
   .putString("order",home.getString("order"));
  for(String key:new String[]{"ride","nav","fuel","service"})shortcuts.putBoolean(key,home.getBoolean(key));
  if(!shortcuts.commit())throw new IOException("홈 설정 저장 실패");
  if(!context.getSharedPreferences("navigation_choice",0).edit()
    .putString("preferred",root.getString("navigation")).commit())throw new IOException("내비 설정 저장 실패");
  if(!context.getSharedPreferences("notifications",0).edit()
    .putBoolean("maintenance_opt_in",root.getBoolean("maintenanceReminders")).commit())
    throw new IOException("알림 설정 저장 실패");
  DailyMaintenanceJob.schedule(context);
  MotoWidgetProvider.refresh(context);
 }
}
