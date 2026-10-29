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
  assumeNull(NavigationApps.installedLaunchIntent(c,NavigationChoice.TMAP));
  c.getSharedPreferences("navigation_choice",0).edit().putString("preferred",NavigationChoice.NAVER.id).commit();
  JSONObject backup=new JSONObject().put("schema",1).put("order","ride,nav,fuel,service")
   .put("ride",true).put("nav",true).put("fuel",true).put("service",true)
   .put("navigator",NavigationChoice.TMAP.id).put("maintenanceNotifications",false);
  DevicePreferencesBackup.restore(c,backup.toString());
  assertEquals(NavigationChoice.NAVER.id,
   c.getSharedPreferences("navigation_choice",0).getString("preferred",""));
 }
}
