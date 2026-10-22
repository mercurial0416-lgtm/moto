package com.motohub.next;
import java.util.*;
import org.json.JSONObject;
/** Regression tests for selecting the last usable refuel, never another bike's expense. */
public final class RecentFuelRecordTest {
 static int checks;
 static void ok(boolean valid,String message){checks++;if(!valid)throw new AssertionError(message);}
 static JSONObject f(String bike,String date,double km,double won,double litres){
  return new JSONObject().put("vehicleId",bike).put("type","주유").put("date",date)
   .put("mileage",km).put("amount",won).put("liters",litres);
 }
 public static void main(String[] args){
  JSONObject first=f("cub","2026-09-01",1000,6000,3.0);
  JSONObject latest=f("cub","2026-10-22",1200,9000,4.2);
  JSONObject other=f("duke","2026-10-25",500,45000,13);
  JSONObject bad=f("cub","2026-11-01",1500,0,5);
  JSONObject sameDateHigher=f("cub","2026-10-22",1250,10000,4.5);
  JSONObject tiedLast=f("cub","2026-10-22",1250,10100,4.6);
  ok(RecentFuelRecord.latest(Arrays.asList(latest,first),"cub")==latest,"reverse order chooses latest");
  ok(RecentFuelRecord.latest(Arrays.asList(first,latest),"cub")==latest,"append order chooses latest");
  ok(RecentFuelRecord.latest(Arrays.asList(first,other,latest),"cub")==latest,"other bike never reused");
  ok(RecentFuelRecord.latest(Arrays.asList(first,latest,bad),"cub")==latest,"invalid latest skipped");
  ok(RecentFuelRecord.latest(Arrays.asList(first,latest,sameDateHigher),"cub")==sameDateHigher,"date tie uses odometer");
  ok(RecentFuelRecord.latest(Arrays.asList(sameDateHigher,tiedLast),"cub")==tiedLast,"exact tie uses last saved");
  ok(RecentFuelRecord.latest(Arrays.asList(other),"cub")==null,"no match returns null");
  ok(RecentFuelRecord.latest(null,"cub")==null,"null records safe");
  ok(RecentFuelRecord.latest(Arrays.asList(latest),"")==null,"blank owner never matches");
  System.out.println("PASS recent fuel lookup: "+checks+" assertions");
 }
}
