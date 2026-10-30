package com.motohub.next;

import android.content.Context;
import java.util.Collections;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE,sdk=28)
public final class UX92IntegrationTest {
 @Test public void backupWarningRecognizesRecordsChangedSinceLastFullZip()throws Exception{
  Context c=RuntimeEnvironment.getApplication();
  String original=new JSONObject().put("vehicles",new org.json.JSONArray()).toString();
  c.getSharedPreferences("verified_full_export",0).edit().clear().commit();
  try{
   assertTrue(BackupGpsProof.fullCurrentStatus(c,original).contains("미검증"));
   BackupGpsProof.record(c,Collections.emptyMap(),Collections.emptyMap(),original,true);
   assertTrue(BackupGpsProof.fullCurrentStatus(c,original).contains("일치"));
   assertTrue(BackupGpsProof.fullCurrentStatus(c,original+" ").contains("데이터 변경"));
  }finally{c.getSharedPreferences("verified_full_export",0).edit().clear().commit();}
 }
 @Test public void gpsGapsRemainVisibleAndNotCountedAsObservedTime(){
  RideInsights.Result result=new RideInsights.Result(20,1,3.5,54,
   1000L,601000L,200000L,300000L,Double.NaN,Double.NaN,2,1);
  assertEquals(2,result.gpsGaps);
  assertEquals(50,result.gpsCoveragePercent());
  assertTrue(result.gpsQuality().contains("수신률 50%"));
  assertEquals(3.5,result.distanceKm,0.001);
 }
}
