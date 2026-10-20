package com.motohub.next;
public class ReceiptTextParserTest{
 static int n;
 static void ok(boolean b,String why){n++;if(!b)throw new AssertionError(why);}
 public static void main(String[] args){
  ReceiptTextParser.Result r=ReceiptTextParser.parse("주유 일자 : 2026-10-19\n주유량: 5.25L\n리터당 금액 1900원\n총 금액 : 9,975원");
  ok(r.amount.equals("9975"),"amount");ok(r.liters.equals("5.25"),"volume");ok(r.date.equals("2026-10-19"),"date");
  r=ReceiptTextParser.parse("카드 결제 : 22,000원\n2026.09.20");
  ok(r.amount.equals("22000"),"maintenance cost");ok(r.date.equals("2026-09-20"),"alternate date");
  ok(!ReceiptTextParser.parse("바이크 용량 1000km").hasAny(),"no spurious price");
  ok(ReceiptTextParser.parse("TOTAL: 10000000000").amount.isEmpty(),"no oversized sum");
  ok(ReceiptTextParser.parse("2026-02-30").date.isEmpty(),"invalid date");
  ok(ReceiptTextParser.parse(null).date.isEmpty(),"null safe");
  System.out.println("PASS local OCR candidate parser: "+n+" assertions");
 }
}
