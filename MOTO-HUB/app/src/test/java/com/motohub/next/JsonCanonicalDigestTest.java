package com.motohub.next;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;
public final class JsonCanonicalDigestTest {
 @Test public void keyOrderDoesNotMatterButSameLengthValueDoes()throws Exception{
  JSONObject first=new JSONObject().put("odo",100).put("name","Duke");
  JSONObject reordered=new JSONObject().put("name","Duke").put("odo",100);
  JSONObject changed=new JSONObject().put("name","Duke").put("odo",101);
  assertEquals(JsonCanonicalDigest.hash(first),JsonCanonicalDigest.hash(reordered));
  assertNotEquals(JsonCanonicalDigest.hash(first),JsonCanonicalDigest.hash(changed));
 }
}
