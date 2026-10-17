package com.motohub.next;
import org.json.*;
import java.time.LocalDate;
import java.util.*;
import java.nio.charset.StandardCharsets;

/** v1 backup-compatible JSON, retaining unknown fields and unused collections. */
public final class Store {
 public static final String[] COLLECTIONS={"vehicles","watchlist","listings","services","expenses","shops","routes","specs","gear"};
 public static JSONObject empty() throws JSONException {
  JSONObject d=new JSONObject();d.put("version",3);
  for(String k:COLLECTIONS)d.put(k,new JSONArray());
  d.put("settings",new JSONObject());return d;
 }
 public static JSONObject parse(String raw) throws JSONException {
  if(raw==null||raw.length()>8_000_000||raw.getBytes(StandardCharsets.UTF_8).length>8_000_000)throw new JSONException("백업 최대 크기는 8MB입니다.");
  JSONObject d=new JSONObject(raw);int version=d.optInt("version",1);
  if(version>3||version<1)throw new JSONException("지원하지 않는 백업 버전입니다.");
  for(String k:new String[]{"vehicles","listings","services","expenses"})if(!(d.opt(k) instanceof JSONArray))throw new JSONException("백업에 "+k+" 목록이 없습니다.");
  for(String k:COLLECTIONS){
   if(!d.has(k))d.put(k,new JSONArray());
   if(!(d.opt(k) instanceof JSONArray))throw new JSONException(k+" 목록 형식 오류");
   JSONArray a=d.getJSONArray(k);if(a.length()>20000)throw new JSONException("기록 수가 너무 많습니다.");
   Set<String> ids=new HashSet<>();
   for(int i=0;i<a.length();i++){
    JSONObject o=a.getJSONObject(i);String id=o.optString("id","");
    if(id.isEmpty()||!ids.add(id))throw new JSONException(k+" ID가 없거나 중복됩니다.");
    for(String n:new String[]{"odo","price","mileage","cost","amount","liters","purchase","nextKm","distance"})if(o.has(n)){double value=o.optDouble(n,Double.NaN);if(!Double.isFinite(value)||value<0||value>1e13)throw new JSONException(n+" 숫자 오류");}
    if(k.equals("listings")&&!o.optString("url").isEmpty()&&!safeUrl(o.optString("url")))throw new JSONException("매물 주소는 http(s)만 가능합니다.");
    for(String date:new String[]{"date","nextDate","purchaseDate","soldDate","insuranceDate"})try{validateDate(o.optString(date));}catch(Exception e){throw new JSONException("날짜 형식 오류: "+date);}
    if(o.has("priceHistory")){JSONArray hist=o.getJSONArray("priceHistory");for(int h=0;h<hist.length();h++){double p=hist.getJSONObject(h).optDouble("price",Double.NaN);if(!Double.isFinite(p)||p<0)throw new JSONException("가격 이력 오류");}}
    if(o.has("checks")&&!(o.opt("checks") instanceof JSONObject))throw new JSONException("체크리스트 형식 오류");
    if(k.equals("vehicles")&&o.optString("name").trim().isEmpty())throw new JSONException("바이크 이름이 없습니다.");
   }
  }
  Set<String> vehicles=new HashSet<>();JSONArray a=d.getJSONArray("vehicles");for(int i=0;i<a.length();i++)vehicles.add(a.getJSONObject(i).getString("id"));
  for(String k:new String[]{"expenses","services"}){a=d.getJSONArray(k);for(int i=0;i<a.length();i++)if(!vehicles.contains(a.getJSONObject(i).optString("vehicleId")))throw new JSONException("차량이 없는 "+k+" 기록입니다.");}
  if(!d.has("settings"))d.put("settings",new JSONObject());if(!(d.opt("settings") instanceof JSONObject))throw new JSONException("설정 형식 오류");
  d.put("version",3);return d;
 }
 public static boolean safeUrl(String s){try{java.net.URI u=new java.net.URI(s);return ("https".equalsIgnoreCase(u.getScheme())||"http".equalsIgnoreCase(u.getScheme()))&&u.getHost()!=null&&u.getUserInfo()==null;}catch(Exception e){return false;}}
 public static String canonicalUrl(String s){return s.trim().replaceFirst("#.*$","").replaceFirst("/$","");}
 public static String id(){return UUID.randomUUID().toString();}
 public static JSONObject find(JSONObject d,String key,String id)throws JSONException{JSONArray a=d.getJSONArray(key);for(int i=0;i<a.length();i++)if(a.getJSONObject(i).optString("id").equals(id))return a.getJSONObject(i);return null;}
 public static void upsert(JSONObject d,String key,JSONObject item)throws JSONException{JSONArray a=d.getJSONArray(key);for(int i=0;i<a.length();i++)if(a.getJSONObject(i).optString("id").equals(item.optString("id"))){a.put(i,item);return;}a.put(item);}
 public static void remove(JSONObject d,String key,String id)throws JSONException{JSONArray a=d.getJSONArray(key);for(int i=a.length()-1;i>=0;i--)if(a.getJSONObject(i).optString("id").equals(id))a.remove(i);}
 public static double monthly(JSONObject d,String month)throws JSONException{double sum=0;for(String k:new String[]{"expenses","services"}){JSONArray a=d.getJSONArray(k);for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);if(o.optString("date").startsWith(month))sum+=o.optDouble(k.equals("services")?"cost":"amount",0);}}return sum;}
 public static boolean due(JSONObject s,JSONObject bike,String today){return (s.optDouble("nextKm")>0&&bike.optDouble("odo")>=s.optDouble("nextKm"))||(!s.optString("nextDate").isEmpty()&&s.optString("nextDate").compareTo(today)<=0);}
 public static void validateDate(String s){if(!s.isEmpty())LocalDate.parse(s);}

 /** Full-to-full only. Includes intermediate partial fills; rejects incomplete segments. */
 public static JSONObject fuelStats(JSONObject d,String vehicleId)throws JSONException {
  List<JSONObject> fills=new ArrayList<>();JSONArray expenses=d.getJSONArray("expenses");
  for(int i=0;i<expenses.length();i++){JSONObject e=expenses.getJSONObject(i);if(vehicleId.equals(e.optString("vehicleId"))&&"주유".equals(e.optString("type")))fills.add(e);}
  fills.sort((a,b)->{int date=a.optString("date").compareTo(b.optString("date"));return date!=0?date:Double.compare(a.optDouble("mileage"),b.optDouble("mileage"));});
  double baseline=-1,last=-1,pending=0,distance=0,liters=0;int segments=0;boolean valid=false;
  for(JSONObject e:fills){double km=e.optDouble("mileage"),fuel=e.optDouble("liters");boolean full=e.optBoolean("fullTank",false);
   if(baseline>=0){if(km<last||fuel<=0)valid=false;pending+=fuel;}
   if(full){if(baseline>=0&&valid&&pending>0&&km>baseline){distance+=km-baseline;liters+=pending;segments++;}baseline=km;pending=0;valid=fuel>0;}
   last=km;
  }
  JSONObject result=new JSONObject();result.put("segments",segments);result.put("distance",distance);result.put("liters",liters);result.put("kmPerLiter",liters>0?distance/liters:0);return result;
 }
}
