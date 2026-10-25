package com.motohub.next;
import java.util.*;
import org.json.JSONObject;
public final class ServiceBatchTest {
 static int count;
 static void ok(boolean v,String name){count++;if(!v)throw new AssertionError(name);}
 static void reject(Runnable job){try{job.run();throw new AssertionError("invalid batch accepted");}catch(IllegalArgumentException expected){count++;}}
 static ServiceBatch.Result build(String bike,String date,String mileage,List<ServiceBatch.Item> items,List<JSONObject> old,boolean reuse){
  try{return ServiceBatch.build(bike,date,mileage,items,old,reuse);}catch(IllegalArgumentException e){throw e;}catch(Exception e){throw new RuntimeException(e);}
 }
 public static void main(String[] args){
  String bike="12345678-1234-1234-1234-123456789abc",other="99999999-1234-1234-1234-123456789abc";
  List<ServiceBatch.Item> two=Arrays.asList(new ServiceBatch.Item("엔진오일","30000",""),new ServiceBatch.Item("오일필터","5000",""));
  List<JSONObject> history=new ArrayList<>();
  history.add(new JSONObject().put("id","a").put("vehicleId",other).put("type","엔진오일").put("date","2026-09-01").put("mileage",1000).put("nextKm",100000).put("cost",999));
  history.add(new JSONObject().put("id","b").put("vehicleId",bike).put("type","엔진오일").put("date","2026-09-02").put("mileage",2000).put("nextKm",5000).put("cost",25000));
  ServiceBatch.Result batch=build(bike,"2026-10-20","3500",two,history,true);
  ok(batch.records.size()==2,"two entries");ok(batch.totalCost==35000,"one combined total");
  ok(batch.records.get(0).optDouble("nextKm")==6500,"reuse own previous interval only");
  ok(batch.records.get(1).optDouble("nextKm")==0,"no invented interval");
  ok(batch.records.get(0).optString("serviceBatchId").equals(batch.records.get(1).optString("serviceBatchId")),"shared group id");
  ok(!batch.records.get(0).optString("id").equals(batch.records.get(1).optString("id")),"unique transaction item ids");
  ok(batch.records.get(0).optString("vehicleId").equals(bike),"bike retained");
  ok(ServiceBatch.duplicates(history,batch.records)==0,"different day not duplicate");
  history.add(new JSONObject().put("id","x").put("vehicleId",bike).put("type","엔진오일").put("date","2026-10-20").put("mileage",3500).put("cost",30000));
  ok(ServiceBatch.duplicates(history,batch.records)==1,"duplicate flagged not silently discarded");
  ok(build(bike,"2026-10-20","3500",two,history,false).records.get(0).optDouble("nextKm")==0,"no silent interval reuse");
  reject(()->build(bike,"2026-10-20","3500",Arrays.asList(two.get(0),two.get(0)),history,false));
  reject(()->build(bike,"2026-10-20","3500",Collections.emptyList(),history,false));
  reject(()->build(bike,"2026-10-20","-1",two,history,false));
  reject(()->build(bike,"invalid","3500",two,history,false));
  reject(()->build("../bad","2026-10-20","3500",two,history,false));
  reject(()->build(bike,"2026-10-20","3500",Collections.singletonList(new ServiceBatch.Item("엔진오일","-10","")),history,false));
  reject(()->build(bike,"2026-10-20","3500",Collections.singletonList(new ServiceBatch.Item("엔진오일","1000000001","")),history,false));
  List<ServiceBatch.Item> excess=new ArrayList<>();for(int i=0;i<9;i++)excess.add(new ServiceBatch.Item("T"+i,"100",""));
  reject(()->build(bike,"2026-10-20","3500",excess,history,false));
  ok(build(bike,"2026-10-20","0",Collections.singletonList(new ServiceBatch.Item("DIY","0","")),history,false).totalCost==0,"zero-cost DIY accepted");
  System.out.println("PASS batch service transaction safety: "+count+" assertions");
 }
}
