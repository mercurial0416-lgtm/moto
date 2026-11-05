package com.motohub.next;

import java.time.LocalDate;
import java.util.*;
import org.json.*;

/** Read-only per-bike component ledger, with optional metadata retained by existing v3 backups. */
final class ComponentHistory {
 private ComponentHistory(){}
 static final class Component {
  final String type;final List<JSONObject> history=new ArrayList<>();JSONObject schedule;
  Component(String type){this.type=type;}
  JSONObject last(){return history.isEmpty()?null:history.get(0);}
  double usedKm(double odometer){JSONObject last=last();if(last==null)return Double.NaN;double at=last.optDouble("mileage",Double.NaN);return Double.isFinite(at)&&at>=0&&Double.isFinite(odometer)&&odometer>=at?odometer-at:Double.NaN;}
 }
 static List<Component> inspect(List<JSONObject> services,String bikeId,LocalDate today){
  if(services==null)services=Collections.emptyList();
  Map<String,Component> groups=new TreeMap<>();Map<String,JSONObject> latest=LatestServiceByType.latest(services,bikeId);
  for(Map.Entry<String,JSONObject> entry:latest.entrySet())groups.computeIfAbsent(entry.getKey(),Component::new).schedule=entry.getValue();
  for(JSONObject item:services){
   if(item==null||!bikeId.equals(item.optString("vehicleId"))||item.optString("type").trim().isEmpty()||item.optBoolean("scheduleOnly"))continue;
   try{if(LocalDate.parse(item.optString("date")).isAfter(today))continue;}catch(Exception invalid){continue;}
   groups.computeIfAbsent(item.optString("type").trim(),Component::new).history.add(item);
  }
  for(Component group:groups.values()){Collections.reverse(group.history);group.history.sort((a,b)->{int date=b.optString("date").compareTo(a.optString("date"));return date!=0?date:Double.compare(b.optDouble("mileage",0),a.optDouble("mileage",0));});}
  return new ArrayList<>(groups.values());
 }
 static JSONObject annotate(JSONObject record,String product,String spec,String shop,String memo)throws Exception{
  if(record==null||record.optString("id").isEmpty()||record.optString("vehicleId").isEmpty()||record.optBoolean("scheduleOnly"))throw new IllegalArgumentException("실제 완료한 정비 기록을 선택하세요.");
  String[] values={product,spec,shop,memo};int[] limits={120,120,120,1000};String[] keys={"product","spec","shop","memo"};JSONObject oldMeta=record.optJSONObject("componentDetails");JSONObject meta=oldMeta==null?new JSONObject():new JSONObject(oldMeta.toString());
  for(int i=0;i<values.length;i++){String value=values[i]==null?"":values[i].trim();if(value.length()>limits[i]||value.indexOf('\0')>=0)throw new IllegalArgumentException("부품 정보 입력 길이를 확인하세요.");meta.put(keys[i],value);}
  JSONObject result=new JSONObject(record.toString());result.put("componentDetails",meta);return result;
 }
}
