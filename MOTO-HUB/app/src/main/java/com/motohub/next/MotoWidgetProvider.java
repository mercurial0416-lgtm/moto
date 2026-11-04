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
  for(int id:ids)manager.updateAppWidget(id,build(context,compact(manager.getAppWidgetOptions(id))));
 }
 @Override public void onAppWidgetOptionsChanged(Context context,AppWidgetManager manager,int id,android.os.Bundle options){
  super.onAppWidgetOptionsChanged(context,manager,id,options);
  manager.updateAppWidget(id,build(context,compact(options)));
 }
 static boolean compact(android.os.Bundle options){
  int height=options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,0);
  return height>0&&height<124;
 }
 static RemoteViews build(Context context){return build(context,false);}
 static RemoteViews build(Context context,boolean compact){
  RemoteViews v=compact?new RemoteViews(context.getPackageName(),R.layout.moto_widget_compact):new RemoteViews(context.getPackageName(),R.layout.moto_widget);
  String bike=WidgetBikeSnapshot.label(context);
  boolean largeText=context.getResources().getConfiguration().fontScale>=1.3f;
  v.setTextViewText(R.id.widget_home,bike);
  v.setContentDescription(R.id.widget_home,bike+" · MOTO HUB 홈 열기");
  String navigator=NavigationApps.preferred(context).label;
  v.setTextViewText(R.id.widget_nav_choice,largeText?context.getString(R.string.widget_choose_nav_short):"내비 · "+navigator+"  ›");
  v.setContentDescription(R.id.widget_nav_choice,"현재 "+navigator+" · 기본 내비 선택 변경");
  // A two-cell launcher row has a fixed height. Give enlarged text the space
  // normally used by decorative icons; spoken labels keep the full action.
  if(largeText){
   v.setTextViewText(R.id.widget_ride,context.getString(R.string.widget_ride_short));
   v.setTextViewText(R.id.widget_nav,context.getString(R.string.widget_nav_short));
  }
  v.setTextViewCompoundDrawables(R.id.widget_ride,largeText?0:R.drawable.ic_widget_ride_primary,0,0,0);
  v.setTextViewCompoundDrawables(R.id.widget_nav,largeText?0:R.drawable.ic_widget_nav_secondary,0,0,0);
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
  if(ids.length>0)for(int id:ids)manager.updateAppWidget(id,build(context,compact(manager.getAppWidgetOptions(id))));
  lastWidgetIds=ids.clone();
  lastNavigationChoice=navigator;
  lastBikeLabel=bike;
 }
}
