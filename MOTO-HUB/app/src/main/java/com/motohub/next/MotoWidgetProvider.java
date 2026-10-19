package com.motohub.next;

import android.app.PendingIntent;
import android.os.Bundle;
import android.view.View;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.content.ComponentName;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.NumberFormat;
import java.util.Locale;
import java.time.LocalDate;


/** Home-screen shortcuts. Displays locally saved garage information on the launcher; no GPS coordinates are shown. */
public final class MotoWidgetProvider extends AppWidgetProvider {
 private static final java.util.concurrent.ExecutorService WIDGET_IO=java.util.concurrent.Executors.newSingleThreadExecutor(job->{
  Thread t=new Thread(job,"moto-widget-local-io");t.setDaemon(true);return t;
 });
 public static final String EXTRA_TAB="com.motohub.next.WIDGET_TAB";
 public static final String EXTRA_FORM="com.motohub.next.WIDGET_FORM";
 @Override public void onUpdate(Context context,AppWidgetManager manager,int[] ids){
  final PendingResult pending=goAsync();
  final Context app=context.getApplicationContext();
  WIDGET_IO.execute(()->{try{updateWidgets(app,manager,ids);}finally{pending.finish();}});
 }
 private static void updateWidgets(Context context,AppWidgetManager manager,int[] ids){
  for(int id:ids){
   RemoteViews views=new RemoteViews(context.getPackageName(),R.layout.moto_widget);
   renderGarage(context,views);
   applySize(manager,id,views);
   shortcut(context,views,R.id.widget_home,"홈",0);
   shortcut(context,views,R.id.widget_garage,"차고",1);
   shortcut(context,views,R.id.widget_ride,"라이딩",2);
   formShortcut(context,views,R.id.widget_fuel,"expenses",3);
   formShortcut(context,views,R.id.widget_service,"services",4);
   manager.updateAppWidget(id,views);
  }
 }
 @Override public void onAppWidgetOptionsChanged(Context context,AppWidgetManager manager,int widgetId,Bundle options){
  super.onAppWidgetOptionsChanged(context,manager,widgetId,options);
  onUpdate(context,manager,new int[]{widgetId});
 }
 private static void applySize(AppWidgetManager manager,int id,RemoteViews views){
  Bundle options=manager.getAppWidgetOptions(id);
  int width=options==null?250:options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,250);
  int height=options==null?110:options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,110);
  views.setViewVisibility(R.id.widget_bike_status,height>=95?View.VISIBLE:View.GONE);
  views.setViewVisibility(R.id.widget_cost,height>=155&&width>=180?View.VISIBLE:View.GONE);
  views.setViewVisibility(R.id.widget_quick_row,height>=205?View.VISIBLE:View.GONE);
  views.setViewVisibility(R.id.widget_home,width>=180?View.VISIBLE:View.GONE);
 }
 /** Refresh on a successful local save, never on an uncommitted draft. */
 public static void refresh(Context context){
  AppWidgetManager manager=AppWidgetManager.getInstance(context);
  ComponentName component=new ComponentName(context,MotoWidgetProvider.class);
  int[] ids=manager.getAppWidgetIds(component);
  if(ids.length>0){Context app=context.getApplicationContext();WIDGET_IO.execute(()->updateWidgets(app,manager,ids));}
 }
 private static void renderGarage(Context context,RemoteViews views){
  if(context.getSharedPreferences("widget_privacy",Context.MODE_PRIVATE).getBoolean("hide_details",false)){
   views.setTextViewText(R.id.widget_bike_name,"MOTO HUB");
   views.setTextViewText(R.id.widget_bike_status,"개인 기록 숨김 · 앱에서 확인");
   views.setTextViewText(R.id.widget_cost,"");return;
  }
  String headline="바이크를 등록해 주세요",detail="차고에서 차량을 추가할 수 있어요",cost="";
  try{
   java.io.File file=new java.io.File(context.getFilesDir(),"motohub-v3.json");
   if(file.exists()&&file.length()<8_000_000){
    byte[] bytes=java.nio.file.Files.readAllBytes(file.toPath());
    JSONObject db=Store.parse(new String(bytes,java.nio.charset.StandardCharsets.UTF_8));
    JSONArray bikes=db.optJSONArray("vehicles");
    if(bikes!=null&&bikes.length()>0){
     JSONObject settings=db.optJSONObject("settings");
     String primary=settings==null?"":settings.optString("primary");
     String selected=context.getSharedPreferences("garage_ui",Context.MODE_PRIVATE).getString("selected_bike","");
     JSONObject bike=Store.find(db,"vehicles",selected.isEmpty()?primary:selected);
     if(bike==null||selected.isEmpty()&&GarageBikeStatus.sold(bike.optString("soldDate"))){
      bike=null;for(int i=0;i<bikes.length();i++)if(!GarageBikeStatus.sold(bikes.getJSONObject(i).optString("soldDate"))){bike=bikes.getJSONObject(i);break;}
      if(bike==null)bike=bikes.getJSONObject(0);
     }
     headline=bike.optString("name","내 바이크");
     double odometer=bike.optDouble("odo",0);
     if(!Double.isFinite(odometer)||odometer<0)odometer=0;
     String km=NumberFormat.getIntegerInstance(Locale.KOREA).format(odometer);
     java.util.List<WidgetMaintenanceSummary.Service> history=new java.util.ArrayList<>();
     JSONArray services=db.optJSONArray("services");
     if(services!=null)for(int i=0;i<services.length();i++){
      JSONObject service=services.optJSONObject(i);
      if(service!=null&&bike.optString("id").equals(service.optString("vehicleId")))
       history.add(new WidgetMaintenanceSummary.Service(service.optString("type"),service.optString("date"),
        service.optString("nextDate"),service.optDouble("nextKm")));
     }
     detail=km+" km  ·  "+WidgetMaintenanceSummary.label(history,LocalDate.now().toString(),odometer);
     String month=LocalDate.now().toString().substring(0,7);
     double monthly=0;
     for(String collection:new String[]{"expenses","services"}){
      JSONArray entries=db.optJSONArray(collection);
      if(entries==null)continue;
      for(int j=0;j<entries.length();j++){
       JSONObject entry=entries.optJSONObject(j);
       if(entry!=null&&bike.optString("id").equals(entry.optString("vehicleId"))&&entry.optString("date").startsWith(month))
        {
         double amount=entry.optDouble(collection.equals("services")?"cost":"amount",0);
         if(Double.isFinite(amount)&&amount>=0&&amount<=1_000_000_000&&monthly<=1_000_000_000-amount)monthly+=amount;
        }
      }
     }
     cost="이번 달 유지비  "+NumberFormat.getIntegerInstance(Locale.KOREA).format(Math.round(monthly))+"원";
    }
   }
  }catch(Exception ignored){headline="MOTO HUB";detail="앱에서 차량 정보를 확인해 주세요";}
  views.setTextViewText(R.id.widget_bike_name,headline);
  views.setTextViewText(R.id.widget_bike_status,detail);
  views.setTextViewText(R.id.widget_cost,cost);
 }
 private static void formShortcut(Context context,RemoteViews views,int viewId,String form,int code){
  Intent intent=new Intent(context,MainActivity.class);
  intent.setAction("com.motohub.next.WIDGET_FORM_"+code);
  intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
  intent.putExtra(EXTRA_FORM,form);
  PendingIntent pending=PendingIntent.getActivity(context,code,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  views.setOnClickPendingIntent(viewId,pending);
 }
 private static void shortcut(Context context,RemoteViews views,int viewId,String tab,int code){
  Intent intent=new Intent(context,MainActivity.class);
  intent.setAction("com.motohub.next.WIDGET_OPEN_"+code);
  intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
  intent.putExtra(EXTRA_TAB,tab);
  PendingIntent pending=PendingIntent.getActivity(context,code,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  views.setOnClickPendingIntent(viewId,pending);
 }
}
