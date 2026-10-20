package com.motohub.next;

import java.time.LocalDate;
import java.util.*;
import org.json.JSONObject;

/** Builds one all-or-nothing maintenance transaction. Does not touch storage. */
final class ServiceBatch {
 private ServiceBatch(){}
 static final String[] COMMON={"엔진오일","오일필터","에어필터","체인","브레이크 패드","타이어","냉각수","기타"};
 static final int MAX_ITEMS=8;
 static final class Item {
  final String type,cost,note;
  Item(String type,String cost,String note){this.type=type;this.cost=cost;this.note=note;}
 }
 static final class Result {
  final List<JSONObject> records;
  final long totalCost;
  Result(List<JSONObject> records,long totalCost){this.records=records;this.totalCost=totalCost;}
 }
 static Result build(String bikeId,String date,String mileage,List<Item> items,List<JSONObject> previous,boolean reuseIntervals)throws Exception{
  if(bikeId==null||!bikeId.matches("[a-fA-F0-9]{8}(-[a-fA-F0-9]{4}){3}-[a-fA-F0-9]{12}"))throw new IllegalArgumentException("바이크 ID가 올바르지 않습니다.");
  try{LocalDate.parse(date);}catch(Exception invalid){throw new IllegalArgumentException("정비 날짜를 확인해 주세요.");}
  if(mileage==null||!mileage.trim().matches("[0-9]+(?:\\.[0-9]{1,2})?"))throw new IllegalArgumentException("계기판을 확인해 주세요.");
  double km=Double.parseDouble(mileage.trim());if(!Double.isFinite(km)||km<0||km>10_000_000)throw new IllegalArgumentException("계기판 범위를 확인해 주세요.");
  if(items==null||items.isEmpty()||items.size()>MAX_ITEMS)throw new IllegalArgumentException("정비 항목은 1~8개 선택해 주세요.");
  Set<String> types=new HashSet<>();List<JSONObject> result=new ArrayList<>();long total=0;
  String bundleId=UUID.randomUUID().toString();
  for(Item item:items){
   if(item==null||item.type==null||item.type.trim().isEmpty()||item.type.trim().length()>60||!types.add(item.type.trim()))throw new IllegalArgumentException("동일한 정비 항목을 중복 선택할 수 없습니다.");
   if(item.cost==null||!item.cost.trim().matches("[0-9]{1,10}"))throw new IllegalArgumentException(item.type+" 비용을 확인해 주세요.");
   long cost=Long.parseLong(item.cost.trim());if(cost>1_000_000_000L||total>1_000_000_000L-cost)throw new IllegalArgumentException("금액 한도를 초과했습니다.");
   total+=cost;
   if(item.note!=null&&item.note.length()>500)throw new IllegalArgumentException("메모는 500자 이내로 입력해 주세요.");
   long nextKm=0;
   if(reuseIntervals){
    JSONObject latest=QuickServiceHistory.latest(previous,bikeId,item.type.trim());
    if(latest!=null)nextKm=RecentRecordReuse.nextServiceKm(latest.optDouble("mileage",Double.NaN),latest.optDouble("nextKm",Double.NaN),km);
   }
   JSONObject row=new JSONObject();row.put("id",UUID.randomUUID().toString());row.put("vehicleId",bikeId);
   row.put("type",item.type.trim());row.put("date",date);row.put("mileage",km);row.put("cost",cost);
   row.put("nextKm",nextKm);row.put("nextDate","");row.put("note",item.note==null?"":item.note.trim());row.put("serviceBatchId",bundleId);
   result.add(row);
  }
  return new Result(result,total);
 }
 static int duplicates(List<JSONObject> existing,List<JSONObject> incoming){
  int count=0;for(JSONObject row:incoming)if(QuickLogDuplicate.exists(existing,row,"services"))count++;return count;
 }
}
