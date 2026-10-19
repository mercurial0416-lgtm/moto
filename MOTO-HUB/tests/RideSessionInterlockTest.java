package com.motohub.next;

public final class RideSessionInterlockTest {
 private static int checks;
 private static void check(boolean condition,String name){
  checks++;if(!condition)throw new AssertionError(name);
 }
 public static void main(String[] args){
  check(!RideSessionInterlock.keepExistingSession(false,false),"idle service may exit");
  check(RideSessionInterlock.keepExistingSession(true,false),"GPS-only ride must survive duplicate start");
  check(RideSessionInterlock.keepExistingSession(false,true),"OBD armed service must survive duplicate start");
  check(RideSessionInterlock.keepExistingSession(true,true),"automatic GPS ride must survive duplicate start");
  System.out.println("PASS RideSessionInterlock "+checks+" assertions");
 }
}
