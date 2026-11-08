package com.motohub.next;

import android.content.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35) // Robolectric 4.14.1 does not support the app's compile/target SDK 36.
public class BikeObdPreferencesTest {
 private static final String DUKE="9f87e196-787f-4dc2-b292-37f70f6f5f3d";
 private static final String SECOND="5dff9845-efae-4200-9078-843042d4bb7e";
 @Test public void adaptersAndSensorSelectionsBelongToEachBike(){
  Context app=RuntimeEnvironment.getApplication();
  BikeObdPreferences.clearBike(app,DUKE);
  BikeObdPreferences.clearBike(app,SECOND);
  try{
   BikeObdPreferences.setDevice(app,DUKE,"AA:BB:CC:DD:EE:01","Vgate");
   BikeObdPreferences.setDevice(app,SECOND,"AA:BB:CC:DD:EE:02","Other");
   BikeObdPreferences.setSelectedPids(app,DUKE,Arrays.asList("010C","010D","01A6"));
   BikeObdPreferences.setSelectedPids(app,SECOND,Collections.singletonList("0105"));
   assertEquals("AA:BB:CC:DD:EE:01",BikeObdPreferences.address(app,DUKE));
   assertEquals("AA:BB:CC:DD:EE:02",BikeObdPreferences.address(app,SECOND));
   assertEquals(Arrays.asList("010C","010D","01A6"),BikeObdPreferences.selectedPids(app,DUKE));
   assertEquals(Collections.singletonList("0105"),BikeObdPreferences.selectedPids(app,SECOND));
   BikeObdPreferences.clearDevice(app,DUKE);
   assertEquals("",BikeObdPreferences.address(app,DUKE));
   assertEquals("AA:BB:CC:DD:EE:02",BikeObdPreferences.address(app,SECOND));
   assertEquals(3,BikeObdPreferences.selectedPids(app,DUKE).size());
   BikeObdPreferences.clearSelectedPids(app,DUKE);
   assertEquals(Collections.emptyList(),BikeObdPreferences.selectedPids(app,DUKE));
   assertEquals(Collections.singletonList("0105"),BikeObdPreferences.selectedPids(app,SECOND));
  }finally{
   BikeObdPreferences.clearBike(app,DUKE);
   BikeObdPreferences.clearBike(app,SECOND);
  }
 }
 @Test public void rejectsUnsafeIdentifiersAndDeduplicatesSelectedPids(){
  Context app=RuntimeEnvironment.getApplication();
  try{BikeObdPreferences.setDevice(app,DUKE,"not a MAC","test");fail();}
  catch(IllegalArgumentException expected){}
  try{BikeObdPreferences.setSelectedPids(app,DUKE,Arrays.asList("010C","0400"));fail();}
  catch(IllegalArgumentException expected){}
  BikeObdPreferences.setSelectedPids(app,DUKE,Arrays.asList("010C","010C","0105"));
  assertEquals(Arrays.asList("010C","0105"),BikeObdPreferences.selectedPids(app,DUKE));
  BikeObdPreferences.clearBike(app,DUKE);
 }
}
