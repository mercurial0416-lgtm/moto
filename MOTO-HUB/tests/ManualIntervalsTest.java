package com.motohub.next;
public final class ManualIntervalsTest {
 static int n;
 static void ok(boolean b,String s){n++;if(!b)throw new AssertionError(s);}
 static void bad(String s){try{ManualServiceIntervals.parse(s);throw new AssertionError("Accepted: "+s);}catch(IllegalArgumentException e){n++;}}
 public static void main(String[] args){
  ok(ManualServiceIntervals.parse("100")==100,"minimum");
  ok(ManualServiceIntervals.parse("100000")==100000,"maximum");
  ok(ManualServiceIntervals.next(5000.5,3000)==8001,"next round up");
  for(String s:new String[]{"","0","99","100001","-100","abc","100.5","9999999999"})bad(s);
  System.out.println("PASS user-defined service interval policy: "+n+" assertions");
 }
}
