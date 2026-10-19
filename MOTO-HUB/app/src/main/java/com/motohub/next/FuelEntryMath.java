package com.motohub.next;
final class FuelEntryMath {
 private FuelEntryMath(){}
 static String unitPrice(String amount,String liters){
  try{double a=Double.parseDouble(amount.trim()),l=Double.parseDouble(liters.trim());
   if(!Double.isFinite(a)||!Double.isFinite(l)||a<0||l<=0||a>1e12)return "금액·주유량을 입력하면 리터당 가격을 표시합니다";
   return String.format(java.util.Locale.KOREA,"리터당 약 %,.0f원",a/l);
  }catch(Exception e){return "금액·주유량을 입력하면 리터당 가격을 표시합니다";}
 }
}
