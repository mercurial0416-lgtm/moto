package com.motohub.next;
import java.time.LocalDate;
import org.json.JSONObject;
import org.json.JSONArray;

/** Data and notification behavior must survive the shorter UI. */
public final class Ux16QuickLogMaintenanceTest {
 static int checks;
 static void ok(boolean v,String message){checks++;if(!v)throw new AssertionError(message);}
 static void invalid(Runnable call,String message){
  try{call.run();throw new AssertionError(message);}
  catch(IllegalArgumentException expected){checks++;}
 }
 static void throwsException(Unsafe call,String message){
  try{call.run();throw new AssertionError(message);}
  catch(IllegalArgumentException expected){checks++;}
  catch(Exception other){throw new AssertionError(message,other);}
 }
 interface Unsafe {void run()throws Exception;}
 public static void main(String[] ignored)throws Exception{
  String today="2026-10-22";
  JSONObject db=Store.empty();
  JSONObject bike=new JSONObject().put("id","bike1").put("name","슈퍼커브 110").put("odo",7500);
  db.getJSONArray("vehicles").put(bike);
  JSONObject fuel=QuickRecordDraft.fuel("bike1",today,7530,12500,4.5,true);
  ok("주유".equals(fuel.getString("type")),"fuel type");
  ok(fuel.getDouble("liters")==4.5&&fuel.getDouble("amount")==12500,"fuel actual data");
  ok(fuel.getBoolean("fullTank"),"full tank independent choice");
  ok(fuel.getDouble("mileage")==7530,"fuel odometer stored");
  Store.upsert(db,"expenses",fuel);
  JSONObject working=Store.parse(db.toString());
  ok(working.getJSONArray("expenses").length()==1,"valid quick fuel survives Store.parse");
  throwsException(()->QuickRecordDraft.fuel("bike1",today,7530,0,4,true),"zero amount rejected");
  throwsException(()->QuickRecordDraft.fuel("bike1",today,7530,12500,0,true),"missing liters rejected");
  throwsException(()->QuickRecordDraft.fuel("bike1",today,-10,12500,4,true),"negative odometer rejected");
  throwsException(()->QuickRecordDraft.fuel("",today,7530,12500,4,true),"no phantom bike");
  throwsException(()->QuickRecordDraft.fuel("bike1","bad-date",7530,12500,4,true),"bad date rejected");
  ok(QuickRecordDraft.nextOdometer(7530,1000)==8530,"relative next due becomes absolute");
  ok(QuickRecordDraft.dueAfterMonths(today,3).equals("2027-01-22"),"month rollover");
  ok(QuickRecordDraft.dueAfterMonths("2026-01-31",1).equals("2026-02-28"),"month end normalization");
  ok(QuickRecordDraft.nextOdometer(7530,0)==0&&QuickRecordDraft.dueAfterMonths(today,0).isEmpty(),"optional independent interval dimensions");
  JSONObject schedule=QuickRecordDraft.maintenance("bike1","엔진오일",today,7530,8030,"",500,0);
  ok(schedule.optBoolean("scheduleOnly")&&schedule.optDouble("cost")==0,"plan not a paid service");
  ok(schedule.optDouble("nextKm")==8030&&schedule.optString("nextDate").isEmpty(),"km-only setup");
  Store.upsert(db,"services",schedule);
  Store.parse(db.toString());
  MaintenanceNotificationSummary none=MaintenanceNotificationSummary.from(db,today);
  ok(none.soon==1&&none.due==0,"within 500km alerts");
  ok(none.detail.contains("엔진오일")&&none.detail.contains("500 km 남음"),"actual type and remaining distance shown");
  ok(none.title().contains("곧"),"actionable notification title");
  JSONObject revised=QuickRecordDraft.maintenance("bike1","엔진오일",today,7530,12000,"",3000,0);
  Store.upsert(db,"services",revised);
  ok(!MaintenanceNotificationSummary.from(db,today).hasNotice(),"latest record replaces same-day mileage plan");
  JSONObject dateOnly=QuickRecordDraft.maintenance("bike1","체인",today,7530,0,"2026-11-15",0,1);
  Store.upsert(db,"services",dateOnly);
  ok(MaintenanceNotificationSummary.from(db,today).soon==1,"date within 30 days alerts");
  JSONObject tooFar=QuickRecordDraft.maintenance("bike1","배터리",today,7530,0,"2027-10-22",0,12);
  Store.upsert(db,"services",tooFar);
  ok(MaintenanceNotificationSummary.from(db,today).soon==1,"future unrelated service does not alert");
  JSONObject overdue=QuickRecordDraft.maintenance("bike1","타이어","2026-10-01",7500,7700,"2026-10-18",200,0);
  Store.upsert(db,"services",overdue);
  MaintenanceNotificationSummary summary=MaintenanceNotificationSummary.from(db,today);
  ok(summary.due==1&&summary.soon==1,"separate due and soon counts");
  ok(summary.title().contains("지났어요")&&summary.detail.contains("타이어"),"overdue outranks soon");
  String before=summary.fingerprint;
  JSONObject newService=QuickRecordDraft.maintenance("bike1","타이어",today,7530,8700,"2027-01-22",1000,3);
  Store.upsert(db,"services",newService);
  MaintenanceNotificationSummary updated=MaintenanceNotificationSummary.from(db,today);
  ok(updated.due==0&&updated.soon==1,"new service cancels prior due reminder");
  ok(!before.equals(updated.fingerprint),"state dedup detects changed maintenance plans");
  throwsException(()->QuickRecordDraft.maintenance("bike1","체인",today,7530,0,"",0,0),"must choose some interval");
  throwsException(()->QuickRecordDraft.maintenance("bike1","체인",today,7530,7500,"",0,0),"due km past current");
  throwsException(()->QuickRecordDraft.maintenance("bike1","체인",today,7530,0,"2026-01-01",0,3),"past due date rejected");
  db.getJSONArray("vehicles").getJSONObject(0).put("soldDate",today);
  ok(!MaintenanceNotificationSummary.from(db,today).hasNotice(),"sold bikes never trigger alerts");
  System.out.println("PASS UX16 quick refuel + maintenance alert behavior: "+checks+" assertions");
 }
}

 }
}
