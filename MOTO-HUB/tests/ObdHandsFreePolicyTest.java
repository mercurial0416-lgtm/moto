package com.motohub.next;
public final class ObdHandsFreePolicyTest {
 static int count;
 static void expect(boolean value,String message){count++;if(!value)throw new AssertionError(message);}
 public static void main(String[] args){
  expect(ObdHandsFreePolicy.reconnectCooldownMs(1)==30_000,"first retry round");
  expect(ObdHandsFreePolicy.reconnectCooldownMs(2)==60_000,"second retry round");
  expect(ObdHandsFreePolicy.reconnectCooldownMs(3)==120_000,"third retry round");
  expect(ObdHandsFreePolicy.reconnectCooldownMs(999)==300_000,"limit background polling");
  expect(ObdHandsFreePolicy.visibleResumeDue(1000,0),"first start");
  expect(!ObdHandsFreePolicy.visibleResumeDue(16000,5000),"activity double resume");
  expect(ObdHandsFreePolicy.visibleResumeDue(20000,5000),"retry at 15 sec");
  expect(ObdHandsFreePolicy.visibleResumeDue(400,800000),"allow retry after reboot elapsed clock reset");
  expect(!ObdHandsFreePolicy.visibleResumeDue(-1,1),"invalid clock");
  long now=1_000_000L, started=100_000L,lastMotion=400_000L;
  expect(ObdHandsFreePolicy.finishAfterObdLoss(true,true,true,false,60,1.8,0,started,lastMotion,now,600_000),"stopped valid ride");
  expect(!ObdHandsFreePolicy.finishAfterObdLoss(true,true,false,false,60,1.8,0,started,lastMotion,now,600_000),"GPS outage never ends ride");
  expect(!ObdHandsFreePolicy.finishAfterObdLoss(true,true,true,true,60,1.8,0,started,lastMotion,now,600_000),"fresh OBD owns stop decision");
  expect(!ObdHandsFreePolicy.finishAfterObdLoss(true,true,true,false,60,1.8,12,started,lastMotion,now,600_000),"moving GPS never ends ride");
  expect(!ObdHandsFreePolicy.finishAfterObdLoss(true,true,true,false,2,1.8,0,started,lastMotion,now,600_000),"few fixes never end ride");
  expect(!ObdHandsFreePolicy.finishAfterObdLoss(true,true,true,false,60,0.02,0,started,lastMotion,now,600_000),"tiny ride never ends");
  expect(!ObdHandsFreePolicy.finishAfterObdLoss(true,true,true,false,60,1.8,0,started,lastMotion+1,now,600_000),"recent movement blocks end");
  expect(!ObdHandsFreePolicy.finishAfterObdLoss(false,true,true,false,60,1.8,0,started,lastMotion,now,600_000),"disarmed cannot end automatically");
  expect(!ObdHandsFreePolicy.finishAfterObdLoss(true,false,true,false,60,1.8,0,started,lastMotion,now,600_000),"not recording");
  expect(!ObdHandsFreePolicy.finishAfterObdLoss(true,true,true,false,60,1.8,Double.NaN,started,lastMotion,now,600_000),"invalid speed");
  System.out.println("ObdHandsFreePolicyTest PASS: "+count);
 }
}
