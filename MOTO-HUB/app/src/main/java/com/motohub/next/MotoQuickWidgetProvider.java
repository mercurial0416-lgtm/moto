package com.motohub.next;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/** Minimal 2x1 widget with 2 shortcuts and no private ride or location status. */
public final class MotoQuickWidgetProvider extends AppWidgetProvider {
 @Override public void onUpdate(Context context,AppWidgetManager manager,int[] ids){
  for(int id:ids){
   RemoteViews v=new RemoteViews(context.getPackageName(),R.layout.moto_widget_quick);
   Intent ride=new Intent(context,MainActivity.class)
      .setAction("com.motohub.next.QUICK_RIDE")
      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
   ride.putExtra(MotoWidgetProvider.EXTRA_TAB,"라이딩");
   ride.putExtra(MotoWidgetProvider.EXTRA_ONE_TAP_RIDE,true);
   Intent nav=new Intent(context,MainActivity.class)
      .setAction("com.motohub.next.QUICK_NAV")
      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
   nav.putExtra(MotoWidgetProvider.EXTRA_NAVIGATION,true);
   v.setOnClickPendingIntent(R.id.quick_ride,PendingIntent.getActivity(context,31,ride,
      PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
   v.setOnClickPendingIntent(R.id.quick_nav,PendingIntent.getActivity(context,32,nav,
      PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
   manager.updateAppWidget(id,v);
  }
 }
}
