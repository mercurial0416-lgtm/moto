package com.motohub.next;
public final class Ux125ActiveSessionPolicyTest {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 public static void main(String[] args){
  String session="12345678-1234-4234-9234-123456789abc";
  String earlier="1762000000000-"+session+".track";
  String active="1762000020000-"+session+".track";
  check(RideActiveSessionPolicy.exclude(earlier,true,active),"previously completed segment must stay hidden");
  check(RideActiveSessionPolicy.exclude(active,true,active),"current segment must stay hidden");
  check(!RideActiveSessionPolicy.exclude(earlier,false,active),"finished ride must be included");
  check(!RideActiveSessionPolicy.exclude(earlier,true,"1762000000001-87654321-1234-4234-9234-123456789abc.track"),"unrelated ride must remain visible");
  check(!RideActiveSessionPolicy.exclude(earlier,true,"invalid"),"invalid active session cannot hide unrelated rides");
  System.out.println("PASS UX125 active split-session filter");
 }
}
