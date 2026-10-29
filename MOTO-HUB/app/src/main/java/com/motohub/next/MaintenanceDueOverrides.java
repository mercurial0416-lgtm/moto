package com.motohub.next;
import org.json.JSONObject;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
/** Apply individually confirmed future maintenance targets before atomic batch commit. */
final class MaintenanceDueOverrides {
 private MaintenanceDueOverrides(){}
 static void apply(JSONObject row,String nextKmRaw,String nextDateRaw)throws Exception{
  String distance=nextKmRaw==null?"":nextKmRaw.trim();
  String date=nextDateRaw==null?"":nextDateRaw.trim();
  if(!distance.matches("[0-9]{1,9}"))throw new IllegalArgumentException(row.optString("type")+" 다음 주행거리를 확인해 주세요.");
  double nextKm=Double.parseDouble(distance),current=row.optDouble("mileage",0);
  if(nextKm>0&&nextKm<=current)throw new IllegalArgumentException(row.optString("type")+" 다음 주행거리는 현재보다 커야 해요.");
  Store.validateDate(date);
  LocalDate completed=LocalDate.parse(row.getString("date"));
  int months=0;
  if(!date.isEmpty()){
   LocalDate due=LocalDate.parse(date);
   if(!due.isAfter(completed))throw new IllegalArgumentException(row.optString("type")+" 다음 날짜는 정비일보다 뒤여야 해요.");
   long elapsed=ChronoUnit.MONTHS.between(completed,due);
   if(elapsed>0&&elapsed<=120&&completed.plusMonths(elapsed).equals(due))months=(int)elapsed;
  }
  int intervalKm=nextKm>0?((nextKm-current)<=100000?(int)Math.ceil(nextKm-current):0):0;
  row.put("nextKm",nextKm).put("nextDate",date).put("intervalKm",intervalKm).put("intervalMonths",months);
 }
}