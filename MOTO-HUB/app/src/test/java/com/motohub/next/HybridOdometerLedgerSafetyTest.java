package com.motohub.next;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
public class HybridOdometerLedgerSafetyTest {
 private static final String BIKE="01234567-89ab-4cde-8fab-0123456789ab";
 private static final String OTHER="01234567-89ab-4cde-8fab-1123456789ab";
 private static final String A="1699999999999-01234567-89ab-4cde-8fab-0123456789ab.track";
 private static final String B="1699999999998-01234567-89ab-4cde-8fab-0123456789ab.track";
 private static final String C="1699999999997-01234567-89ab-4cde-8fab-0123456789ab.track";
 private Context context;
 @Before public void setup(){
  context=RuntimeEnvironment.getApplication();
  context.getSharedPreferences("gps_distance_estimate_v567",0).edit().clear().commit();
  context.getSharedPreferences("hybrid_odo_v588",0).edit().clear().commit();
  RideService.running=false;
  RideService.autoArmed=false;
  RideService.activeRideBikeId="";
 }
 private void writeLedger(String values)throws Exception{
  assertTrue(context.getSharedPreferences("gps_distance_estimate_v567",0)
    .edit().putString("ledger",values).commit());
 }
 private String row(String name,String bike,double km)throws Exception{
  return JSONObject.quote(name)+":{\"bike\":"+JSONObject.quote(bike)+",\"km\":"+km+"}";
 }
 @Test public void detectsLossEvenWhenReplacementKeepsSameTotal()throws Exception{
  writeLedger("{"+row(A,BIKE,10)+","+row(B,BIKE,20)+"}");
  assertTrue(HybridOdometerEstimate.calibrate(context,BIKE,1200));
  assertEquals(1200.0,HybridOdometerEstimate.snapshot(context,BIKE).estimatedKm,0.001);
  // Same aggregate GPS distance but a prior track was deleted.
  writeLedger("{"+row(B,BIKE,20)+","+row(C,BIKE,10)+"}");
  HybridOdometerEstimate.Reading after=HybridOdometerEstimate.snapshot(context,BIKE);
  assertFalse(after.valid);
  assertTrue(after.ledgerChanged);
 }
 @Test public void acceptsOnlyAdditiveCompletedRidesAndRejectsLaterRemoval()throws Exception{
  writeLedger("{"+row(A,BIKE,10)+"}");
  assertTrue(HybridOdometerEstimate.calibrate(context,BIKE,800));
  writeLedger("{"+row(A,BIKE,10)+","+row(B,BIKE,12.5)+"}");
  assertEquals(812.5,HybridOdometerEstimate.snapshot(context,BIKE).estimatedKm,0.001);
  // It was a post-calibration ride: deletion must be detected too.
  writeLedger("{"+row(A,BIKE,10)+"}");
  assertTrue(HybridOdometerEstimate.snapshot(context,BIKE).ledgerChanged);
 }
 @Test public void otherBikeRidesDoNotInflateOurEstimate()throws Exception{
  writeLedger("{"+row(A,BIKE,10)+"}");
  assertTrue(HybridOdometerEstimate.calibrate(context,BIKE,1234));
  writeLedger("{"+row(A,BIKE,10)+","+row(B,OTHER,78)+"}");
  assertEquals(1234,HybridOdometerEstimate.snapshot(context,BIKE).estimatedKm,0.001);
 }
 @Test public void malformedLedgerBlocksGuessing()throws Exception{
  writeLedger("{"+row(A,BIKE,10)+"}");
  assertTrue(HybridOdometerEstimate.calibrate(context,BIKE,1500));
  writeLedger("{not valid json}");
  assertFalse(HybridOdometerEstimate.snapshot(context,BIKE).valid);
 }
 @Test public void oldV588AnchorWithoutMembershipRequiresRecalibration()throws Exception{
  SharedPreferences p=context.getSharedPreferences("hybrid_odo_v588",0);
  p.edit().putString(BIKE+".baseline","1200").putString(BIKE+".gps","0").commit();
  assertTrue(HybridOdometerEstimate.snapshot(context,BIKE).ledgerChanged);
 }
}
