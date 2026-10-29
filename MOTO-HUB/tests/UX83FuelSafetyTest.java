package com.motohub.next;
public final class UX83FuelSafetyTest {
 public static void main(String[] args){
  check(FuelAnomalyPolicy.lowOdometer(3000,2980),"lower odometer");
  check(!FuelAnomalyPolicy.lowOdometer(3000,3000),"same odometer");
  check(FuelAnomalyPolicy.unusualLiters(26),"large liters");
  check(!FuelAnomalyPolicy.unusualLiters(12),"normal liters");
  check(FuelAnomalyPolicy.unusualAmount(12000,50000),"large amount");
  check(FuelAnomalyPolicy.unusualAmount(12000,900),"tiny amount");
  check(!FuelAnomalyPolicy.unusualAmount(12000,13000),"ordinary amount");
  check(FuelAnomalyPolicy.staleDate("2026-10-29","2026-10-30"),"old date");
  check(!FuelAnomalyPolicy.staleDate("2026-10-30","2026-10-30"),"today");
  System.out.println("PASS UX83FuelSafetyTest");
 }
 static void check(boolean value,String what){if(!value)throw new AssertionError(what);}
}