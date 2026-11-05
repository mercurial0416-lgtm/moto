package com.motohub.next;

import java.util.*;
import org.json.*;

/** ID-based diff of validated snapshots. Never prints record values, credentials or coordinates. */
final class BackupChangeSet {
 static final class Counts {int added,removed,changed;String text(){return "추가 "+added+" · 삭제 "+removed+" · 변경 "+changed;}}
 static Map<String,Counts> compare(JSONObject before,JSONObject after)throws Exception{
  before=Store.parse(before.toString());after=Store.parse(after.toString());Map<String,Counts> result=new LinkedHashMap<>();
  for(String key:Store.COLLECTIONS){Map<String,JSONObject> left=index(before.getJSONArray(key)),right=index(after.getJSONArray(key));Counts counts=new Counts();for(String id:left.keySet()){if(!right.containsKey(id))counts.removed++;else if(!same(left.get(id),right.get(id)))counts.changed++;}for(String id:right.keySet())if(!left.containsKey(id))counts.added++;result.put(key,counts);}
  Counts settings=new Counts();JSONObject left=before.getJSONObject("settings"),right=after.getJSONObject("settings");Iterator<String> keys=left.keys();while(keys.hasNext()){String key=keys.next();if(!right.has(key))settings.removed++;else if(!same(left.opt(key),right.opt(key)))settings.changed++;}keys=right.keys();while(keys.hasNext())if(!left.has(keys.next()))settings.added++;result.put("settings",settings);return result;
 }
 private static Map<String,JSONObject> index(JSONArray items)throws Exception{Map<String,JSONObject> result=new HashMap<>();for(int i=0;i<items.length();i++){JSONObject row=items.getJSONObject(i);result.put(row.getString("id"),row);}return result;}
 static boolean same(Object a,Object b)throws Exception{
  if(a==b)return true;if(a==null||b==null)return false;
  if(a instanceof JSONObject&&b instanceof JSONObject){JSONObject x=(JSONObject)a,y=(JSONObject)b;if(x.length()!=y.length())return false;Iterator<String> keys=x.keys();while(keys.hasNext()){String k=keys.next();if(!y.has(k)||!same(x.opt(k),y.opt(k)))return false;}return true;}
  if(a instanceof JSONArray&&b instanceof JSONArray){JSONArray x=(JSONArray)a,y=(JSONArray)b;if(x.length()!=y.length())return false;for(int i=0;i<x.length();i++)if(!same(x.opt(i),y.opt(i)))return false;return true;}
  if(a instanceof Number&&b instanceof Number)return new java.math.BigDecimal(a.toString()).compareTo(new java.math.BigDecimal(b.toString()))==0;
  return a.equals(b);
 }
}
