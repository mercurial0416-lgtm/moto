package com.motohub.next;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Stages a malformed JSON backup with missing motorcycle references for MANUAL
 * additive import. Never relinks automatically, writes to storage, or changes
 * the user's original backup. Synthetic source bikes are discarded at merge.
 */
final class JsonOrphanRepair {
 private JsonOrphanRepair(){}
 static final class Staged {
  final JSONObject source;
  final List<String> missing;
  Staged(JSONObject source,List<String> missing){this.source=source;this.missing=missing;}
 }
 static Staged stage(String raw)throws JSONException{
  if(raw==null||raw.length()>8_000_000)throw new JSONException("JSON 크기 제한 초과");
  JSONObject candidate=new JSONObject(raw);
  JSONArray bikes=candidate.optJSONArray("vehicles");
  JSONArray services=candidate.optJSONArray("services");
  JSONArray expenses=candidate.optJSONArray("expenses");
  JSONArray listings=candidate.optJSONArray("listings");
  if(bikes==null||services==null||expenses==null||listings==null)
   throw new JSONException("필수 차량·정비·비용 목록이 없습니다.");
  Set<String> known=new LinkedHashSet<>();
  for(int i=0;i<bikes.length();i++){
   JSONObject row=bikes.getJSONObject(i);
   known.add(row.optString("id"));
  }
  Set<String> orphanIds=new LinkedHashSet<>();
  for(JSONArray records:new JSONArray[]{services,expenses}){
   for(int i=0;i<records.length();i++){
    JSONObject row=records.getJSONObject(i);
    String id=row.optString("vehicleId","");
    if(id.isEmpty())throw new JSONException("차량 ID가 비어 있는 기록은 자동 구분할 수 없습니다.");
    if(!known.contains(id)){
     if(id.length()>128||!id.matches("[A-Za-z0-9_-]+"))
      throw new JSONException("비정상 차량 ID는 수동 연결을 지원하지 않습니다.");
     orphanIds.add(id);
    }
   }
  }
  if(orphanIds.isEmpty())throw new JSONException("연결되지 않은 차량 ID가 없습니다. 일반 백업 검사를 확인하세요.");
  if(orphanIds.size()>20)throw new JSONException("연결되지 않은 차량 종류가 20개를 넘습니다.");
  // Validate with the normal store parser AFTER adding in-memory placeholders.
  // No placeholders are written to the live DB, even if the user confirms.
  JSONObject staged=new JSONObject(candidate.toString());
  JSONArray stagedBikes=staged.getJSONArray("vehicles");
  for(String missing:orphanIds)
   stagedBikes.put(new JSONObject().put("id",missing).put("name","수동 연결 대기 차량"));
  return new Staged(Store.parse(staged.toString()),new ArrayList<>(orphanIds));
 }
}
