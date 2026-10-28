package com.motohub.next;
import android.content.Context;
import java.io.IOException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public final class UiSettingsArchiveTest {
 @Test public void allowlistedUiPrefsRoundTripWithoutSecrets()throws Exception{
  Context context=RuntimeEnvironment.getApplication();
  context.getSharedPreferences("home_shortcuts",0).edit().clear().commit();
  context.getSharedPreferences("navigation_choice",0).edit().clear().commit();
  context.getSharedPreferences("notifications",0).edit().clear().commit();
  try{
   context.getSharedPreferences("home_shortcuts",0).edit()
    .putString("order","nav,ride,fuel,service").putBoolean("fuel",false).commit();
   String saved=UiSettingsArchive.capture(context);
   assertFalse(saved.contains("token"));assertFalse(saved.contains("password"));
   context.getSharedPreferences("home_shortcuts",0).edit()
    .putString("order","ride,nav,fuel,service").putBoolean("fuel",true).commit();
   UiSettingsArchive.restore(context,saved);
   assertEquals("nav,ride,fuel,service",context.getSharedPreferences("home_shortcuts",0).getString("order",""));
   assertFalse(context.getSharedPreferences("home_shortcuts",0).getBoolean("fuel",true));
   JSONObject altered=new JSONObject(saved);
   altered.getJSONObject("shortcuts").put("order","garbage");
   try{UiSettingsArchive.restore(context,altered.toString());fail("invalid shortcuts must reject");}
   catch(IOException expected){}
  }finally{
   context.getSharedPreferences("home_shortcuts",0).edit().clear().commit();
   context.getSharedPreferences("navigation_choice",0).edit().clear().commit();
   context.getSharedPreferences("notifications",0).edit().clear().commit();
  }
 }
}
