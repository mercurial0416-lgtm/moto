package com.motohub.next;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/** Configurable 2x2 action pad with a compact 2x1 fallback and no ride/location status. */
public final class MotoQuickWidgetProvider extends AppWidgetProvider {
 @Override public void onAppWidgetOptionsChanged(Context context,AppWidgetManager manager,int id,android.os.Bundle options){
  super.onAppWidgetOptionsChanged(context,manager,id,options);
  manager.updateAppWidget(id,build(context,id,compact(options)));
 }
 static boolean compact(android.os.Bundle options){
  int height=options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,0);
  return (height>0&&height<120);
 }
 private static PendingIntent launch(Context context,int widgetId,int slot,String action){
  Intent open=new Intent(context,MainActivity.class)
   .setAction("com.motohub.next.QUICK_"+widgetId+"_"+slot+"_"+action)
   .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
  switch(action){
   case "fuel":open.putExtra(MotoWidgetProvider.EXTRA_FORM,"fuel_quick");break;
   case "nav":open.putExtra(MotoWidgetProvider.EXTRA_NAVIGATION,true);break;
   default:
    open.putExtra(MotoWidgetProvider.EXTRA_TAB,"라이딩");
    open.putExtra(MotoWidgetProvider.EXTRA_ONE_TAP_RIDE,true);
  }
  return PendingIntent.getActivity(context,widgetId*10+slot,open,
   PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
 }
 static String visualLabel(String action){return "fuel".equals(action)?"주유":"nav".equals(action)?"내비":"출발";}
 static int icon(String action,boolean primary){
  if("fuel".equals(action))return primary?R.drawable.ic_widget_fuel_primary:R.drawable.ic_widget_fuel_secondary;
  if("nav".equals(action))return primary?R.drawable.ic_widget_nav_primary:R.drawable.ic_widget_nav_secondary;
  return primary?R.drawable.ic_widget_ride_primary:R.drawable.ic_widget_ride_secondary;
 }
 static RemoteViews build(Context context,int id,boolean compact){
   RemoteViews views=new RemoteViews(context.getPackageName(),compact?R.layout.moto_widget_quick_compact:R.layout.moto_widget_quick);
   String first=QuickWidgetChoices.choice(context,0),second=QuickWidgetChoices.choice(context,1);
   String bike=WidgetBikeSnapshot.label(context);
   boolean largeText=context.getResources().getConfiguration().fontScale>=1.3f;
   views.setTextViewText(R.id.quick_ride,visualLabel(first));
   views.setTextViewCompoundDrawables(R.id.quick_ride,!largeText&&!compact?icon(first,true):0,!largeText&&compact?icon(first,true):0,0,0);
   views.setTextViewText(R.id.quick_nav,visualLabel(second));
   views.setTextViewCompoundDrawables(R.id.quick_nav,!largeText&&!compact?icon(second,false):0,!largeText&&compact?icon(second,false):0,0,0);
   views.setContentDescription(R.id.quick_ride,bike+" · "+QuickWidgetChoices.label(first)+" 바로가기");
   views.setContentDescription(R.id.quick_nav,bike+" · "+QuickWidgetChoices.label(second)+" 바로가기");
   views.setOnClickPendingIntent(R.id.quick_ride,launch(context,id,1,first));
   views.setOnClickPendingIntent(R.id.quick_nav,launch(context,id,2,second));return views;
 }
 @Override public void onUpdate(Context context,AppWidgetManager manager,int[] ids){
  for(int id:ids){
   manager.updateAppWidget(id,build(context,id,compact(manager.getAppWidgetOptions(id))));
  }
 }
}
