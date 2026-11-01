package com.motohub.next;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** v569 navigation and controls: test the rendered native views, not only strings. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,qualifiers="ko-rKR-w393dp-h852dp-xhdpi")
public final class Ux106NativeUiTest {
 MainActivity activity;
 @Before public void setup(){activity=Robolectric.buildActivity(MainActivity.class).setup().get();}
 @After public void cleanup(){if(activity!=null)activity.finish();}

 static View desc(View root,String label){
  CharSequence value=root.getContentDescription();
  if(value!=null&&value.toString().equals(label))return root;
  if(root instanceof ViewGroup){
   ViewGroup group=(ViewGroup)root;
   for(int i=0;i<group.getChildCount();i++){
    View result=desc(group.getChildAt(i),label);
    if(result!=null)return result;
   }
  }
  return null;
 }
 static TextView text(View root,String label){
  if(root instanceof TextView&&label.contentEquals(((TextView)root).getText()))return (TextView)root;
  if(root instanceof ViewGroup){
   ViewGroup group=(ViewGroup)root;
   for(int i=0;i<group.getChildCount();i++){
    TextView result=text(group.getChildAt(i),label);
    if(result!=null)return result;
   }
  }
  return null;
 }
 @Test public void brandedNavigationHasFullClickableDestinations(){
  assertNotNull(text(activity.root,"MOTO HUB"));
  assertNotNull(desc(activity.root,"홈, 현재 탭"));
  for(String label:new String[]{"라이딩","정비","설정"}){
   View item=desc(activity.root,label+" 탭 열기");
   assertNotNull(label+" bottom tab",item);
   assertTrue(label+" is clickable",item.isClickable());
   assertTrue(label+" >= 62dp target",item.getMinimumHeight()>=activity.dp(62));
  }
  View ride=desc(activity.root,"라이딩 탭 열기");
  assertNotNull(ride);ride.performClick();
  assertEquals("라이딩",activity.tab);
  assertNotNull(desc(activity.root,"라이딩, 현재 탭"));
  View settings=desc(activity.root,"설정 탭 열기");
  assertNotNull(settings);settings.performClick();
  assertEquals("더보기",activity.tab);
  assertNotNull(desc(activity.root,"설정, 현재 탭"));
 }
 @Test public void settingsCardsRetainClickableActions(){
  activity.navigate("더보기");
  View backup=desc(activity.root,"백업 · 복원, GPS 백업 상태를 백그라운드에서 검사합니다, 열기");
  assertNotNull("data backup settings row",backup);
  assertTrue(backup.isClickable());
  assertTrue(backup.getMinimumHeight()>=activity.dp(76));
  assertNotNull(text(activity.root,"앱 업데이트"));
 }
}
