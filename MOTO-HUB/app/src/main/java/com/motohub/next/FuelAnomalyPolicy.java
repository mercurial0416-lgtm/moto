package com.motohub.next;
/** Conservative warnings, never silent rejection of genuine motorcycle fuel fills. */
final class FuelAnomalyPolicy {
 private FuelAnomalyPolicy(){}
 static boolean lowOdometer(double vehicleKm,double enteredKm){
  return Double.isFinite(enteredKm)&&Double.isFinite(vehicleKm)&&enteredKm+0.01<vehicleKm;
 }
 static boolean unusualLiters(double liters){return Double.isFinite(liters)&&liters>25;}
 static boolean unusualAmount(double previous,double entered){
  return Double.isFinite(previous)&&previous>=3000&&Double.isFinite(entered)
   &&(entered>previous*3.5||entered<previous*0.12);
 }
 static boolean staleDate(String date,String today){
  return date!=null&&today!=null&&!date.isEmpty()&&!date.equals(today);
 }
}