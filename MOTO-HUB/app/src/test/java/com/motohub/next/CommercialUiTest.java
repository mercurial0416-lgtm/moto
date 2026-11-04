package com.motohub.next;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import java.io.File;
import java.io.FileOutputStream;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,qualifiers="ko-rKR-w393dp-h852dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public final class CommercialUiTest {
 static double luminance(int color){
  double result=0;double[] weights={.2126,.7152,.0722};
  for(int i=0;i<3;i++){
   double c=((color>>(16-i*8))&255)/255.0;
   result+=weights[i]*(c<=.04045?c/12.92:Math.pow((c+.055)/1.055,2.4));
  }return result;
 }
 static double contrast(int x,int y){double a=luminance(x),b=luminance(y);return (Math.max(a,b)+.05)/(Math.min(a,b)+.05);}
 @Test public void textAndPrimaryButtonsMeetNormalTextContrast(){
  for(boolean dark:new boolean[]{false,true}){
   int[] c=MotoDesign.colors(dark);
   assertTrue(contrast(c[2],c[0])>=4.5);
   assertTrue(contrast(c[2],c[5])>=4.5);
   assertTrue(contrast(MotoDesign.onAccent(dark),c[4])>=4.5);
  }
 }
 @Test public void homeActionsAndResponsiveScreens()throws Exception{
  MainActivity a=Robolectric.buildActivity(MainActivity.class).setup().get();
  try{
   JSONObject bike=new JSONObject().put("id","11111111-2222-4333-8444-555555555555")
    .put("name","KTM 390 DUKE").put("year",2024).put("odo",12480);
   a.arr("vehicles").put(bike);a.selected=bike.getString("id");
   a.arr("expenses").put(new JSONObject().put("id","fuel-test").put("vehicleId",a.selected)
    .put("type","주유").put("date","2026-11-04").put("amount",23500));
   a.arr("services").put(new JSONObject().put("id","service-test").put("vehicleId",a.selected)
    .put("type","엔진오일 교환").put("date","2026-11-01").put("cost",80000));
   for(String theme:new String[]{"light","dark"}){
    a.settings().put("theme",theme);a.tab="홈";a.render();
    View change=Ux106NativeUiTest.desc(a.root,"기본 내비게이션 앱 변경");
    assertNotNull(change);assertTrue(change.isClickable());assertTrue(((TextView)change).getMinHeight()>=a.dp(48));
    TextView ride=Ux106NativeUiTest.text(a.root,"라이딩 시작");assertNotNull(ride);
    assertEquals(MotoDesign.onAccent(a.dark),ride.getCurrentTextColor());
    assertNull(Ux106NativeUiTest.text(a.root,"내 기록 · 코스 · 기타 도구  ›"));
    assertNull(Ux106NativeUiTest.text(a.root,"유지비 요약 보기  ›"));
    assertNull(Ux106NativeUiTest.text(a.root,"최근 라이딩 저장 요약  ›"));
    capture(a,"home-"+theme,393,852);
   }
   android.content.res.Configuration config=new android.content.res.Configuration(a.getResources().getConfiguration());
   config.fontScale=1.5f;config.screenWidthDp=320;
   a.getResources().updateConfiguration(config,a.getResources().getDisplayMetrics());
   a.render();capture(a,"home-large-text",320,852);
   TextView amount=Ux106NativeUiTest.text(a.root,"23,500원  ›");assertNotNull(amount);
   assertTrue(((android.widget.LinearLayout)amount.getParent()).getOrientation()==android.widget.LinearLayout.VERTICAL);
   a.navigate("차고");capture(a,"garage-large-text",320,852);
   a.navigate("더보기");capture(a,"settings-large-text",320,852);
   String unchanged=a.db.toString();
   new HubScreens(a).advancedSettings();
   View tools=a.dialog.getWindow().getDecorView();
   assertNotNull(Ux106NativeUiTest.text(tools,"홈 화면 위젯"));
   assertNotNull(Ux106NativeUiTest.text(tools,"주차 위치"));
   assertNotNull(Ux106NativeUiTest.text(tools,"유지비 통계"));
   assertNull(Ux106NativeUiTest.text(tools,"선택한 내비 실행"));
   assertNull(Ux106NativeUiTest.text(tools,"바이크별 통합 활동"));
   assertNull(Ux106NativeUiTest.text(tools,"바이크 판매용 정비 이력 PDF"));
   assertEquals(unchanged,a.db.toString());a.close();
  }finally{a.finish();}
 }
 static void capture(MainActivity a,String name,int widthDp,int heightDp)throws Exception{
  int width=a.dp(widthDp),height=a.dp(heightDp);
  a.root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));
  a.root.layout(0,0,width,height);
  Bitmap bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
  a.root.draw(new Canvas(bitmap));
  File dir=new File("build/reports/ui-review");dir.mkdirs();
  try(FileOutputStream out=new FileOutputStream(new File(dir,name+".png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}
  bitmap.recycle();
 }
}
