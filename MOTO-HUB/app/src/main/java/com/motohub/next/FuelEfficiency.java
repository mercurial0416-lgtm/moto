package com.motohub.next;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.json.JSONObject;

/** Full-to-full real tank calculation; never treats GPS distance as odometer. */
final class FuelEfficiency {
 private FuelEfficiency(){}
 static final class Result {
  final double km,liters,kmPerLiter,costPerKm;
  final int trips;
  Result(double distance,double volume,double price,int visits){km=distance;liters=volume;kmPerLiter=distance/volume;costPerKm=price/distance;trips=visits;}
 }
 static Result forBike(List<JSONObject> expenses,String id){
  List<JSONObject> fuels=new ArrayList<>();
  if(expenses==null||id==null)return null;
  for(JSONObject x:expenses)if(x!=null&&id.equals(x.optString("vehicleId"))&&"주유".equals(x.optString("type"))){fuels.add(x);}
  fuels.sort(Comparator.comparing((JSONObject x)->x.optString("date")).thenComparingDouble(x->x.optDouble("mileage")));
  double startKm=-1,liters=0,cost=0,totalKm=0,totalLiters=0,totalCost=0;
  int visits=0;
  for(JSONObject fuel:fuels){
   double odo=fuel.optDouble("mileage",Double.NaN),volume=fuel.optDouble("liters",Double.NaN),price=fuel.optDouble("amount",Double.NaN);
   if(!Double.isFinite(odo)||!Double.isFinite(volume)||!Double.isFinite(price)||odo<0||volume<=0||volume>1000||price<0){startKm=-1;liters=cost=0;continue;}
   if(startKm>=0&&odo<=startKm){startKm=-1;liters=cost=0;continue;}
   if(startKm>=0){liters+=volume;cost+=price;}
   if(fuel.optString("fullTank").equals("true")){
    if(startKm>=0&&odo>startKm&&liters>0){
     double distance=odo-startKm;if(distance<3000*liters&&distance>=1){totalKm+=distance;totalLiters+=liters;totalCost+=cost;visits++;}
    }
    startKm=odo;liters=cost=0;
   }
  }
  return totalKm>0&&totalLiters>0?new Result(totalKm,totalLiters,totalCost,visits):null;
 }
}
