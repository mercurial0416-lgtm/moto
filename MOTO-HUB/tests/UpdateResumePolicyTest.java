package com.motohub.next;
public final class UpdateResumePolicyTest {
 static int count=0;
 static void check(boolean test,String name){count++;if(!test)throw new AssertionError(name);}
 public static void main(String[] args){
  check(UpdateResumePolicy.canResume(10,20),"valid position");
  check(!UpdateResumePolicy.canResume(0,20),"no resume from zero");
  check(!UpdateResumePolicy.canResume(20,20),"already complete");
  check(!UpdateResumePolicy.canResume(21,20),"oversized part");
  check(UpdateResumePolicy.validPartial(206,"bytes 10-19/20",10,20),"matching response");
  check(UpdateResumePolicy.validPartial(206,"bytes 10-10/20",10,20),"single byte range");
  check(!UpdateResumePolicy.validPartial(200,null,10,20),"full response must restart");
  check(!UpdateResumePolicy.validPartial(206,"bytes 0-19/20",10,20),"wrong start");
  check(!UpdateResumePolicy.validPartial(206,"bytes 10-21/20",10,20),"out of bounds");
  check(!UpdateResumePolicy.validPartial(206,"bytes 10-19/21",10,20),"wrong total");
  check(!UpdateResumePolicy.validPartial(206,"bytes */20",10,20),"unsatisfied range");
  check(!UpdateResumePolicy.validPartial(206,"BYTES 10-19/20",10,20),"noncompliant prefix");
  check(!UpdateResumePolicy.validPartial(206,"bytes 10-19/*",10,20),"unknown total");
  check(!UpdateResumePolicy.validPartial(206,"bytes 10-19/20 trailing",10,20),"junk suffix");
  check(!UpdateResumePolicy.validPartial(206,"bytes 99999999999999999999999-19/20",10,20),"overflow");
  check(!UpdateResumePolicy.validPartial(206,"bytes 10-19/20",0,20),"zero offset");
  check(!UpdateResumePolicy.validPartial(206,"bytes 10-19/20",10,10),"complete content");
  check(!UpdateResumePolicy.validPartial(416,"bytes 10-19/20",10,20),"range unsatisfiable");
  System.out.println("PASS resume range policy "+count+" checks");
 }
}
