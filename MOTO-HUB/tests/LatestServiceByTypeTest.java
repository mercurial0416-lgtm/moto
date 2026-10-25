package com.motohub.next;

import java.util.*;
import org.json.JSONObject;

/** Regression: reminders must follow the latest record, not insertion order. */
public final class LatestServiceByTypeTest {
 private static int n;
 private static void check(boolean condition,String label){n++;if(!condition)throw new AssertionError(label);}
 private static JSONObject row(String bike,String type,String date,double km,double due){
  return new JSONObject().put("vehicleId",bike).put("type",type).put("date",date)
   .put("mileage",km).put("nextKm",due);
 }
 public static void main(String[] args){
  JSONObject old=row("cub","엔진오일","2026-01-10",1000,3000);
  JSONObject newer=row("cub","엔진오일","2026-10-10",4800,6800);
  JSONObject newest=row("cub","엔진오일","2026-10-10",5100,7100);
  JSONObject cleared=row("cub","체인","2026-10-11",5400,0);
  JSONObject staleChain=row("cub","체인","2026-08-01",2000,3500);
  JSONObject other=row("duke","엔진오일","2026-12-01",10000,12000);
  Map<String,JSONObject> result=LatestServiceByType.latest(
   Arrays.asList(newer,cleared,other,old,staleChain,newest),"cub");
  check(result.size()==2,"only own service types");
  check(result.get("엔진오일")==newest,"latest on tied date uses higher odometer");
  check(result.get("체인")==cleared,"later no-reminder record must clear stale due warning");
  check(LatestServiceByType.latest(Arrays.asList(old,newer),"cub").get("엔진오일")==newer,"ascending input");
  check(LatestServiceByType.latest(Arrays.asList(newer,old),"cub").get("엔진오일")==newer,"descending input");
  check(LatestServiceByType.latest(Arrays.asList(other),"cub").isEmpty(),"other bike isolated");
  check(LatestServiceByType.latest(null,"cub").isEmpty(),"null list safe");
  check(LatestServiceByType.latest(Arrays.asList(old),null).isEmpty(),"null bike safe");
  check(LatestServiceByType.latest(Arrays.asList(row("cub"," ","2026-10-20",10,100)),"cub").isEmpty(),"empty service name ignored");
  check(LatestServiceByType.latest(Arrays.asList(old,old),"cub").get("엔진오일")==old,"stable equal record");
  JSONObject registered=row("cub","체인","2026-10-22",6000,6050);
  JSONObject completed=row("cub","체인","2026-10-22",6000,9000);
  check(LatestServiceByType.latest(Arrays.asList(registered,completed),"cub").get("체인")==completed,
   "same-day same-mileage completion supersedes earlier plan");
  check(LatestServiceByType.latest(Arrays.asList(completed,registered),"cub").get("체인")==registered,
   "exact ties follow stored append order, not due distance");
  System.out.println("PASS garage latest-service reminder: "+n+" checks");
 }
}
