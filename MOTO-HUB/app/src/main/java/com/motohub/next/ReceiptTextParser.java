package com.motohub.next;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.*;

/** Local-only OCR candidate extraction. Values are suggestions and never persist without approval. */
final class ReceiptTextParser {
 private ReceiptTextParser(){}
 static final class Result {
  final String amount,liters,date;
  final List<String> amountCandidates;
  Result(String amount,String liters,String date){this(amount,liters,date,amount.isEmpty()?Collections.emptyList():Collections.singletonList(amount));}
  Result(String amount,String liters,String date,List<String> candidates){
   this.amount=amount;this.liters=liters;this.date=date;
   amountCandidates=Collections.unmodifiableList(new ArrayList<>(candidates));
  }
  boolean hasAny(){return !amount.isEmpty()||!liters.isEmpty()||!date.isEmpty();}
 }
 /** Recognize the final paid total before subtotals, unit prices or VAT. */
 private static final Pattern MONEY=Pattern.compile("(총\\s*결제\\s*금액|총\\s*금액|실\\s*결제\\s*금액|카드\\s*결제|결제\\s*금액|합\\s*계|청구\\s*금액|판매\\s*금액|TOTAL|AMOUNT\\s*PAID)\\s*[:：]?\\s*₩?\\s*([0-9]{1,3}(?:,[0-9]{3})+|[0-9]{3,10})(?![0-9,])(?:\\s*원)?",Pattern.CASE_INSENSITIVE);
 private static final Pattern VOLUME=Pattern.compile("(?:주유\\s*량|공급\\s*량|리터|LITERS?)\\s*[:：]?\\s*([0-9]{1,4}(?:\\.[0-9]{1,3})?)\\s*(?:L|리터)?",Pattern.CASE_INSENSITIVE);
 private static final Pattern DATE=Pattern.compile("(20[0-9]{2})[./-]([0-9]{1,2})[./-]([0-9]{1,2})");
 private static int priority(String label){
  String value=label.toUpperCase(Locale.ROOT).replaceAll("\\s+","");
  if(value.startsWith("총결제")||value.startsWith("실결제")||value.startsWith("카드결제")||value.contains("PAID"))return 100;
  if(value.startsWith("총금액")||value.startsWith("결제금액")||value.equals("TOTAL"))return 90;
  if(value.startsWith("합계")||value.startsWith("청구"))return 70;
  return 50;
 }
 static Result parse(String text){
  if(text==null)return new Result("","","");
  String limited=text.substring(0,Math.min(text.length(),16000));
  // Preserve document order for tied candidates; only the highest score is recommended.
  Map<String,Integer> scores=new LinkedHashMap<>();Matcher m=MONEY.matcher(limited);
  while(m.find()){
   try{
    long value=Long.parseLong(m.group(2).replace(",",""));
    if(value>0&&value<=1_000_000_000L){String candidate=String.valueOf(value);
     int score=priority(m.group(1));
     scores.put(candidate,Math.max(scores.getOrDefault(candidate,0),score));
    }
   }catch(NumberFormatException ignored){}
  }
  List<String> amounts=new ArrayList<>(scores.keySet());
  amounts.sort((a,b)->Integer.compare(scores.get(b),scores.get(a)));
  if(amounts.size()>4)amounts=new ArrayList<>(amounts.subList(0,4));
  String money=amounts.isEmpty()?"":amounts.get(0),liters="",date="";
  m=VOLUME.matcher(limited);
  while(m.find()){
   try{double value=Double.parseDouble(m.group(1));
    if(Double.isFinite(value)&&value>0&&value<=1000){liters=m.group(1);break;}}
   catch(NumberFormatException ignored){}
  }
  m=DATE.matcher(limited);
  while(m.find()){
   try{
    LocalDate parsed=LocalDate.of(Integer.parseInt(m.group(1)),Integer.parseInt(m.group(2)),Integer.parseInt(m.group(3)));
    // Receipts cannot be future-dated; historical transactions are still valid.
    if(!parsed.isAfter(LocalDate.now().plusDays(1))){date=parsed.toString();break;}
   }catch(Exception ignored){}
  }
  return new Result(money,liters,date,amounts);
 }
}
