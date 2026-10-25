package com.motohub.next;

/** Safe live fuel-price calculator: an incomplete formula cannot leave an old auto total. */
final class FuelAutoAmount {
 private String lastAuto="";
 private boolean manuallyEdited;
 /** A manually reused fuel entry must no longer be treated as our generated amount. */
 void reset(){lastAuto="";manuallyEdited=false;}
 /** Called for human input as well as setText from a calculation.
  * Do not overwrite a user's correction while the calculator remains open. */
 void noteAmountEdited(String amount){
  String current=amount==null?"":amount.trim();
  if(!lastAuto.isEmpty()&&!lastAuto.equals(current)){
   manuallyEdited=!current.isEmpty();
   lastAuto="";
  }else if(current.isEmpty()){
   manuallyEdited=false;
  }
 }
 String update(String enteredAmount,String litersText,String priceText){
  String current=enteredAmount==null?"":enteredAmount;
  if(manuallyEdited)return current;
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
