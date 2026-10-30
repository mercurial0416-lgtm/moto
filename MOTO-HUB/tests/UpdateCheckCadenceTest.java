package com.motohub.next;

/** Pure-Java test so retry timing is verified even without Android SDK. */
public final class UpdateCheckCadenceTest {
 private static void check(boolean condition,String name){
  if(!condition)throw new AssertionError(name);
 }
 public static void main(String[] args){
  long start=1_000_000L;
  check(UpdatePromptPolicy.checkDue(start,0,true),"first-ever check");
  check(!UpdatePromptPolicy.checkDue(start+14*60_000L,start,false),"failure retry not too early");
  check(UpdatePromptPolicy.checkDue(start+15*60_000L,start,false),"retry failed network after 15 min");
  check(!UpdatePromptPolicy.checkDue(start+15*60_000L,start,true),"successful check still quiet");
  check(UpdatePromptPolicy.checkDue(start+6*60*60_000L,start,true),"successful check repeats after 6 h");
  check(UpdatePromptPolicy.checkDue(start-1000,start,true),"device clock moved backward");
  check(!UpdatePromptPolicy.checkDue(0,start,false),"invalid current timestamp");
  check(!UpdatePromptPolicy.shouldPrompt(556,556,0,0,start),"current version is not an update");
  check(UpdatePromptPolicy.shouldPrompt(557,556,556,start,start+1),"new version bypasses old snooze");
  System.out.println("PASS UX91: update notice timing and snooze logic");
 }
}
