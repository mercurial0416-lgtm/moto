package com.motohub.next;
import org.json.*;
public final class RestorePreviewTest {
 static int n=0;
 static void eq(Object got,Object expected){n++;if(!got.equals(expected))throw new AssertionError("got: "+got+" expected: "+expected);}
 public static void main(String[] args){
  JSONObject source=new JSONObject(),current=new JSONObject();
  source.put("vehicles",new JSONArray(2)).put("services",new JSONArray(5)).put("expenses",new JSONArray(8)).put("listings",new JSONArray(2));
  current.put("vehicles",new JSONArray(1)).put("services",new JSONArray(3)).put("expenses",new JSONArray(8));
  String s=RestorePreview.text(source,current,2,3);
  eq(s.contains("바이크 1 → 2건"),true);eq(s.contains("정비 3 → 5건"),true);
  eq(s.contains("주유·비용 8 → 8건"),true);eq(s.contains("매물 0 → 2건"),true);
  eq(s.contains("GPS 2건 · 사진 3장"),true);eq(RestorePreview.count(source,"gear"),0);
  eq(RestorePreview.count(null,"vehicles"),0);System.out.println("PASS: "+n+" restore preview assertions");
 }
}
