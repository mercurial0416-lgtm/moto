package com.motohub.next;

public class RideStartConsentTest {
 static int checks=0;
 static void ok(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
 public static void main(String[] args){
  RideStartConsent flow=new RideStartConsent();
  ok(!flow.pending(),"default must be off");
  ok(!flow.mayContinue(true,true),"no callback continuation without consent");
  ok(!flow.consume(true,true),"no recording without consent");
  flow.approve();ok(flow.pending(),"dialog approval starts pending flow");
  ok(flow.mayContinue(true,true),"location grant continues flow");
  ok(flow.pending(),"location grant preserves consent for notification request");
  ok(flow.mayContinue(true,true),"notification grant continues flow");
  ok(flow.consume(true,true),"ready flow consumes consent");
  ok(!flow.pending(),"consent consumed once");
  ok(!flow.consume(true,true),"duplicate service start forbidden");
  flow.approve();ok(!flow.mayContinue(true,false),"denied permission must stop");
  ok(!flow.pending(),"denial revokes consent");
  flow.approve();ok(!flow.mayContinue(false,true),"navigation away must stop");
  ok(!flow.pending(),"navigation revokes consent");
  flow.approve();ok(!flow.consume(true,false),"not-ready device cannot start");
  ok(flow.pending(),"readiness failure leaves state to be explicitly cancelled");
  flow.cancel();ok(!flow.pending(),"settings redirect cancels consent");
  flow.approve();flow.cancel();ok(!flow.consume(true,true),"cancelled dialog cannot start");
  System.out.println("PASS ride consent: "+checks+" checks");
 }
}
