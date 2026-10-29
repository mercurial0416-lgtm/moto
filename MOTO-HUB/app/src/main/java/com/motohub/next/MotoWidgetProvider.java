package com.motohub.next;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/**
 * Launcher is a shortcut, NOT a duplicate dashboard.
 * The user's current garage, GPS/OBD status and all ride details stay private in the app.
 * No JSON/AtomicFile reads or network/OBD calls occur in widget updates.
 */
public final class MotoWidgetProvider extends AppWidgetProvider {
 public static final String EXTRA_TAB="com.motohub.next.WIDGET_TAB";
 public static final String EXTRA_FORM="com.motohub.next.WIDGET_FORM";
 public static final String EXTRA_ONE_TAP_RIDE="com.motohub.next.WIDGET_ONE_TAP_RIDE";
 public static final String EXTRA_NAVIGATION="com.motohub.next.WIDGET_NAVIGATION";
 public static final String EXTRA_SELECT_NAV="com.motohub.next.WIDGET_SELECT_NAV";
 private static String lastNavigationChoice=null;
 private static String lastBikeLabel=null;
 private static int[] lastWidgetIds=null;
 @Override public void onUpdate(Context context,AppWidgetManager manager,int[] ids){
  for(int id:ids)manager.updateAppWidget(id,build(context));
 }
 @Override public void onAppWidgetOptionsChanged(Context context,AppWidgetManager manager,int id,android.os.Bundle options){
  super.onAppWidgetOptionsChanged(context,manager,id,options);
  manager.updateAppWidget(id,build(context));
 }
 static RemoteViews build(Context context){
  RemoteViews v=new RemoteViews(context.getPackageName(),R.layout.moto_widget);
  v.setTextViewText(R.id.widget_home,WidgetBikeSnapshot.label(context));
  v.setTextViewText(R.id.widget_nav_choice,"내비 · "+NavigationApps.preferred(context).label+"  ›");
  v.setOnClickPendingIntent(R.id.widget_home,pending(context,51,"HOME",EXTRA_TAB,"홈"));
  v.setOnClickPendingIntent(R.id.widget_nav_choice,pending(context,52,"CHOOSE_NAV",EXTRA_SELECT_NAV,true));
  v.setOnClickPendingIntent(R.id.widget_ride,pending(context,53,"RIDE_WITH_NAV",EXTRA_ONE_TAP_RIDE,true));
  v.setOnClickPendingIntent(R.id.widget_nav,pending(context,54,"NAV_ONLY",EXTRA_NAVIGATION,true));
  return v;
 }
 private static PendingIntent pending(Context context,int request,String suffix,String key,Object value){
  Intent i=new Intent(context,MainActivity.class);
  i.setAction("com.motohub.next.WIDGET_"+suffix);
  i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
  if(value instanceof Boolean)i.putExtra(key,(Boolean)value);
  else i.putExtra(key,String.valueOf(value));
  return PendingIntent.getActivity(context,request,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
 }
 /** Called after changing default navigator or upgrading; must remain light even during GPS recording. */
 public static synchronized void refresh(Context context){
  AppWidgetManager manager=AppWidgetManager.getInstance(context);
  int[] ids=manager.getAppWidgetIds(new ComponentName(context,MotoWidgetProvider.class));
  java.util.Arrays.sort(ids);
  String navigator=NavigationApps.preferred(context).id;
  String bike=WidgetBikeSnapshot.label(context);
  if(java.util.Arrays.equals(ids,lastWidgetIds)&&navigator.equals(lastNavigationChoice)&&bike.equals(lastBikeLabel))return;
  // Widgets contain shortcuts and navigator name only. GPS fixes and fuel/ride
  // database writes must not trigger repeated expensive launcher RemoteViews.
  if(ids.length>0)for(int id:ids)manager.updateAppWidget(id,build(context));
  lastWidgetIds=ids.clone();
  lastNavigationChoice=navigator;
  lastBikeLabel=bike;
 }
}
