package com.motohub.next;

import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import org.json.JSONObject;

/** Launcher-only preferences; never store GPS/ride details in RemoteViews. */
final class QuickWidgetChoices {
 static final String PREF="quick_widget_choices";
 private static final String[] IDS={"ride","nav","fuel"};
 private static final String[] LABELS={"GPS + 내비","기본 내비","주유 기록"};
 private QuickWidgetChoices(){}
 static String normalize(String value){for(String id:IDS)if(id.equals(value))return id;return "ride";}
 static String choice(Context c,int slot){
  return normalize(c.getSharedPreferences(PREF,0).getString(slot==0?"first":"second",slot==0?"ride":"nav"));
 }
 static String label(String value){
  for(int i=0;i<IDS.length;i++)if(IDS[i].equals(value))return LABELS[i];return LABELS[0];
 }
 static void configure(MainActivity a){
  String[] options=LABELS;
  String first=choice(a,0),second=choice(a,1);
  new AlertDialog.Builder(a).setTitle("작은 위젯 · 왼쪽 버튼")
   .setSingleChoiceItems(options,indexOf(first),(d,idx)->{
    d.dismiss();
    String left=IDS[idx];
    new AlertDialog.Builder(a).setTitle("작은 위젯 · 오른쪽 버튼")
     .setSingleChoiceItems(options,indexOf(second),(dd,i)->{
      dd.dismiss();
      if(!a.getSharedPreferences(PREF,0).edit().putString("first",left)
        .putString("second",IDS[i]).commit()){
       a.toast("위젯 버튼 설정 저장 실패");return;
      }
      refresh(a);
      a.toast("작은 위젯 버튼이 변경됐어요.");
     }).setNegativeButton("취소",null).show();
   }).setNegativeButton("취소",null).show();
 }
 static int indexOf(String value){for(int i=0;i<IDS.length;i++)if(IDS[i].equals(value))return i;return 0;}
 static void refresh(Context context){
  AppWidgetManager manager=AppWidgetManager.getInstance(context);
  ComponentName component=new ComponentName(context,MotoQuickWidgetProvider.class);
  int[] ids=manager.getAppWidgetIds(component);
  if(ids.length>0)new MotoQuickWidgetProvider().onUpdate(context,manager,ids);
 }
}
