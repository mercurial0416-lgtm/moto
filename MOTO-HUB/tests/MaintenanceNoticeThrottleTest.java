package com.motohub.next;
public final class MaintenanceNoticeThrottleTest {
 static int checks;
 static void check(String now,String prior,long sent,long time,boolean expected,String label){
  checks++;
  boolean actual=MaintenanceNoticeThrottle.shouldNotify(now,prior,sent,time);
  if(actual!=expected)throw new AssertionError(label+": "+actual+" != "+expected);
 }
 public static void main(String[] args){
  long day=24L*60*60*1000;
  long now=1_800_000_000_000L;
  check("service:new","",0,now,true,"first opted-in reminder");
  check("service:new","service:old",now-10_000,now,true,"new maintenance item");
  check("service:new","service:new",now-day,now,false,"no daily spam");
  check("service:new","service:new",now-7*day+1,now,false,"six days 23:59 no repeat");
  check("service:new","service:new",now-7*day,now,true,"weekly repeat for unchanged due");
  check("service:new","service:new",now-30*day,now,true,"late periodic check");
  check("service:new","service:new",0,now,false,"legacy stamp without timestamp does not duplicate");
  check("service:new","service:new",now+day,now,false,"clock moved backwards");
  check("","",0,now,false,"no maintenance alert");
  check(null,"",0,now,false,"null fingerprint");
  check("service:new","",0,0,false,"invalid device clock");
  System.out.println("PASS maintenance notice throttling: "+checks+" assertions");
 }
}
