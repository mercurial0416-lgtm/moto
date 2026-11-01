package com.motohub.next;
import android.app.*;import android.os.*;import android.content.*;import android.view.*;import android.widget.*;import android.graphics.Bitmap;import org.json.*;import java.io.*;
/** Run on an empty emulator only. Fixtures are labelled TEST and never shipped in the app. */
public class SmokeRunner extends Instrumentation {
 String mode=""; MainActivity a; int passed=0; StringBuilder log=new StringBuilder();
 interface Job {void run() throws Exception;}
 void ui(Job j){final Throwable[] e={null};runOnMainSync(()->{try{j.run();}catch(Throwable t){e[0]=t;}});if(e[0]!=null)throw new RuntimeException(e[0]);waitForIdleSync();}
 void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);log.append("PASS ").append(msg).append('\n');passed++;}
 View find(View v,String s){if(v instanceof Button&&((Button)v).getText().toString().equals(s))return v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){View x=find(g.getChildAt(i),s);if(x!=null)return x;}}return null;}
 void save(){View v=find(a.dialog.getWindow().getDecorView(),"저장");if(v==null)throw new AssertionError("save missing");v.performClick();}
 void set(String key,String value){a.draftFields.get(key).setText(value);}
 @Override public void onCreate(Bundle b){super.onCreate(b);mode=b==null?"":b.getString("mode","");start();}
 @Override public void onStart(){Bundle result=new Bundle();try{
  a=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
  if(mode.equals("screens")){
   for(String tab:a.tabs){ui(()->{a.tab=tab;a.render();});Thread.sleep(1000);Bitmap shot=getUiAutomation().takeScreenshot();if(shot==null)throw new IOException("screenshot unavailable");try(FileOutputStream f=getTargetContext().openFileOutput("screen-"+tab+".png",0)){shot.compress(Bitmap.CompressFormat.PNG,100,f);}shot.recycle();}
   result.putString("stream","Screenshots captured from emulator\n");finish(Activity.RESULT_OK,result);return;
  }
  if(mode.equals("pdf")){
   // Real Android PdfDocument requires a device graphics backend. Unlike
   // Robolectric unit tests, this verifies actual on-device PDF bytes.
   JSONObject sample=Store.empty();
   String id="22222222-3333-4444-8555-666666666666";
   sample.getJSONArray("vehicles").put(new JSONObject().put("id",id)
    .put("name","TEST DUKE").put("year",2024).put("odo",1200));
   sample.getJSONArray("services").put(new JSONObject().put("id","test-service")
    .put("vehicleId",id).put("type","엔진오일").put("date","2026-10-30")
    .put("cost",50000).put("mileage",1200));
   MotoOwnershipReport.Report doc=MotoOwnershipReport.prepare(Store.parse(sample.toString()),id);
   ByteArrayOutputStream buffer=new ByteArrayOutputStream();
   int pages=MotoOwnershipReport.write(doc,buffer);
   check(pages>=1,"PDF has at least one page");
   check(buffer.size()>120,"PDF is not empty");
   check("%PDF".equals(new String(buffer.toByteArray(),0,4,java.nio.charset.StandardCharsets.US_ASCII)),
    "PDF header is valid on Android");
   check(doc.completedServiceCount==1&&doc.totalCount==1,"PDF is vehicle-scoped");
   result.putString("stream",log+"PDF DEVICE SMOKE TOTAL "+passed+" passed\\n");
   finish(Activity.RESULT_OK,result);return;
  }
  ui(()->{check(a.arr("vehicles").length()==0,"empty emulator required");a.edit("vehicles",null);set("name","TEST Bike");set("odo","12000");save();check(a.arr("vehicles").length()==1,"bike form save");});
  ui(()->{a.edit("services",null);set("cost","50000");set("nextKm","15000");save();check(a.arr("services").length()==1,"maintenance form save");});
  ui(()->{a.edit("expenses",null);set("amount","25000");set("liters","12");a.fullTankBox.setChecked(true);save();check(a.arr("expenses").getJSONObject(0).getBoolean("fullTank"),"fuel full tank save");});
  ui(()->{a.edit("listings",null);set("model","TEST Listing");set("price","5000000");save();check(a.arr("listings").length()==1,"listing form save");});
  ui(()->{check(Store.parse(a.db.toString()).getJSONArray("services").length()==1,"backup JSON round trip");check(Store.parse(new String(a.disk.readFully(),java.nio.charset.StandardCharsets.UTF_8)).getJSONArray("expenses").length()==1,"disk read after save");});
  ui(()->{a.tradeIn();check(a.dialog.isShowing(),"calculator opens");a.back();check(a.dialog==null,"form back closes");});
  for(String tab:a.tabs){ui(()->{a.tab=tab;a.render();check(a.content.getChildCount()>0,"tab "+tab);});}
  ui(()->{a.back();check(a.tab.equals("홈"),"tab back returns home");});
  ui(()->{a.finish();});a=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
  ui(()->{check(a.arr("vehicles").length()==1&&a.arr("listings").length()==1&&a.arr("services").length()==1,"activity restart preserves records");});
  result.putString("stream",log+"TOTAL "+passed+" passed\n");finish(Activity.RESULT_OK,result);
 }catch(Throwable t){result.putString("stream",log+"FAIL "+android.util.Log.getStackTraceString(t));finish(Activity.RESULT_CANCELED,result);}}
}
