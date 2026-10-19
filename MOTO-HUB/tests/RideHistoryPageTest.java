package com.motohub.next;
public final class RideHistoryPageTest {
 static int checks;
 static void check(boolean ok,String reason){checks++;if(!ok)throw new AssertionError(reason);}
 public static void main(String[] args){
  check(RideHistoryPage.PAGE_SIZE==25,"initial view cap");
  check(RideHistoryPage.nextLimit(25)==50,"next page");
  check(RideHistoryPage.nextLimit(0)==25,"zero limit starts at first page");
  check(RideHistoryPage.nextLimit(-5)==25,"negative limit starts at first page");
  check(RideHistoryPage.nextLimit(Integer.MAX_VALUE)==Integer.MAX_VALUE,"no integer overflow");
  check(RideHistoryPage.nextLimit(Integer.MAX_VALUE-1)==Integer.MAX_VALUE,"near overflow");
  check(RideHistoryPage.remaining(101,25)==76,"remaining count");
  check(RideHistoryPage.remaining(10,25)==0,"all displayed");
  check(RideHistoryPage.remaining(0,0)==0,"empty result");
  check(RideHistoryPage.remaining(3,-5)==3,"negative displayed safe");
  System.out.println("PASS: "+checks+" history pagination assertions");
 }
}
