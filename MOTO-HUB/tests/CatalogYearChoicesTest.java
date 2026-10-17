package com.motohub.next;

public final class CatalogYearChoicesTest {
 private static int checks;
 private static void eq(int expected,int actual,String message) {
  checks++;if(expected!=actual)throw new AssertionError(message+": expected "+expected+", got "+actual);
 }
 private static void yes(boolean condition,String message) {
  checks++;if(!condition)throw new AssertionError(message);
 }
 public static void main(String[] args) {
  int now=2026;
  String[] choices=CatalogYearChoices.labels(now);
  eq(129,choices.length,"all supported years 1900-2028 inclusive");
  yes("2028년식".equals(choices[0]),"first is newest");
  yes("1900년식".equals(choices[choices.length-1]),"last is oldest");
  eq(2028,CatalogYearChoices.yearAt(0,now),"top year");
  eq(1900,CatalogYearChoices.yearAt(choices.length-1,now),"bottom year");
  eq(2024,CatalogYearChoices.yearAt(CatalogYearChoices.selectedIndex(2024,now),now),"selected existing year");
  eq(2026,CatalogYearChoices.preferred("",now),"empty falls back to current year");
  eq(2026,CatalogYearChoices.preferred("abc",now),"nonnumeric falls back");
  eq(2026,CatalogYearChoices.preferred("1899",now),"out of range falls back");
  eq(2026,CatalogYearChoices.preferred("2029",now),"future beyond allowance falls back");
  eq(2024,CatalogYearChoices.preferred(" 2024 ",now),"trim typed year");
  eq(1900,CatalogYearChoices.preferred("1900",now),"oldest valid year");
  eq(2028,CatalogYearChoices.preferred("2028",now),"max valid future year");
  yes(!CatalogYearChoices.valid(1899,now),"reject year 1899");
  yes(!CatalogYearChoices.valid(2029,now),"reject year 2029");
  yes(CatalogYearChoices.valid(2026,now),"accept current year");
  boolean threw=false;try{CatalogYearChoices.yearAt(choices.length,now);}catch(IllegalArgumentException expected){threw=true;}
  yes(threw,"reject invalid index");
  eq(2,CatalogYearChoices.selectedIndex(9999,now),"invalid selection falls back to current year index");
  System.out.println("PASS: "+checks+" catalog year choice checks");
 }
}
