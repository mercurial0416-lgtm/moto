package com.motohub.next;
public final class ObdAutoStartPreflightTest {
 static int count;
 static void eq(ObdAutoStartPreflight.Decision expected,ObdAutoStartPreflight.Decision actual){
  count++;if(expected!=actual)throw new AssertionError("expected "+expected+" got "+actual);
 }
 public static void main(String[] args){
  eq(ObdAutoStartPreflight.Decision.ALLOW,ObdAutoStartPreflight.decide(true,false,false,true));
  eq(ObdAutoStartPreflight.Decision.WAIT_GPS,ObdAutoStartPreflight.decide(true,false,false,false));
  eq(ObdAutoStartPreflight.Decision.SKIP,ObdAutoStartPreflight.decide(false,false,false,true));
  eq(ObdAutoStartPreflight.Decision.SKIP,ObdAutoStartPreflight.decide(true,true,false,true));
  eq(ObdAutoStartPreflight.Decision.SKIP,ObdAutoStartPreflight.decide(true,false,true,true));
  eq(ObdAutoStartPreflight.Decision.SKIP,ObdAutoStartPreflight.decide(true,true,true,false));
  System.out.println("ObdAutoStartPreflightTest PASS "+count);
 }
}
