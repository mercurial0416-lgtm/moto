package com.motohub.next;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,qualifiers="ko-rKR-w320dp-h852dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public final class AccessibleNavigationTest {
 static void configure(MainActivity a,int width,float scale){
  RuntimeEnvironment.setQualifiers("ko-rKR-w"+width+"dp-h852dp-xhdpi");
  Configuration c=new Configuration(a.getResources().getConfiguration());
  c.fontScale=scale;
  a.getResources().updateConfiguration(c,a.getResources().getDisplayMetrics());
 }
 static void layout(MainActivity a,View host,int width){
  host.measure(View.MeasureSpec.makeMeasureSpec(a.dp(width),View.MeasureSpec.EXACTLY),
   View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
  host.layout(0,0,host.getMeasuredWidth(),host.getMeasuredHeight());
 }
 @Test public void narrowNavigationPreservesLabelsTouchTargetsAndSpokenDestinations()throws Exception{
  MainActivity a=Robolectric.buildActivity(MainActivity.class).setup().get();
  try{
   String before=a.db.toString();
   for(int width:new int[]{320,393})for(float scale:new float[]{1f,1.3f,1.5f,2f}){
    configure(a,width,scale);
    LinearLayout host=a.col();MotoDesign.addNavigation(a,host);layout(a,host,width);
    LinearLayout nav=(LinearLayout)host.getChildAt(1);
    int height=nav.getChildAt(0).getHeight();
    for(int i=0;i<4;i++){
     LinearLayout cell=(LinearLayout)nav.getChildAt(i);
     TextView name=(TextView)cell.getChildAt(1);
     assertTrue("minimum touch height",cell.getHeight()>=a.dp(48));
     assertEquals("all tabs stay aligned",height,cell.getHeight());
     assertTrue("no three-line tab labels",name.getLineCount()<=2);
     assertTrue("entire label fits vertically",name.getLayout().getHeight()<=name.getHeight());
     for(int line=0;line<name.getLineCount();line++)assertEquals(0,name.getLayout().getEllipsisCount(line));
     assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO,name.getImportantForAccessibility());
    }
    assertEquals("기록",((TextView)((LinearLayout)nav.getChildAt(2)).getChildAt(1)).getText().toString());
    assertEquals("정비·주유 탭 열기",nav.getChildAt(2).getContentDescription().toString());
    File dir=new File("build/reports/ui-review");dir.mkdirs();
    Bitmap image=Bitmap.createBitmap(host.getWidth(),host.getHeight(),Bitmap.Config.ARGB_8888);
    host.draw(new Canvas(image));
    try(FileOutputStream out=new FileOutputStream(new File(dir,"navigation-"+width+"-"+scale+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,out);}
    image.recycle();
   }
   assertEquals(before,a.db.toString());
   configure(a,320,1.5f);a.render();
   Ux106NativeUiTest.desc(a.root,"정비·주유 탭 열기").performClick();
   assertEquals("차고",a.tab);
   assertNotNull(Ux106NativeUiTest.text(a.root,"정비 · 주유"));
  }finally{a.finish();}
 }
 @Test public void pairedActionsStackWithoutLosingHandlersOrData(){
  MainActivity a=Robolectric.buildActivity(MainActivity.class).setup().get();
  try{
   String before=a.db.toString();
   for(int width:new int[]{320,393})for(float scale:new float[]{1f,1.5f,2f}){
    configure(a,width,scale);
    LinearLayout host=a.col();int[] clicked={0,0};
    a.pair(host,"간편 주유",()->clicked[0]++,"정비주기 설정",()->clicked[1]++);
    layout(a,host,width);
    LinearLayout row=(LinearLayout)host.getChildAt(0);
    boolean stacked=width<360||scale>=1.3f;
    assertEquals(stacked?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL,row.getOrientation());
    for(int i=0;i<2;i++){
     TextView button=(TextView)row.getChildAt(i);
     assertTrue(button.getHeight()>=a.dp(48));
     assertTrue(button.getLayout().getHeight()<=button.getHeight()-button.getCompoundPaddingTop()-button.getCompoundPaddingBottom());
     if(stacked)assertEquals(row.getWidth(),button.getWidth());
     button.performClick();assertEquals(1,clicked[i]);
    }
   }
   assertEquals(before,a.db.toString());
  }finally{a.finish();}
 }
 @Test public void recordToolsAnnounceTheActionMatchingTheirVisibility()throws Exception{
  MainActivity a=Robolectric.buildActivity(MainActivity.class).setup().get();
  try{
   String id="11111111-2222-4333-8444-555555555555";
   a.arr("vehicles").put(new JSONObject().put("id",id).put("name","Test bike").put("odo",100));a.selected=id;
   a.navigate("차고");String before=a.db.toString();
   TextView reveal=Ux106NativeUiTest.text(a.root,"상세 입력·영수증 도구 보기");
   View details=(View)Ux106NativeUiTest.text(a.root,"정비 완료 내역 상세 입력").getParent();
   assertEquals(View.GONE,details.getVisibility());
   reveal.performClick();assertEquals(View.VISIBLE,details.getVisibility());
   assertEquals("상세 입력·영수증 도구 닫기",reveal.getText().toString());
   assertTrue(reveal.getContentDescription().toString().endsWith("접기"));
   reveal.performClick();assertEquals(View.GONE,details.getVisibility());
   assertEquals("상세 입력·영수증 도구 보기",reveal.getText().toString());
   assertTrue(reveal.getContentDescription().toString().endsWith("펼치기"));
   assertEquals(before,a.db.toString());
  }finally{a.finish();}
 }
}
