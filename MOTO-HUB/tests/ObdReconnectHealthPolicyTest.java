package com.motohub.next;

public final class ObdReconnectHealthPolicyTest {
 public static void main(String[] args){
  assertEquals(1,ObdReconnectHealthPolicy.acceptValidSpeed(0),"initial");
  int n=0;
  for(int i=0;i<29;i++)n=ObdReconnectHealthPolicy.acceptValidSpeed(n);
  assertFalse(ObdReconnectHealthPolicy.stable(n),"intermittent connector");
  n=ObdReconnectHealthPolicy.acceptValidSpeed(n);
  assertTrue(ObdReconnectHealthPolicy.stable(n),"healthy connector");
  assertEquals(30,ObdReconnectHealthPolicy.acceptValidSpeed(n),"saturated count");
  n=0; // connected() or reconnecting() explicitly resets this session count
  n=ObdReconnectHealthPolicy.acceptValidSpeed(n);
  assertFalse(ObdReconnectHealthPolicy.stable(n),"a single sample after reconnect is not healthy");
  assertEquals(1,ObdReconnectHealthPolicy.acceptValidSpeed(-1),"invalid previous counter");
  System.out.println("ObdReconnectHealthPolicyTest PASS");
 }
 private static void assertTrue(boolean b,String s){if(!b)throw new AssertionError(s);}
 private static void assertFalse(boolean b,String s){assertTrue(!b,s);}
 private static void assertEquals(int a,int b,String s){assertTrue(a==b,s);}
}
