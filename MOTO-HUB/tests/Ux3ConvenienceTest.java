package com.motohub.next;
import org.json.JSONObject;
import java.util.*;

public final class Ux3ConvenienceTest {
 static int count;
 static void ok(boolean yes,String reason){count++;if(!yes)throw new AssertionError(reason);}
 static JSONObject fuel(String bike,String date,double odo,double liters,double amount,boolean full){
  return new JSONObject().put("vehicleId",bike).put("type","주유").put("date",date)
   .put("mileage",odo).put("liters",liters).put("amount",amount).put("fullTank",full);
 }
 public static void main(String[] args){
  String bike="12345678-1234-1234-1234-123456789abc",other="aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
  ok(ReceiptSlots.validId(bike),"valid UUID accepted");
  ok(!ReceiptSlots.validId("../evil"),"traversal rejected");
  ok(!ReceiptSlots.validSlot(0)&&!ReceiptSlots.validSlot(5),"receipt slot range enforced");
  ok(ReceiptSlots.filename(bike,1).equals("record-receipt-"+bike+".jpg"),"legacy receipt retained");
  ok(ReceiptSlots.filename(bike,2).equals("record-receipt-"+bike+"-2.jpg"),"second receipt unique");
  for(int slot=1;slot<=4;slot++)ok(BackupFormat.allowed("receipts/"+ReceiptSlots.filename(bike,slot),false),"slot "+slot+" backup allowed");
  ok(!BackupFormat.allowed("receipts/record-receipt-"+bike+"-5.jpg",false),"fifth slot denied");
  ok(!BackupFormat.allowed("receipts/record-receipt-"+bike+"-2.jpg/../../x",false),"zip traversal denied");
  long now=1_800_000_000_000L;
  ok(BatchDraftPolicy.recoverable(bike,bike,now-1000,now),"fresh same bike draft");
  ok(!BatchDraftPolicy.recoverable(bike,other,now-1000,now),"other bike draft denied");
  ok(!BatchDraftPolicy.recoverable(bike,bike,now-BatchDraftPolicy.MAX_AGE_MS-1,now),"expired draft denied");
  ok(!BatchDraftPolicy.recoverable(bike,bike,now+1000,now),"future draft denied");
  List<JSONObject> service=Arrays.asList(
   new JSONObject().put("vehicleId",bike).put("serviceBatchId",other).put("type","엔진오일").put("cost",42000d).put("date","2026-08-08").put("mileage",1000d),
   new JSONObject().put("vehicleId",bike).put("serviceBatchId",other).put("type","오일필터").put("cost",6000d).put("date","2026-08-08").put("mileage",1000d),
   new JSONObject().put("vehicleId",other).put("serviceBatchId",bike).put("type","체인").put("cost",80000d).put("date","2026-12-08").put("mileage",1000d),
   new JSONObject().put("vehicleId",bike).put("serviceBatchId",bike).put("type","엔진오일").put("cost",30000d).put("date","2026-10-20").put("mileage",2000d));
  List<ServiceBatch.Item> latest=ServiceBatchPreset.latest(service,bike);
  ok(latest.size()==1&&latest.get(0).type.equals("엔진오일")&&latest.get(0).cost.equals("30000"),"most recent own batch");
  ok(ServiceBatchPreset.latest(service,"invalid").isEmpty(),"untrusted bike rejected");
  List<JSONObject> fuels=Arrays.asList(
   fuel(other,"2026-01-01",150,5,9000,true),
   fuel(bike,"2026-03-05",1000,5,10000,true),
   fuel(bike,"2026-03-09",1060,4,8000,false),
   fuel(bike,"2026-03-15",1120,4,8000,true),
   fuel(bike,"2026-03-17",1180,5,10000,false),
   fuel(bike,"2026-03-20",1240,5,10000,true));
  FuelStats stat=FuelStats.forBike(fuels,bike);
  ok(stat.fills==5,"bike separated");
  ok(Math.abs(stat.liters-23)<0.0001&&stat.cost==46000,"cost and liters");
  ok(stat.fullIntervals==2,"two complete full-tank intervals");
  ok(Math.abs(stat.kmPerLiter-(240d/18))<0.001,"intervals accumulate actual filled liters");
  ok(FuelStats.forBike(Collections.emptyList(),bike).fullIntervals==0,"empty entries safe");
  System.out.println("PASS UX3 convenience/receipts/analytics: "+count+" assertions");
 }
}
