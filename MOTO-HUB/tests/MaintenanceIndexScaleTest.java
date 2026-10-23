package com.motohub.next;
import org.json.JSONArray;
import org.json.JSONObject;
/** Busy local garages must retain the same due/soon meanings without quadratic scans. */
public final class MaintenanceIndexScaleTest {
 static int checks;
 static void ok(boolean v,String message){checks++;if(!v)throw new AssertionError(message);}
 static JSONObject bike(String id,int odo,boolean sold)throws Exception{
  return new JSONObject().put("id",id).put("name",id).put("odo",odo)
   .put("soldDate",sold?"2026-10-01":"");
 }
 static JSONObject service(String id,String bike,String type,String date,String due,int nextKm)throws Exception{
  return new JSONObject().put("id",id).put("vehicleId",bike).put("type",type)
   .put("date",date).put("mileage",5000).put("nextDate",due).put("nextKm",nextKm)
   .put("alertDaysBefore",15).put("alertKmBefore",300);
 }
 public static void main(String[] args)throws Exception{
  JSONObject db=Store.empty();
  JSONArray bikes=db.getJSONArray("vehicles"),services=db.getJSONArray("services");
  bikes.put(bike("a",9800,false));
  bikes.put(bike("b",9800,true));
  services.put(service("old","a","엔진오일","2026-09-01","2026-10-01",9900));
  services.put(service("new","a","엔진오일","2026-10-19","2026-10-25",10000));
  services.put(service("sold","b","엔진오일","2026-10-01","2026-10-01",9500));
  MaintenanceNotificationSummary first=MaintenanceNotificationSummary.from(db,"2026-10-23");
  ok(first.due==0&&first.soon==1,"latest active bike only, prior overdue ignored");
  ok(first.detail.contains("엔진오일"),"specific service preserved");
  String fingerprint=first.fingerprint;
  bikes.getJSONObject(0).put("odo",9850);
  ok(MaintenanceNotificationSummary.from(db,"2026-10-23").fingerprint.equals(fingerprint),
   "odometer changes in same SOON range never reset throttle");
  services.getJSONObject(1).put("nextDate","2026-10-26");
  MaintenanceNotificationSummary changedDate=MaintenanceNotificationSummary.from(db,"2026-10-23");
  ok(changedDate.soon==1&&!changedDate.fingerprint.equals(fingerprint),
   "rescheduled date updates identity while still soon");
  String changedFingerprint=changedDate.fingerprint;
  services.getJSONObject(1).put("nextKm",10100);
  ok(!MaintenanceNotificationSummary.from(db,"2026-10-23").fingerprint.equals(changedFingerprint),
   "changed distance deadline updates notification identity");
  for(int i=0;i<250;i++){
   String id="bulk-"+i;
   bikes.put(bike(id,1000,false));
   for(int j=0;j<30;j++)
    services.put(service(id+"-record-"+j,id,"cycle-"+(j%3),"2026-10-18","2027-09-01",50000));
  }
  MaintenanceNotificationSummary scaled=MaintenanceNotificationSummary.from(db,"2026-10-23");
  ok(scaled.due==0&&scaled.soon==1,"7,500 extra records leave one true notice");
  ok(scaled.fingerprint.equals(MaintenanceNotificationSummary.from(db,"2026-10-23").fingerprint),
   "large summary remains deterministic");
  System.out.println("PASS maintenance indexing and reschedule: "+checks+" assertions");
 }
}
