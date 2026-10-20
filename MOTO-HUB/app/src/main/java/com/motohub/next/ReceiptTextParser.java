package com.motohub.next;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Conservative OCR suggestions. Never saves a record without an explicit user review. */
final class ReceiptTextParser {
 private ReceiptTextParser(){}
 static final class Guess {
  final String date,amount,liters,unitPrice;
  Guess(String date,String amount,String liters,String unitPrice){this.date=date;this.amount=amount;this.liters=liters;this.unitPrice=unitPrice;}
  boolean useful(){return !date.isEmpty()||!amount.isEmpty()||!liters.isEmpty()||!unitPrice.isEmpty();}
 }
 private static final Pattern DATE=Pattern.compile("(?<!\\d)(20\\d{2})[./-]\\s*(0?[1-9]|1[0-2])[./-]\\s*(0?[1-9]|[12]\\d|3[01])(?!\\d)");
 private static final Pattern MONEY=Pattern.compile("(?:합\\s*계|총\\s*결제|결제\\s*금액|결제금액|청구금액|받을금액|총액|TOTAL)\\s*[:：]?\\s*(?:₩|￦|원)?\\s*([0-9][0-9,]{1,11})",Pattern.CASE_INSENSITIVE);
 private static final Pattern LITERS=Pattern.compile("(?:주유량|주입량|수량|LITER[S]?|리터)\\s*[:：]?\\s*([0-9]{1,3}(?:[.]\\d{1,3})?)\\s*(?:L|ℓ|리터)?",Pattern.CASE_INSENSITIVE);
 private static final Pattern PRICE=Pattern.compile("(?:단가|리터당|L당|L\\s*/\\s*원|UNIT\\s*PRICE)\\s*[:：]?\\s*([0-9][0-9,]{2,6})",Pattern.CASE_INSENSITIVE);
 static Guess parse(String raw){
  if(raw==null||raw.isEmpty())return new Guess("","","","");
  String text=raw.length()>10000?raw.substring(0,10000):raw;
  String date="";Matcher dm=DATE.matcher(text);
  while(dm.find())try{LocalDate d=LocalDate.of(Integer.parseInt(dm.group(1)),Integer.parseInt(dm.group(2)),Integer.parseInt(dm.group(3)));
   if(!d.isBefore(LocalDate.of(2000,1,1))&&!d.isAfter(LocalDate.now().plusDays(1))){date=d.toString();break;}
  }catch(Exception ignored){}
  return new Guess(date,number(text,MONEY,1,1_000_000_000),number(text,LITERS,0.01,1000),number(text,PRICE,100,100_000));
 }
 private static String number(String text,Pattern regex,double min,double max){
  Matcher m=regex.matcher(text);while(m.find())try{
   String n=m.group(1).replace(",","");double value=Double.parseDouble(n);
   if(Double.isFinite(value)&&value>=min&&value<=max)return n;
  }catch(NumberFormatException ignored){}
  return "";
 }
}
