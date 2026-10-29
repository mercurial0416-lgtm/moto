package com.motohub.next;
import android.content.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public final class Ux73DevicePreferencesBackupTest {
 @Test public void onlyAllowlistedUiPreferencesCanBeArchived()throws Exception{
  Context c=RuntimeEnvironment.getApplication();
  c.getSharedPreferences("home_shortcuts",0).edit().putString("order","nav,ride,fuel,service")
   .putBoolean("service",false).commit();
  c.getSharedPreferences("navigation_choice",0).edit().putString("preferred","naver").commit();
  String snapshot=DevicePreferencesBackup.snapshot(c);
  DevicePreferencesBackup.validate(snapshot);
  assertTrue(snapshot.contains("nav,ride,fuel,service"));
  assertTrue(snapshot.contains("naver"));
  assertFalse(snapshot.contains("password"));
  try{DevicePreferencesBackup.validate(snapshot.replace("\"naver\"","\"unknown\""));
    fail("unknown navigator must be rejected");}
  catch(java.io.IOException expected){}
  c.getSharedPreferences("home_shortcuts",0).edit().clear().commit();
  c.getSharedPreferences("navigation_choice",0).edit().clear().commit();
 }
}
