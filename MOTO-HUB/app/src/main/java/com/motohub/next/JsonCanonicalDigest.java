package com.motohub.next;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.IOException;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;
/** Stable value-based digest; records changes even if list counts and file lengths are unchanged. */
final class JsonCanonicalDigest {
 private JsonCanonicalDigest(){}
 static String hash(JSONObject root)throws IOException{
  try{
   MessageDigest md=MessageDigest.getInstance("SHA-256");
   append(md,root,0);
   StringBuilder out=new StringBuilder();
   for(byte part:md.digest())out.append(String.format(Locale.ROOT,"%02x",part&255));
   return out.toString();
  }catch(NoSuchAlgorithmException e){throw new IOException("데이터 SHA-256 사용 불가",e);}
 }
 private static void put(MessageDigest md,String value){
  byte[] bytes=value.getBytes(StandardCharsets.UTF_8);
  md.update(new byte[]{(byte)(bytes.length>>>24),(byte)(bytes.length>>>16),
   (byte)(bytes.length>>>8),(byte)bytes.length});
  md.update(bytes);
 }
 private static void append(MessageDigest md,Object value,int depth)throws IOException{
  if(depth>32)throw new IOException("JSON 구조가 너무 깊습니다.");
  if(value==null||value==JSONObject.NULL){put(md,"null");return;}
  if(value instanceof JSONObject){
   put(md,"object");
   JSONObject obj=(JSONObject)value;
   List<String> keys=new ArrayList<>();
   Iterator<String> it=obj.keys();while(it.hasNext())keys.add(it.next());
   Collections.sort(keys);
   for(String key:keys){put(md,key);append(md,obj.opt(key),depth+1);}
   put(md,"endObject");return;
  }
  if(value instanceof JSONArray){
   put(md,"array");
   JSONArray arr=(JSONArray)value;
   for(int i=0;i<arr.length();i++)append(md,arr.opt(i),depth+1);
   put(md,"endArray");return;
  }
  if(value instanceof Number){
   put(md,"number");put(md,value.toString());return;
  }
  if(value instanceof Boolean){
   put(md,"boolean");put(md,value.toString());return;
  }
  put(md,"string");put(md,String.valueOf(value));
 }
}
