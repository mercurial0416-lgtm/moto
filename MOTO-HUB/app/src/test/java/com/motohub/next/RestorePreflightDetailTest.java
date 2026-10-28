package com.motohub.next;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public final class RestorePreflightDetailTest {
 @Test public void warnsBeforeReplacingMoreCompleteGarage()throws Exception{
  JSONObject existing=Store.empty(),incoming=Store.empty();
  existing.getJSONArray("vehicles").put(new JSONObject().put("id","a").put("name","Duke"));
  String text=RestorePreflightDetail.text(incoming,existing,0,0,0);
  assertTrue(text.contains("기존 1건 → 복원 후 0건"));
  assertTrue(text.contains("GPS 기록 파일이 없습니다"));
 }
}
