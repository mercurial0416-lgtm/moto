package com.motohub.next;
import org.json.*;
import java.util.Locale;
/** Human-readable bounded history; no writes, no network, no sensitive identifiers. */
final class JsonConflictPreview {
 private JsonConflictPreview(){}
 static String text(JSONObject imported,String bikeId){
  StringBuilder b=new StringBuilder("원본 보류 기록 미리보기");
  int total=0,shown=0;
  for(String collection:new String[]{"services","expenses"}){
   JSONArray rows=imported.optJSONArray(collection);
   if(rows==null)continue;
   for(int i=0;i<rows.length();i++){
    JSONObject row=rows.optJSONObject(i);
    if(row==null||!bikeId.equals(row.optString("vehicleId")))continue;
    total++;
    if(shown++>=6)continue;
    String date=row.optString("date","날짜 미상"),type=row.optString("type","");
    double cost=row.optDouble("services".equals(collection)?"cost":"amount",0);
    b.append("\n").append("services".equals(collection)?"정비":"주유·비용")
      .append(" · ").append(date);
    if(!type.isEmpty())b.append(" · ").append(type);
    if(Double.isFinite(cost)&&cost>=0)b.append(String.format(Locale.KOREA," · %,.0f원",cost));
   }
  }
  if(total==0)return "이 원본 바이크에 보류된 정비·주유 이력이 없습니다.";
  if(total>6)b.append("\n외 ").append(total-6).append("건");
  return b.append("\n총 ").append(total).append("건 · 같은 바이크인지 확인한 후 선택하세요.").toString();
 }
}
