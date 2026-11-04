package com.motohub.next;

import android.app.Application;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,qualifiers="ko-rKR-w393dp-h852dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public final class RedesignWidgetTest {
 int px(Context c,int dp){return Math.round(dp*c.getResources().getDisplayMetrics().density);}
 void capture(Context c,View v,String name,int width,int height)throws Exception{
  int w=px(c,width),h=px(c,height);v.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));v.layout(0,0,w,h);
  Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);v.draw(new Canvas(b));
  File dir=new File("build/reports/ui-review");dir.mkdirs();
  try(FileOutputStream out=new FileOutputStream(new File(dir,name+".png"))){assertTrue(b.compress(Bitmap.CompressFormat.PNG,100,out));}b.recycle();
 }
 @Test public void remoteWidgetsHaveNightAndDayContrastAndBoundedTouchControls()throws Exception{
  Context c=RuntimeEnvironment.getApplication();
  c.getSharedPreferences("widget_bike",0).edit().putString("selected_bike_label","KTM 390 DUKE").commit();
  for(String mode:new String[]{"notnight","night"}){
   RuntimeEnvironment.setQualifiers("ko-rKR-w393dp-h852dp-"+mode+"-xhdpi");
   assertTrue(CommercialUiTest.contrast(c.getColor(R.color.widget_text),c.getColor(R.color.widget_surface))>=4.5);
   assertTrue(CommercialUiTest.contrast(c.getColor(R.color.widget_on_primary),c.getColor(R.color.widget_primary))>=4.5);
   View full=MotoWidgetProvider.build(c).apply(c,new FrameLayout(c));capture(c,full,"widget-full-"+mode,340,132);
   for(int id:new int[]{R.id.widget_home,R.id.widget_nav_choice,R.id.widget_ride,R.id.widget_nav}){
    View button=full.findViewById(id);assertNotNull(button);assertTrue(button.isClickable());assertTrue(button.getHeight()>=px(c,48));
   }
   View compact=MotoWidgetProvider.build(c,true).apply(c,new FrameLayout(c));capture(c,compact,"widget-compact-"+mode,300,72);
   assertTrue(compact.findViewById(R.id.widget_ride).getHeight()>=px(c,48));
   View quick=MotoQuickWidgetProvider.build(c,71,false).apply(c,new FrameLayout(c));capture(c,quick,"widget-quick-"+mode,160,76);
   View small=MotoQuickWidgetProvider.build(c,71,true).apply(c,new FrameLayout(c));capture(c,small,"widget-quick-small-"+mode,140,56);
   for(int id:new int[]{R.id.quick_ride,R.id.quick_nav}){
    TextView button=quick.findViewById(id);assertNotNull(button.getCompoundDrawables()[1]);assertTrue(button.isClickable());assertTrue(button.getHeight()>=px(c,48));
    assertTrue(((TextView)small.findViewById(id)).getHeight()>=px(c,48));
   }
  }
 }
 @Test public void widgetActionsKeepConsentAndNavigationSeparate(){
  Context c=RuntimeEnvironment.getApplication();View root=MotoWidgetProvider.build(c).apply(c,new FrameLayout(c));
  root.findViewById(R.id.widget_ride).performClick();Intent ride=Shadows.shadowOf((Application)c.getApplicationContext()).getNextStartedActivity();
  assertNotNull(ride);assertTrue(ride.getBooleanExtra(MotoWidgetProvider.EXTRA_ONE_TAP_RIDE,false));assertFalse(ride.getBooleanExtra(MotoWidgetProvider.EXTRA_NAVIGATION,false));
  root.findViewById(R.id.widget_nav).performClick();Intent nav=Shadows.shadowOf((Application)c.getApplicationContext()).getNextStartedActivity();
  assertNotNull(nav);assertTrue(nav.getBooleanExtra(MotoWidgetProvider.EXTRA_NAVIGATION,false));assertFalse(nav.getBooleanExtra(MotoWidgetProvider.EXTRA_ONE_TAP_RIDE,false));
 }
 @Test public void resizingSelectsCompactLayoutWithoutChangingActions(){
  Bundle shortSize=new Bundle();shortSize.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,72);assertTrue(MotoWidgetProvider.compact(shortSize));
  shortSize.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,140);assertFalse(MotoWidgetProvider.compact(shortSize));
  assertFalse(MotoWidgetProvider.compact(new Bundle()));
  Bundle quickSize=new Bundle();quickSize.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,110);quickSize.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,80);
  assertTrue(MotoQuickWidgetProvider.compact(quickSize));quickSize.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,140);assertFalse(MotoQuickWidgetProvider.compact(quickSize));
  quickSize.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,56);assertTrue(MotoQuickWidgetProvider.compact(quickSize));
 }
 @Test public void largeWidgetTextFitsAtMinimumSizesWithoutTruncation()throws Exception{
  Context c=RuntimeEnvironment.getApplication();
  for(float scale:new float[]{1.3f,1.5f,2f}){
   Configuration config=new Configuration(c.getResources().getConfiguration());config.fontScale=scale;
   c.getResources().updateConfiguration(config,c.getResources().getDisplayMetrics());
   for(boolean compact:new boolean[]{false,true}){
    View full=MotoWidgetProvider.build(c,compact).apply(c,new FrameLayout(c));
    capture(c,full,"widget-large-font-"+scale+"-"+compact,220,compact?72:124);
    for(int id:new int[]{R.id.widget_ride,R.id.widget_nav})assertTextFits((TextView)full.findViewById(id));
    View quick=MotoQuickWidgetProvider.build(c,71,compact).apply(c,new FrameLayout(c));
    capture(c,quick,"widget-quick-large-font-"+scale+"-"+compact,compact?110:140,compact?56:76);
    for(int id:new int[]{R.id.quick_ride,R.id.quick_nav})assertTextFits((TextView)quick.findViewById(id));
   }
  }
 }
 void assertTextFits(TextView text){
  assertNotNull(text.getLayout());
  assertTrue(text.getText()+" is too tall",text.getLayout().getHeight()<=text.getHeight()-text.getCompoundPaddingTop()-text.getCompoundPaddingBottom());
  int width=text.getWidth()-text.getCompoundPaddingLeft()-text.getCompoundPaddingRight();
  for(int line=0;line<text.getLineCount();line++){
   assertEquals(0,text.getLayout().getEllipsisCount(line));
   assertTrue(text.getText()+" is too wide",text.getLayout().getLineWidth(line)<=width+1);
  }
 }
 @Test public void themedAdaptiveIconContainsANativeMonochromeLayer()throws Exception{
  Context c=RuntimeEnvironment.getApplication();Drawable icon=c.getDrawable(R.mipmap.ic_launcher);assertTrue(icon instanceof AdaptiveIconDrawable);assertNotNull(((AdaptiveIconDrawable)icon).getMonochrome());
  Bitmap b=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888);icon.setBounds(0,0,256,256);icon.draw(new Canvas(b));
  File dir=new File("build/reports/ui-review");dir.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(dir,"launcher-icon.png"))){assertTrue(b.compress(Bitmap.CompressFormat.PNG,100,out));}b.recycle();
 }
 @Test public void buildingWidgetsNeverReadsTheGarageDatabase(){
  Context c=new ContextWrapper(RuntimeEnvironment.getApplication()){@Override public File getFilesDir(){throw new AssertionError("widget must not read private app files");}};
  assertNotNull(MotoWidgetProvider.build(c));assertNotNull(MotoQuickWidgetProvider.build(c,1,true));
 }
 @Test public void quickWidgetChoicesAndInstancesRetainTheirOwnActions(){
  Context c=RuntimeEnvironment.getApplication();
  for(String choice:new String[]{"ride","nav","fuel"}){
   c.getSharedPreferences(QuickWidgetChoices.PREF,0).edit().putString("first",choice).commit();
   View root=MotoQuickWidgetProvider.build(c,51,false).apply(c,new FrameLayout(c));root.findViewById(R.id.quick_ride).performClick();
   Intent launched=Shadows.shadowOf((Application)c.getApplicationContext()).getNextStartedActivity();assertNotNull(launched);
   assertEquals("ride".equals(choice),launched.getBooleanExtra(MotoWidgetProvider.EXTRA_ONE_TAP_RIDE,false));
   assertEquals("nav".equals(choice),launched.getBooleanExtra(MotoWidgetProvider.EXTRA_NAVIGATION,false));
   if("fuel".equals(choice))assertEquals("fuel_quick",launched.getStringExtra(MotoWidgetProvider.EXTRA_FORM));
  }
  c.getSharedPreferences(QuickWidgetChoices.PREF,0).edit().putString("first","ride").commit();
  View first=MotoQuickWidgetProvider.build(c,71,true).apply(c,new FrameLayout(c));
  c.getSharedPreferences(QuickWidgetChoices.PREF,0).edit().putString("first","nav").commit();
  MotoQuickWidgetProvider.build(c,72,true).apply(c,new FrameLayout(c));
  first.findViewById(R.id.quick_ride).performClick();Intent original=Shadows.shadowOf((Application)c.getApplicationContext()).getNextStartedActivity();
  assertTrue(original.getBooleanExtra(MotoWidgetProvider.EXTRA_ONE_TAP_RIDE,false));assertTrue(original.getAction().contains("QUICK_71_1_ride"));
 }

}
