package com.motohub.next;

import java.time.LocalDate;
import java.util.regex.*;

/** Conservative OCR candidate extraction. It never writes to persistent records. */
final class ReceiptTextParser {
 private ReceiptTextParser(){}
 static final class Result {
  final String amount,liters,date;
  Result(String amount,String liters,String date){this.amount=amount;this.liters=liters;this.date=date;}
  boolean hasAny(){return !amount.isEmpty()||!liters.isEmpty()||!date.isEmpty();}
 }
 private static final Pattern MONEY=Pattern.compile("(?:합\\s*계|총\\s*금액|결제\\s*금액|카드\\s*결제|판매\\s*금액|TOTAL)\\s*[:：]?\\s*\u20a9?\\s*([0-9]{1,3}(?:,[0-9]{3})+|[0-9]{4,10})(?![0-9,])(?:\\s*원)?",Pattern.CASE_INSENSITIVE);
 private static final Pattern VOLUME=Pattern.compile("(?:주유\\s*량|공급\\s*량|리터|LITERS?)\\s*[:：]?\\s*([0-9]{1,4}(?:\\.[0-9]{1,3})?)\\s*(?:L|리터)?",Pattern.CASE_INSENSITIVE);
 private static final Pattern DATE=Pattern.compile("(20[0-9]{2})[./-]([0-9]{1,2})[./-]([0-9]{1,2})");
 static Result parse(String text){
  if(text==null)return new Result("","","");
  String limited=text.substring(0,Math.min(text.length(),16000));
  String money="",liters="",date="";
  Matcher m=MONEY.matcher(limited);
  while(m.find()){try{long value=Long.parseLong(m.group(1).replace(",",""));if(value>0&&value<=1_000_000_000L){money=String.valueOf(value);break;}}catch(NumberFormatException ignored){}}
  m=VOLUME.matcher(limited);
  while(m.find()){try{double value=Double.parseDouble(m.group(1));if(value>0&&value<=1000){liters=m.group(1);break;}}catch(NumberFormatException ignored){}}
  m=DATE.matcher(limited);
  while(m.find()){
   try{LocalDate parsed=LocalDate.of(Integer.parseInt(m.group(1)),Integer.parseInt(m.group(2)),Integer.parseInt(m.group(3)));
    if(!parsed.isAfter(LocalDate.now().plusDays(1))){date=parsed.toString();break;}}
   catch(Exception ignored){}
  }
  return new Result(money,liters,date);
 }
}
