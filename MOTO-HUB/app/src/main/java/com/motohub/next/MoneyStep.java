package com.motohub.next;
/** Add/subtract practical cash increments without overwriting a typed purchase price. */
final class MoneyStep {
 private MoneyStep(){}
 static long apply(String raw,long delta){
  String text=raw==null?"":raw.trim().replace(",","");
  long current;
  try{current=text.isEmpty()?0:Long.parseLong(text);}
  catch(NumberFormatException invalid){throw new IllegalArgumentException("금액을 숫자로 입력해 주세요.");}
  if(current<0||current>10_000_000_000_000L)throw new IllegalArgumentException("금액 범위를 확인해 주세요.");
  long changed;
  try{changed=Math.addExact(current,delta);}
  catch(ArithmeticException overflow){throw new IllegalArgumentException("금액이 너무 큽니다.");}
  return Math.max(0,Math.min(10_000_000_000_000L,changed));
 }
}