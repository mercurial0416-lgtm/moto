package com.motohub.next;
public final class ReceiptTextParserChoicesTest {
 static int n=0;
 static void ok(boolean yes,String text){n++;if(!yes)throw new AssertionError(text);}
 public static void main(String[] args){
  ReceiptTextParser.Result p=ReceiptTextParser.parse("2026-10-19\n단가 1850원\n합계 28,800원\n부가세 2,618원\n카드 결제 31,418원");
  ok(p.amount.equals("31418"),"paid beats subtotal");
  ok(p.amountCandidates.size()==2,"alternatives kept");
  ok(p.amountCandidates.contains("28800"),"subtotal visible as choice");
  ok(p.date.equals("2026-10-19"),"date extracted");
  p=ReceiptTextParser.parse("TOTAL: 12000\n총 결제 금액 13,500원\n합계 12,000원");
  ok(p.amount.equals("13500"),"grand total higher priority");
  ok(p.amountCandidates.size()==2,"duplicate amount deduplicated");
  p=ReceiptTextParser.parse("판매 금액 12000\n합계 12500\n결제금액 13000\n총금액 13100\n실결제금액 13200\n카드결제 13300");
  ok(p.amountCandidates.size()==4,"bound amount options");
  ok(p.amount.equals("13200"),"highest preference first stable order");
  p=ReceiptTextParser.parse("리터당 1950원\n주유량 : 4.700 L\n2026.10.20");
  ok(p.amount.isEmpty(),"unit price not confused with receipt total");
  ok(p.liters.equals("4.700"),"liters with decimals");
  ok(p.date.equals("2026-10-20"),"valid Korean date");
  p=ReceiptTextParser.parse("카드결제 9,999,999,999원\n2026-02-30");
  ok(!p.hasAny(),"oversized/invalid values rejected");
  ok(ReceiptTextParser.parse(null).amountCandidates.isEmpty(),"null safe");
  System.out.println("PASS OCR ranked choices: "+n+" assertions");
 }
}
