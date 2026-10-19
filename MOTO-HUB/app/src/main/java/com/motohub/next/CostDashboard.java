package com.motohub.next;
/** Overflow/invalid-value-safe monthly display helpers. */
final class CostDashboard {
 private CostDashboard(){}
 static double add(double subtotal,double amount){
  if(!Double.isFinite(subtotal)||subtotal<0||!Double.isFinite(amount)||amount<0||amount>1_000_000_000)return Math.max(0,subtotal);
  return subtotal+amount>1_000_000_000?subtotal:subtotal+amount;
 }
 static int proportion(double part,double total){
  if(!Double.isFinite(part)||!Double.isFinite(total)||total<=0)return 0;
  return (int)Math.max(0,Math.min(100,Math.round(100*part/total)));
 }
}
