package com.motohub.next;
import org.junit.Test;import static org.junit.Assert.*;
public class ObdSpeedJumpRecoveryTest {
 @Test public void singleSpikeIsRejected(){
  ObdSpeedSanityPolicy p=new ObdSpeedSanityPolicy();
  assertTrue(p.accept(0,1000));assertFalse(p.accept(170,2000));
  assertTrue(p.accept(3,2100));
 }
 @Test public void repeatedStableChangeCanRecover(){
  ObdSpeedSanityPolicy p=new ObdSpeedSanityPolicy();
  assertTrue(p.accept(0,1000));assertFalse(p.accept(100,2000));
  assertTrue(p.accept(101,2600));assertTrue(p.accept(105,3000));
 }
 @Test public void contradictoryFramesRemainRejected(){
  ObdSpeedSanityPolicy p=new ObdSpeedSanityPolicy();
  assertTrue(p.accept(10,1000));assertFalse(p.accept(110,2000));
  assertFalse(p.accept(180,2200));assertTrue(p.accept(12,2500));
 }
}