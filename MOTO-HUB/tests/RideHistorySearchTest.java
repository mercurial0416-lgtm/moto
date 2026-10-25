package com.motohub.next;

public final class RideHistorySearchTest {
 static int count;
 static void check(boolean ok,String why){count++;if(!ok)throw new AssertionError(why);}
 public static void main(String[] args){
  String date="2026.10.17 (토) 21:40";
  check(RideHistorySearch.matches(date,"완료",null),"null query matches");
  check(RideHistorySearch.matches(date,"완료",""),"empty query matches");
  check(RideHistorySearch.matches(date,"완료","   "),"spaces match");
  check(RideHistorySearch.matches(date,"완료","2026.10.17"),"formatted date");
  check(RideHistorySearch.matches(date,"완료","2026-10-17"),"ISO date");
  check(RideHistorySearch.matches(date,"완료","2026/10/17"),"slash date");
  check(RideHistorySearch.matches(date,"완료","10월 17일"),"Korean month day");
  check(RideHistorySearch.matches(date,"완료","10/17"),"short date");
  check(RideHistorySearch.matches(date,"완료","21:40"),"time");
  check(RideHistorySearch.matches(date,"완료","토"),"weekday");
  check(RideHistorySearch.matches(date,"완료","완료"),"status");
  check(RideHistorySearch.matches(date,"GPX","gpx"),"case insensitive GPX");
  check(RideHistorySearch.matches(date,"복구","복구"),"recovered status");
  check(RideHistorySearch.matches(date,"기록 중","기록중"),"spaces ignored");
  check(RideHistorySearch.matches(date,"중단","중단"),"interrupted status");
  check(!RideHistorySearch.matches(date,"완료","2025"),"wrong year");
  check(!RideHistorySearch.matches(date,"완료","gpx"),"wrong status");
  check(!RideHistorySearch.matches(date,"완료","2026.11"),"wrong month");
  check(!RideHistorySearch.matches("날짜 정보 없음","중단","2026"),"missing date not matched");
  check(RideHistorySearch.matches("날짜 정보 없음","중단","날짜정보없음"),"missing date searchable");
  check(RideHistorySearch.limit(null).equals(""),"null limit");
  check(RideHistorySearch.limit(" abc ").equals("abc"),"trim");
  check(RideHistorySearch.limit("x".repeat(60)).length()==RideHistorySearch.MAX_LENGTH,"max length");
  check(RideHistorySearch.normalize(null).equals(""),"null normalization");
  check(RideHistorySearch.normalize("2026년 10월 17일").equals("20261017"),"Korean date normalization");
  check(RideHistorySearch.matches("2026.10.17 (토) 21:40","완료","  10/17  "),"trimmed query");
  check(RideHistorySearch.matches(date,"GPX","10/17 GPX"),"combined date and GPX query");
  check(RideHistorySearch.matches(date,"완료","완료 10/17"),"terms may appear in either order");
  check(!RideHistorySearch.matches(date,"완료","10/17 GPX"),"all terms required");
  check(RideHistorySearch.matches(date,"완료","10/17   완료"),"multiple spaces");
  check(RideHistorySearch.matches(date,"완료","/ /"),"punctuation-only query");
  String indexed=RideHistorySearch.index(date,"GPX");
  check(RideHistorySearch.matchesIndex(indexed,RideHistorySearch.terms("10/17 GPX")),"pre-indexed search");
  check(!RideHistorySearch.matchesIndex(indexed,RideHistorySearch.terms("10/17 복구")),"pre-indexed mismatch");
  check(RideHistorySearch.normalize("일상").equals("일상"),"preserve Korean words containing date suffix letters");
  check(!RideHistorySearch.matches("2026.10.17","완료","일상"),"category must not match arbitrary text");
  check(RideHistorySearch.matches("2026.10.17 · 일상","완료","일상"),"category search exact");
  System.out.println("PASS: "+count+" ride-history search assertions");
 }
}
