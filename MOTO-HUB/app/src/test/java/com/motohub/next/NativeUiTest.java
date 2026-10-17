package com.motohub.next;
import org.junit.*;import org.junit.runner.RunWith;import static org.junit.Assert.*;
import org.robolectric.*;import org.robolectric.annotation.*;import org.robolectric.shadows.ShadowAlertDialog;
import android.app.*;import android.os.*;import android.view.*;import android.widget.*;import android.graphics.*;import org.json.*;import java.io.*;import java.nio.charset.StandardCharsets;import java.time.Duration;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35,qualifiers="ko-rKR-w393dp-h852dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class NativeUiTest {
 MainActivity a;
 @Before public void start(){a=Robolectric.buildActivity(MainActivity.class).setup().get();}
 @After public void finish(){if(a!=null){a.close();a.finish();}}
 View find(View v,String text){if(v instanceof Button&&((Button)v).getText().toString().equals(text))return v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){View r=find(g.getChildAt(i),text);if(r!=null)return r;}}return null;}
 void save(){View b=find(a.dialog.getWindow().getDecorView(),"저장");assertNotNull(b);b.performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();}
 void field(String name,String value){a.draftFields.get(name).setText(value);}
 void bike(){a.edit("vehicles",null);field("name","TEST · My Bike");field("odo","12450");save();assertEquals(1,a.arr("vehicles").length());}
 @Test public void emptyLaunchAndNavigation(){assertEquals(0,a.arr("vehicles").length());for(String tab:a.tabs){a.navigate(tab);assertTrue(a.content.getChildCount()>0);}a.navigate("더보기");a.back();assertEquals("지도",a.tab);a.back();assertEquals("홈",a.tab);}
 @Test public void formsPersistAcrossActivityRestart()throws Exception{bike();a.edit("services",null);field("cost","55000");field("nextKm","15000");save();a.edit("expenses",null);field("amount","23000");field("liters","11");a.fullTankBox.setChecked(true);save();a.edit("listings",null);field("model","TEST · Saved motorcycle");field("price","5300000");save();assertEquals(1,a.arr("services").length());assertTrue(a.arr("expenses").getJSONObject(0).getBoolean("fullTank"));a.finish();a=Robolectric.buildActivity(MainActivity.class).setup().get();assertEquals(1,a.arr("listings").length());assertEquals(1,a.arr("services").length());assertEquals(1,a.arr("expenses").length());}
 @Test public void invalidPriceDoesNotSave(){a.edit("listings",null);field("model","TEST invalid");field("price","0");save();assertEquals(0,a.arr("listings").length());assertTrue(a.dialog.isShowing());}
 @Test public void draftSurvivesFormClose(){bike();a.edit("services",null);field("cost","77000");a.close();assertTrue(a.getSharedPreferences("draft",0).contains("active"));a.resumeDraft();assertEquals("77000",a.draftFields.get("cost").getText().toString());assertEquals(0,a.arr("services").length());}
 @Test public void backupRestoreAndRollback()throws Exception{bike();String original=a.db.toString();a.confirmImport(Store.empty());ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals(0,a.arr("vehicles").length());String rollback=new String(new android.util.AtomicFile(new File(a.getFilesDir(),"before-import.json")).readFully(),StandardCharsets.UTF_8);assertEquals(Store.parse(original).toString(),Store.parse(rollback).toString());a.confirmImport(Store.parse(rollback));ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals(1,a.arr("vehicles").length());}
 @Test public void compareOpensInOneAction()throws Exception{JSONObject x=new JSONObject().put("id","listing-test").put("model","TEST comparison").put("price",5000000);a.saveItem("listings",x);a.compare.add("listing-test");a.navigate("매물");find(a.root,"선택 비교").performClick();assertTrue(a.dialog.isShowing());}
 @Test public void calculatorAndBack(){a.tradeIn();assertTrue(a.dialog.isShowing());a.back();assertNull(a.dialog);}
 @Test public void nativeLayoutPreviews()throws Exception{
  bike();a.edit("services",null);field("cost","55000");field("nextKm","13000");save();a.edit("listings",null);field("model","TEST · DUKE 390");field("price","5300000");field("region","테스트 지역");save();
  File dir=new File(System.getProperty("user.dir"),"../evidence/native-previews");dir.mkdirs();
  for(int width:new int[]{320,393})for(String theme:new String[]{"light","dark"}){
   RuntimeEnvironment.setQualifiers("ko-rKR-w"+width+"dp-h852dp-xhdpi");android.content.res.Configuration cfg=new android.content.res.Configuration(a.getResources().getConfiguration());cfg.fontScale=width==320?1.3f:1f;a.getResources().updateConfiguration(cfg,a.getResources().getDisplayMetrics());a.settings().put("theme",theme);
   for(String tab:new String[]{"홈","차고","매물","라이딩","지도"}){a.tab=tab;a.render();Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(250));a.content.animate().cancel();a.content.setAlpha(1f);View view=a.root;int w=a.dp(width),h=a.dp(852);view.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));view.layout(0,0,w,h);assertTrue(a.content.getWidth()>0);Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);view.draw(new Canvas(bitmap));try(FileOutputStream out=new FileOutputStream(new File(dir,width+"-"+theme+"-"+tab+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();}
  }
 }
}
