package com.motohub.next;
import android.content.Context;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.junit.Assume.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public final class Ux74MissingNavigatorRestoreTest {
 @Test public void restoringBackupCannotChooseAbsentNavigator()throws Exception {
  Context c=RuntimeEnvironment.getApplication();
  assumeTrue(NavigationApps.installedLaunchIntent(c,NavigationChoice.TMAP)==null);
  c.getSharedPreferences("navigation_choice",0).edit().putString("preferred",NavigationChoice.NAVER.id).commit();
  JSONObject backup=new JSONObject().put("schema",1).put("order","ride,nav,fuel,service")
   .put("ride",true).put("nav",true).put("fuel",true).put("service",true)
   .put("navigator",NavigationChoice.TMAP.id).put("maintenanceNotifications",false);
  DevicePreferencesBackup.restore(c,backup.toString());
  assertEquals(NavigationChoice.NAVER.id,
   c.getSharedPreferences("navigation_choice",0).getString("preferred",""));
 }
 @Test public void validBackupRestoresHomeLayoutAndNotificationPreference()throws Exception {
  Context c=RuntimeEnvironment.getApplication();
  c.getSharedPreferences("home_shortcuts",0).edit().clear().commit();
  c.getSharedPreferences("notifications",0).edit().clear().commit();
  JSONObject backup=new JSONObject().put("schema",1).put("order","service,ride,nav,fuel")
   .put("ride",true).put("nav",false).put("fuel",true).put("service",true)
   .put("navigator",NavigationChoice.KAKAO.id).put("maintenanceNotifications",false);
  DevicePreferencesBackup.restore(c,backup.toString());
  assertEquals("service,ride,nav,fuel",c.getSharedPreferences("home_shortcuts",0).getString("order",""));
  assertFalse(c.getSharedPreferences("home_shortcuts",0).getBoolean("nav",true));
  assertFalse(c.getSharedPreferences("notifications",0).getBoolean("maintenance_opt_in",true));
 }
 @Test public void invalidBackupCannotPartiallyOverwriteHomeSettings()throws Exception {
  Context c=RuntimeEnvironment.getApplication();
  c.getSharedPreferences("home_shortcuts",0).edit().putString("order","nav,ride,fuel,service").commit();
  JSONObject invalid=new JSONObject().put("schema",1).put("order","ride,ride,fuel,service")
   .put("ride",true).put("nav",true).put("fuel",true).put("service",true)
   .put("navigator",NavigationChoice.KAKAO.id).put("maintenanceNotifications",true);
  try{DevicePreferencesBackup.restore(c,invalid.toString());fail("duplicate order must be rejected");}
  catch(java.io.IOException expected){}
  assertEquals("nav,ride,fuel,service",
   c.getSharedPreferences("home_shortcuts",0).getString("order",""));
 }

}
