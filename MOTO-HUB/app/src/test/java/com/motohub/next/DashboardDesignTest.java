package com.motohub.next;

import android.content.res.Configuration;
import android.os.Looper;
import android.util.TypedValue;
import android.widget.TextView;
import java.time.Duration;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,qualifiers="ko-rKR-w393dp-h852dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public final class DashboardDesignTest {
 void seed(MainActivity a)throws Exception{
  String id="11111111-2222-4333-8444-555555555555";
  a.arr("vehicles").put(new JSONObject().put("id",id).put("name","KTM 390 DUKE").put("year",2024).put("odo",12480));a.selected=id;
  a.arr("services").put(new JSONObject().put("id","22222222-2222-4333-8444-555555555555").put("vehicleId",id).put("type","엔진오일 교환").put("date","2026-11-01").put("mileage",12000).put("nextKm",15000).put("cost",80000));
  a.arr("expenses").put(new JSONObject().put("id","33333333-2222-4333-8444-555555555555").put("vehicleId",id).put("type","주유").put("date","2026-11-04").put("mileage",12400).put("amount",23500).put("liters",12.5));
 }
 @Test public void allPrimaryScreensKeepDataAndNavigationAcrossThemesAndLargeText()throws Exception{
  MainActivity a=Robolectric.buildActivity(MainActivity.class).setup().get();
  try{
   seed(a);
   for(int width:new int[]{320,393})for(String theme:new String[]{"light","dark"}){
    RuntimeEnvironment.setQualifiers("ko-rKR-w"+width+"dp-h852dp-xhdpi");
    Configuration config=new Configuration(a.getResources().getConfiguration());config.fontScale=width==320?1.5f:1f;a.getResources().updateConfiguration(config,a.getResources().getDisplayMetrics());
    a.settings().put("theme",theme);String original=a.db.toString();
    String[] tabs={"홈","라이딩","차고","더보기"},names={"home","ride","records","settings"};
    for(int i=0;i<tabs.length;i++){
     a.tab=tabs[i];a.render();Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200));
     assertNotNull(Ux106NativeUiTest.text(a.root,MotoDesign.heading(tabs[i])));
     assertEquals(original,a.db.toString());
     CommercialUiTest.capture(a,"design-"+names[i]+"-"+theme+"-"+width,width,852);
    }
    a.tab="홈";a.render();
    assertNotNull(Ux106NativeUiTest.desc(a.root,"바이크 사진 변경"));
    assertNotNull(Ux106NativeUiTest.desc(a.root,"바이크 선택 또는 추가"));
    assertNotNull(Ux106NativeUiTest.desc(a.root,"주유 기록 열기"));
    assertNotNull(Ux106NativeUiTest.text(a.root,"라이딩 시작"));
    TypedValue accent=new TypedValue();assertTrue(a.getTheme().resolveAttribute(android.R.attr.colorAccent,accent,true));assertEquals(a.accent,accent.data);
   }
  }finally{a.close();a.finish();}
 }
 @Test public void redesignedEntryPageRetainsExistingFieldsAndCancellingDoesNotSave()throws Exception{
  MainActivity a=Robolectric.buildActivity(MainActivity.class).setup().get();
  try{
   seed(a);
   for(String theme:new String[]{"light","dark"}){
    a.settings().put("theme",theme);a.render();String original=a.db.toString();
    a.edit("expenses",a.arr("expenses").getJSONObject(0));assertNotNull(a.draftFields.get("amount"));assertFalse(a.draftFields.get("amount").getText().toString().isEmpty());
    TextView save=Ux106NativeUiTest.text(a.dialog.getWindow().getDecorView(),"저장");assertNotNull(save);
    assertTrue(save.isClickable());a.close();assertEquals(original,a.db.toString());
   }
  }finally{a.close();a.finish();}
 }
}
