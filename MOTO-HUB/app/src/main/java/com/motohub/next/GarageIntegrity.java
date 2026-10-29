package com.motohub.next;
import java.util.*;
import org.json.*;

/** Read-only link/odometer audit. Never repairs personal data without user action. */
final class GarageIntegrity {
 private GarageIntegrity(){}
 static String report(JSONObject db){
  if(db==null)return "저장 파일을 읽지 못했습니다. 기존 파일을 삭제하지 마세요.";
  JSONArray bikes=db.optJSONArray("vehicles");
  JSONArray services=db.optJSONArray("services");
  JSONArray expenses=db.optJSONArray("expenses");
  if(bikes==null||services==null||expenses==null)return "기록 목록 형식 오류. 백업을 확인해 주세요.";
  Map<String,Double> odometers=new HashMap<>();
  for(int i=0;i<bikes.length();i++){
   JSONObject v=bikes.optJSONObject(i);if(v!=null)odometers.put(v.optString("id"),v.optDouble("odo",0));
  }
  int missing=0,odoMismatch=0,invalid=0,possibleDuplicates=0;
  Set<String> fingerprints=new HashSet<>();
  for(JSONArray records:new JSONArray[]{services,expenses})for(int i=0;i<records.length();i++){
   JSONObject item=records.optJSONObject(i);if(item==null){invalid++;continue;}
   String id=item.optString("vehicleId");
   if(!odometers.containsKey(id)){missing++;continue;}
   double km=item.optDouble("mileage",Double.NaN);
   double money=item.optDouble(records==services?"cost":"amount",Double.NaN);
   if(!Double.isFinite(km)||km<0||!Double.isFinite(money)||money<0){invalid++;continue;}
   // Historical readings may precede an odometer reset; flag only, never fix.
   if(km>odometers.get(id)+2000)odoMismatch++;
   String key=(records==services?"service":"expense")+"|"+id+"|"+item.optString("date")+
     "|"+item.optString("type")+"|"+km+"|"+money;
   if(!fingerprints.add(key))possibleDuplicates++;
  }
  JSONObject settings=db.optJSONObject("settings");
  JSONObject notes=settings==null?null:settings.optJSONObject("rideNotes");
  if(notes!=null&&notes.length()>10000)invalid++;
  if(missing==0&&odoMismatch==0&&invalid==0&&possibleDuplicates==0)
   return "연결된 바이크·주유·정비 기록 검사 완료. 이상 징후가 없어요. GPS 원본의 개별 손상까지 보증하는 검사는 아닙니다.";
  return "데이터 점검 · 바이크 연결 오류 "+missing+"건, 계기판보다 2,000km 이상 앞선 기록 "+odoMismatch+
    "건, 형식 오류 "+invalid+"건, 중복 의심 "+possibleDuplicates+
    "건. 원본은 수정하지 않았어요. 전체 백업 후 기록별로 확인해 주세요.";
 }
 static boolean hasIssue(String result){return result!=null&&result.startsWith("데이터 점검 ·");}
}