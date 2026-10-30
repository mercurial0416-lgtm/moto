package com.motohub.next;

import java.util.*;
import org.json.JSONObject;

/** Estimate local per-bike fuel expenses and full-tank intervals without network or ECU access. */
final class FuelStats {
 final int fills,fullIntervals;
 final double liters,cost,averageWonPerLiter,kmPerLiter;
 private FuelStats(int fills,int fullIntervals,double liters,double cost,double avg,double kmPerLiter){
  this.fills=fills;this.fullIntervals=fullIntervals;this.liters=liters;this.cost=cost;
  averageWonPerLiter=avg;this.kmPerLiter=kmPerLiter;
 }
 static FuelStats forBike(List<JSONObject> records,String bikeId){
  if(bikeId==null||bikeId.isEmpty())return new FuelStats(0,0,0,0,0,Double.NaN);
  List<JSONObject> filtered=new ArrayList<>();
  if(records!=null)for(JSONObject x:records){
   if(x==null||!bikeId.equals(x.optString("vehicleId"))||!"주유".equals(x.optString("type")))continue;
   double amount=x.optDouble("amount",Double.NaN),liters=x.optDouble("liters",Double.NaN);
   if(!Double.isFinite(amount)||!Double.isFinite(liters)||amount<=0||liters<=0||amount>1e9||liters>1000)continue;
   filtered.add(x);
  }
  filtered.sort(Comparator.comparing((JSONObject x)->x.optString("date")).thenComparingDouble(x->x.optDouble("mileage")));
  double sumL=0,sumC=0,rangeL=0,sumKm=0,intervalLiters=0;double lastFullKm=Double.NaN;int intervals=0;
  for(JSONObject x:filtered){
   double liters=x.optDouble("liters"),amount=x.optDouble("amount"),km=x.optDouble("mileage",Double.NaN);
   sumL+=liters;sumC+=amount;
   if(Double.isFinite(lastFullKm))rangeL+=liters;
   if(x.optBoolean("fullTank")){
    if(Double.isFinite(lastFullKm)&&Double.isFinite(km)){
     double distance=km-lastFullKm;
     if(distance>0.1&&distance<=2500&&rangeL>0&&rangeL<=150&&distance/rangeL>=1&&distance/rangeL<=80){
      intervals++;sumKm+=distance;intervalLiters+=rangeL;
     }
    }
    lastFullKm=Double.isFinite(km)&&km>=0?km:Double.NaN;rangeL=0;
   }
  }
  return new FuelStats(filtered.size(),intervals,sumL,sumC,sumL>0?sumC/sumL:0,
   intervalLiters>0?sumKm/intervalLiters:Double.NaN);
 }
}
