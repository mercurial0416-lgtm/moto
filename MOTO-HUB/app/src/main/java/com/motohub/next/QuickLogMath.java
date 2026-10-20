package com.motohub.next;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Explicit, reversible estimates for quick fuel entry; no price is silently assumed. */
final class QuickLogMath {
 private QuickLogMath(){}
 static double positive(String input,double max){
  if(input==null)return Double.NaN;
  try{double result=Double.parseDouble(input.trim().replace(",",""));
   return Double.isFinite(result)&&result>0&&result<=max?result:Double.NaN;
  }catch(NumberFormatException e){return Double.NaN;}
 }
 static long unitPrice(double amount,double liters){
  if(!Double.isFinite(amount)||!Double.isFinite(liters)||amount<=0||liters<=0||amount>1e9||liters>1000)return 0;
  return Math.round(amount/liters);
 }
 static String litersFromAmount(String amount,String unitPrice){
  double money=positive(amount,1e9),price=positive(unitPrice,100000);
  if(!Double.isFinite(money)||!Double.isFinite(price))return "";
  double liters=money/price;
  if(liters<=0||liters>1000)return "";
  BigDecimal rounded=BigDecimal.valueOf(liters).setScale(2,RoundingMode.HALF_UP);
  return rounded.signum()>0?rounded.stripTrailingZeros().toPlainString():"";
 }
 static String amountFromLiters(String liters,String unitPrice){
  double volume=positive(liters,1000),price=positive(unitPrice,100000);
  if(!Double.isFinite(volume)||!Double.isFinite(price))return "";
  double money=volume*price;
  if(money>1e9||money<1)return "";
  return String.valueOf(Math.round(money));
 }
}
