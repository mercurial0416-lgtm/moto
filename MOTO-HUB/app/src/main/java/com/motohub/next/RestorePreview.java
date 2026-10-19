package com.motohub.next;
import org.json.JSONArray;
import org.json.JSONObject;
/** Read-only import preview. No mutation until a second explicit confirmation. */
final class RestorePreview {
 private RestorePreview(){}
 static int count(JSONObject data,String key){JSONArray array=data==null?null:data.optJSONArray(key);return array==null?0:array.length();}
 static String text(JSONObject incoming,JSONObject current,int rides,int photos){
  StringBuilder b=new StringBuilder("백업 데이터 미리보기 (현재 → 복원 후)\n");
  for(String[] pair:new String[][]{{"vehicles","바이크"},{"services","정비"},{"expenses","주유·비용"},{"listings","매물"},{"routes","코스"},{"gear","장구"}})
   b.append(pair[1]).append(" ").append(count(current,pair[0])).append(" → ").append(count(incoming,pair[0])).append("건\n");
  return b.append("GPS ").append(rides).append("건 · 사진 ").append(photos).append("장").toString();
 }
}
