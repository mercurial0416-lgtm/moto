package com.motohub.next;

import org.json.JSONObject;

/** Keep realistic service cycles and low-friction fill calculations regression-tested. */
public final class Ux17ServiceCycleTest {
 static int checks=0;
 static void ok(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
 interface Unsafe {void run()throws Exception;}
 static void bad(Unsafe run,String label){try{run.run();throw new AssertionError(label);}catch(IllegalArgumentException expected){checks++;}catch(Exception failure){throw new AssertionError(label,failure);}}
 public static void main(String[] args)throws Exception{
  String date="2026-10-22";
  JSONObject db=Store.empty();
  JSONObject bike=new JSONObject().put("id","a").put("name","바이크").put("odo",12600);
  db.getJSONArray("vehicles").put(bike);
  JSONObject plan=QuickRecordDraft.maintenance("a","엔진오일",date,12600,15600,"2027-04-22",3000,6);
  plan.put("alertKmBefore",300).put("alertDaysBefore",15);Store.upsert(db,"services",plan);
  ok(MaintenanceAlertWindow.kmBefore(plan)==300,"manual 300 km threshold preserved");
  ok(MaintenanceAlertWindow.daysBefore(plan)==15,"manual 15 day threshold preserved");
  ok(!MaintenanceNotificationSummary.from(db,date).hasNotice(),"not due on registration");
  bike.put("odo",15299);
  ok(!MaintenanceNotificationSummary.from(db,date).hasNotice(),"301 km remaining does not notify");
  bike.put("odo",15300);
  MaintenanceNotificationSummary soon=MaintenanceNotificationSummary.from(db,date);
  ok(soon.soon==1&&soon.detail.contains("엔진오일"),"300 km remaining notifies correct item");
  String stable=soon.fingerprint;
  bike.put("odo",15400);
  ok(stable.equals(MaintenanceNotificationSummary.from(db,date).fingerprint),"no duplicate notice for every odometer update");
  JSONObject completed=ServiceCycle.completed(plan,"2026-10-22",15400,58000);
  ok(!completed.optBoolean("scheduleOnly"),"done is real service, not zero-cost plan");
  ok(completed.optDouble("cost")==58000,"actual cost recorded");
  ok(completed.optDouble("nextKm")==18400,"next +3000 km calculated from actual reading");
  ok(completed.optString("nextDate").equals("2027-04-22"),"next six months from completed date");
  ok(completed.optInt("alertKmBefore")==300&&completed.optInt("alertDaysBefore")==15,"custom notice retained");
  Store.upsert(db,"services",completed);
  ok(!MaintenanceNotificationSummary.from(db,date).hasNotice(),"completion clears old due warning");
  Store.parse(db.toString());
  // Completing a schedule immediately with the same date AND odometer used to
  // leave the garage on the old plan while the notification followed the new one.
  JSONObject sameDayPlan=QuickRecordDraft.maintenance("a","체인",date,15400,0,date,0,6);
  Store.upsert(db,"services",sameDayPlan);
  ok(MaintenanceNotificationSummary.from(db,date).due==1,"same-day due schedule detected");
  JSONObject sameDayDone=ServiceCycle.completed(sameDayPlan,date,15400,0);
  Store.upsert(db,"services",sameDayDone);
  ok(LatestServiceByType.latest(java.util.Arrays.asList(sameDayPlan,sameDayDone),"a").get("체인")==sameDayDone,
   "garage latest record uses just-completed cycle on exact ties");
  ok(!MaintenanceNotificationSummary.from(db,date).hasNotice(),
   "notification agrees with garage after same-day completion");
  bike.put("odo",18100);
  ok(MaintenanceNotificationSummary.from(db,date).soon==1,"next service fires at new cycle threshold");
  bad(()->ServiceCycle.completed(plan,date,-1,0),"negative odometer rejected");
  bad(()->ServiceCycle.completed(plan,date,15400,-50),"negative repair cost rejected");
  bad(()->ServiceCycle.completed(plan,"invalid",15400,50),"invalid completion date rejected");
  JSONObject noCycle=QuickRecordDraft.maintenance("a","브레이크",date,15400,20000,"",0,0);
  JSONObject marked=ServiceCycle.completed(noCycle,date,15400,0);
  ok(marked.optDouble("nextKm")==0&&marked.optString("nextDate").isEmpty(),"never invent recurring schedule from unknown interval");
  ok(ServiceCycle.calculatedFuelAmount(4.5,1750)==7875,"unit price × liters rounded to KRW");
  bad(()->ServiceCycle.calculatedFuelAmount(0,1700),"cannot calculate from missing actual litres");
  bad(()->ServiceCycle.calculatedFuelAmount(5,0),"cannot calculate from missing unit price");
  JSONObject small=QuickRecordDraft.maintenance("a","체인",date,15400,15900,"",500,0);
  small.put("alertKmBefore",500);
  ok(MaintenanceAlertWindow.kmBefore(small)==100,"500km schedule never alerts at registration");
  small.put("alertKmBefore",100);
  ok(MaintenanceAlertWindow.kmBefore(small)==100,"custom 100 preserved");
  small.put("intervalMonths",1).put("alertDaysBefore",30);
  ok(MaintenanceAlertWindow.daysBefore(small)==6,"one month interval caps overlarge warning window");
  System.out.println("PASS UX17 service completion, alert thresholds and automatic fuel total: "+checks+" assertions");
 }
}
