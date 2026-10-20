package com.motohub.next;
import java.util.*;
import java.util.regex.*;
import java.time.*;
/** Untrusted OCR hints, never silently overwrite or save user data. */
final class ReceiptTextHints {
 final String date,amount,liters;final boolean recognized;
 private ReceiptTextHints(String date,String amount,String liters){this.date=date;this.amount=amount;this.liters=liters;recognized=!date.isEmpty()||!amount.isEmpty()||!liters.isEmpty();}
 private static final Pattern MONEY=Pattern.compile("(?i)(?:총\\s*금액|결제\\s*금액|승인\\s*금액|합\\s*계|총\\s*액|판매\\s*금액|결제)(?:[^0-9\\n]{0,15})([0-9][0-9,]{2,8})(?:\\s*원)?");
 private static final Pattern LITER=Pattern.compile("(?i)(?:주유량|급유량|수량|리터|LITER|VOLUME)\\s*[:：]?\\s*([0-9]{1,3}(?:\\.[0-9]{1,3})?)\\s*(?:L|ℓ|리터)?");
 private static final Pattern DATE=Pattern.compile("(?<![0-9])((?:20)[0-9]{2})[./-]([0-9]{1,2})[./-]([0-9]{1,2})(?![0-9])");
 private static final Pattern KRW=Pattern.compile("([0-9][0-9,]{2,8})\\s*원");
 static ReceiptTextHints parse(String text){
  if(text==null)return new ReceiptTextHints("","","");String source=text.length()>12000?text.substring(0,12000):text;
  String date="",amount="",liters="";
  Matcher d=DATE.matcher(source);while(d.find()){try{LocalDate day=LocalDate.of(Integer.parseInt(d.group(1)),Integer.parseInt(d.group(2)),Integer.parseInt(d.group(3)));if(!day.isAfter(LocalDate.now().plusDays(1))&&!day.isBefore(LocalDate.of(2000,1,1))){date=day.toString();break;}}catch(Exception ignored){}}
  Matcher m=MONEY.matcher(source);if(m.find())amount=validMoney(m.group(1));
  if(amount.isEmpty()){Matcher k=KRW.matcher(source);while(k.find()){String candidate=validMoney(k.group(1));if(!candidate.isEmpty()&&(amount.isEmpty()||Long.parseLong(candidate)>Long.parseLong(amount)))amount=candidate;}}
  Matcher l=LITER.matcher(source);if(l.find()){try{double n=Double.parseDouble(l.group(1));if(Double.isFinite(n)&&n>0&&n<=1000)liters=String.valueOf(n);}catch(Exception ignored){}}
  return new ReceiptTextHints(date,amount,liters);
 }
 private static String validMoney(String input){try{long n=Long.parseLong(input.replace(",",""));return n>=100&&n<=100_000_000?String.valueOf(n):"";}catch(Exception e){return "";}}
}
