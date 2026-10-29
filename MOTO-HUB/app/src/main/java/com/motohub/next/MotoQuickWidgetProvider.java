package com.motohub.next;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/** Minimal 2x1 widget with user-selected launch actions, no private ride or location status. */
public final class MotoQuickWidgetProvider extends AppWidgetProvider {
 @Override public void onAppWidgetOptionsChanged(Context context,AppWidgetManager manager,int id,android.os.Bundle options){
  super.onAppWidgetOptionsChanged(context,manager,id,options);
  onUpdate(context,manager,new int[]{id});
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
 @Override public void onUpdate(Context context,AppWidgetManager manager,int[] ids){
  for(int id:ids){
   RemoteViews views=new RemoteViews(context.getPackageName(),R.layout.moto_widget_quick);
   String first=QuickWidgetChoices.choice(context,0),second=QuickWidgetChoices.choice(context,1);
   views.setTextViewText(R.id.quick_ride,QuickWidgetChoices.label(first));
   views.setTextViewText(R.id.quick_nav,QuickWidgetChoices.label(second));
   views.setContentDescription(R.id.quick_ride,QuickWidgetChoices.label(first)+" 바로가기");
   views.setContentDescription(R.id.quick_nav,QuickWidgetChoices.label(second)+" 바로가기");
   views.setOnClickPendingIntent(R.id.quick_ride,launch(context,id,1,first));
   views.setOnClickPendingIntent(R.id.quick_nav,launch(context,id,2,second));
   manager.updateAppWidget(id,views);
  }
 }
}
