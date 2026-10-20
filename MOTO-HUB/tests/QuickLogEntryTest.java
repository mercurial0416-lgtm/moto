package com.motohub.next;
import org.json.JSONObject;
import java.util.*;
public final class QuickLogEntryTest {
 private static int checks;
 private static void ok(boolean condition,String msg){checks++;if(!condition)throw new AssertionError(msg);}
 private static JSONObject fuel(String id,String bike,String date,double km,String type,double amount,double liters){
  return new JSONObject().put("id",id).put("vehicleId",bike).put("date",date).put("mileage",km)
   .put("type",type).put("amount",amount).put("liters",liters);
 }
 private static JSONObject service(String id,String bike,String date,double km,String type,double cost,double nextKm){
  return new JSONObject().put("id",id).put("vehicleId",bike).put("date",date).put("mileage",km)
   .put("type",type).put("cost",cost).put("nextKm",nextKm);
 }
 public static void main(String[] args){
  ok("5".equals(QuickLogMath.litersFromAmount("10,000","2000")),"comma price input");
  ok("4.55".equals(QuickLogMath.litersFromAmount("10000","2200")),"round 2 decimals");
  ok("10400".equals(QuickLogMath.amountFromLiters("5.2","2000")),"amount computed");
  ok(QuickLogMath.unitPrice(10000,4.5)==2222,"latest fuel unit price");
  for(String bad:new String[]{"", "0", "-1", "NaN", "Infinity", "NaN xyz", "  "}){
   ok(QuickLogMath.litersFromAmount(bad,"2000").isEmpty(),"reject bad amount "+bad);
   ok(QuickLogMath.amountFromLiters(bad,"2000").isEmpty(),"reject bad liters "+bad);
  }
  ok(QuickLogMath.litersFromAmount("10000","0").isEmpty(),"reject zero unit");
  ok(QuickLogMath.litersFromAmount("10000","Infinity").isEmpty(),"reject infinity unit");
  ok(QuickLogMath.litersFromAmount("1000000000","1").isEmpty(),"reject volume above limit");
  ok(QuickLogMath.amountFromLiters("1000","100000").isEmpty()==false,"valid at max");
  ok(QuickLogMath.amountFromLiters("2000","3000").isEmpty(),"reject too many liters");
  ok(QuickLogMath.unitPrice(0,3)==0,"no unit for zero money");
  ok(QuickLogMath.unitPrice(Double.POSITIVE_INFINITY,3)==0,"no unit for infinite money");
  JSONObject a=fuel("a","cub","2026-10-20",500,"주유",10000,5);
  JSONObject b=fuel("b","cub","2026-10-20",500,"주유",10000,5);
  ok(QuickLogDuplicate.same(a,b,"expenses"),"detect same fuel");
  ok(!QuickLogDuplicate.same(a,a,"expenses"),"exclude self edit");
  ok(!QuickLogDuplicate.same(a,fuel("c","duke","2026-10-20",500,"주유",10000,5),"expenses"),"different bike");
  ok(!QuickLogDuplicate.same(a,fuel("c","cub","2026-10-21",500,"주유",10000,5),"expenses"),"different date");
  ok(!QuickLogDuplicate.same(a,fuel("c","cub","2026-10-20",501,"주유",10000,5),"expenses"),"different odometer");
  ok(!QuickLogDuplicate.same(a,fuel("c","cub","2026-10-20",500,"주유",12000,5),"expenses"),"different amount");
  ok(!QuickLogDuplicate.same(a,fuel("c","cub","2026-10-20",500,"주유",10000,6),"expenses"),"different liters");
  ok(!QuickLogDuplicate.same(a,fuel("c","cub","2026-10-20",500,"보험",10000,5),"expenses"),"different kind");
  ok(QuickLogDuplicate.exists(Arrays.asList(a),b,"expenses"),"found duplicate");
  JSONObject s1=service("s1","cub","2026-02-10",1000,"엔진오일",20000,3000);
  JSONObject s2=service("s2","cub","2026-04-11",2800,"엔진오일",25000,4800);
  JSONObject sOther=service("s3","duke","2026-08-01",5000,"엔진오일",30000,6000);
  JSONObject sType=service("s4","cub","2026-09-01",4000,"타이어",210000,0);
  JSONObject pick=QuickServiceHistory.latest(Arrays.asList(s1,s2,sOther,sType),"cub","엔진오일");
  ok(pick==s2,"latest same bike and type");
  ok(QuickServiceHistory.latest(Arrays.asList(sOther),"cub","엔진오일")==null,"no other bike history");
  ok(QuickServiceHistory.latest(Arrays.asList(s1),"cub","타이어")==null,"no mismatched service type");
  ok(RecentRecordReuse.nextServiceKm(2800,4800,5100)==7100,"preserve interval relative to current km");
  JSONObject sSame=service("s5","cub","2026-04-11",2800,"엔진오일",25000,7000);
  ok(QuickLogDuplicate.same(s2,sSame,"services"),"service duplicate ignores future interval");
  ok(!QuickLogDuplicate.same(s2,service("s6","cub","2026-04-11",2800,"엔진오일",28000,4800),"services"),"different service cost");
  ok(QuickLogDuplicate.same(fuel("","cub","2026-10-20",500,"주유",10000,5),
   fuel("","cub","2026-10-20",500,"주유",10000,5),"expenses"),"two blank IDs still warn");
  ok(!QuickLogDuplicate.same(fuel("","cub","2026-10-20",500,"주유",Double.NaN,5),b,"expenses"),"NaN amount never matches");
  ok(!QuickLogDuplicate.same(fuel("","cub","2026-10-20",Double.POSITIVE_INFINITY,"주유",10000,5),b,"expenses"),"infinite odometer never matches");
  ok(!QuickLogDuplicate.same(new JSONObject().put("vehicleId","cub").put("type","주유").put("date","2026-10-20"),b,"expenses"),"incomplete record cannot warn");
  ok(!QuickLogDuplicate.same(a,b,null),"null collection safe");
  ok(!QuickLogDuplicate.same(null,b,"expenses"),"null row safe");
  ok(QuickLogDuplicate.same(service("","cub","2026-10-20",500,"엔진오일",0,1000),
   service("","cub","2026-10-20",500,"엔진오일",0,2000),"services"),"zero-cost DIY service can duplicate");
  ok(!QuickLogDuplicate.same(a,b,"vehicles"),"unsupported collections ignored");
  System.out.println("PASS quick-log safety/math/history: "+checks+" assertions");
 }
}
