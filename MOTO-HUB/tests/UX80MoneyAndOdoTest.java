package com.motohub.next;
public final class UX80MoneyAndOdoTest {
 public static void main(String[] args){
  check(MoneyStep.apply("6,750,000",100000)==6850000L,"add 10만원");
  check(MoneyStep.apply("6850000",500000)==7350000L,"add 50만원");
  check(MoneyStep.apply("7350000",-1000000)==6350000L,"subtract 100만원");
  check(MoneyStep.apply("",100000)==100000L,"empty starts at zero");
  check(MoneyStep.apply("10",-100000)==0,"never negative");
  check(OdometerGuard.require(800,850)==850,"increasing odo");
  check(OdometerGuard.largeJump(800,3000),"large jump warning");
  check(!OdometerGuard.largeJump(800,850),"normal odo");
  try{OdometerGuard.require(800,799);throw new AssertionError("regression accepted");}catch(IllegalArgumentException okay){}
  try{MoneyStep.apply("bad",100000);throw new AssertionError("text accepted");}catch(IllegalArgumentException okay){}
  System.out.println("PASS UX80MoneyAndOdoTest");
 }
 static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}