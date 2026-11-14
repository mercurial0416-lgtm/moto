package com.motohub.next;
public final class ObdPowerModePolicyTest {
 static int count=0;
 static void eq(long a,long b,String label){count++;if(a!=b)throw new AssertionError(label+" "+a+" != "+b);}
 public static void main(String[] args){
  eq(30_000,ObdPowerModePolicy.delayMs(1,false,false),"default first");
  eq(300_000,ObdPowerModePolicy.delayMs(99,false,false),"balanced caps at five minutes");
  eq(30_000,ObdPowerModePolicy.delayMs(1,true,false),"eco initial");
  eq(600_000,ObdPowerModePolicy.delayMs(4,true,false),"eco parked after four failures");
  eq(900_000,ObdPowerModePolicy.delayMs(8,true,false),"eco parked after eight failures");
  eq(300_000,ObdPowerModePolicy.delayMs(8,true,true),"active ride keeps fast reconnect");
  System.out.println("ObdPowerModePolicyTest PASS: "+count);
 }
}
