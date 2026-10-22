package com.motohub.next;

/** Safe live fuel-price calculator: an incomplete formula cannot leave an old auto total. */
final class FuelAutoAmount {
 private String lastAuto="";
 /** A manually reused fuel entry must no longer be treated as our generated amount. */
 void reset(){lastAuto="";}
 String update(String enteredAmount,String litersText,String priceText){
  String current=enteredAmount==null?"":enteredAmount;
  try{
   double liters=Double.parseDouble(litersText.trim().replace(",",""));
   double price=Double.parseDouble(priceText.trim().replace(",",""));
   long calculated=(long)ServiceCycle.calculatedFuelAmount(liters,price);
   lastAuto=Long.toString(calculated);
   return lastAuto;
  }catch(Exception invalid){
   boolean wasAuto=!lastAuto.isEmpty()&&lastAuto.equals(current);
   lastAuto="";
   return wasAuto?"":current;
  }
 }
}
