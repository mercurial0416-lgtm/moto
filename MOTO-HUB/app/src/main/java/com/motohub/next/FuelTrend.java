package com.motohub.next;
import java.util.*;
import org.json.JSONObject;
/** Local-only fuel statistics; odometer distance and full-tank estimates are never conflated. */
final class FuelTrend {
 private FuelTrend(){}
 static final class Summary {
  final int entries;final double liters,totalCost,unitPrice,fullTankKmPerLiter;
  final int fullTankSegments;
  Summary(int entries,double liters,double cost,int segments,double economy){this.entries=entries;this.liters=liters;totalCost=cost;unitPrice=liters>0?cost/liters:0;fullTankSegments=segments;fullTankKmPerLiter=economy;}
 }
 static Summary forBike(List<JSONObject> expenses,String bike){
  ArrayList<JSONObject> valid=new ArrayList<>();double liters=0,cost=0;int totalFills=0;
  for(JSONObject item:expenses){
   if(!bike.equals(item.optString("vehicleId"))||!"주유".equals(item.optString("type")))continue;
   double l=item.optDouble("liters",0),c=item.optDouble("amount",0),odo=item.optDouble("mileage",-1);
   if(!Double.isFinite(l)||!Double.isFinite(c)||l<=0||l>200||c<0||c>10_000_000)continue;
   liters+=l;cost+=c;totalFills++;if(Double.isFinite(odo)&&odo>=0)valid.add(item);
  }
  valid.sort(Comparator.comparingDouble((JSONObject j)->j.optDouble("mileage",0)).thenComparing(j->j.optString("date")));
  // Between full tanks, count the fuel used AFTER the starting fill, through the ending fill.
  double rolling=0,totalDist=0,totalFuel=0;double startOdo=Double.NaN;int segments=0;
  for(JSONObject v:valid){double odo=v.optDouble("mileage",-1),l=v.optDouble("liters",0);boolean full=v.optBoolean("fullTank",false);
   if(!Double.isFinite(startOdo)){if(full){startOdo=odo;rolling=0;}continue;}
   if(odo<startOdo){startOdo=Double.NaN;rolling=0;continue;}
   rolling+=l;
   if(full){double distance=odo-startOdo;if(distance>0&&distance<=5000&&rolling>0&&rolling<=300){totalDist+=distance;totalFuel+=rolling;segments++;}startOdo=odo;rolling=0;}
  }
  return new Summary(totalFills,liters,cost,segments,totalFuel>0?totalDist/totalFuel:0);
 }
}
