package com.motohub.next;

import org.junit.Test;
import static org.junit.Assert.*;

public class DtcCounterConfirmPolicyTest {
 @Test public void singleLowGlitchNeverCountsAsConfirmedReset(){
  int prev=-1;
  int[] samples={44,0,44,44};
  int[] expected={-1,-1,-1,44};
  for(int i=0;i<samples.length;i++){
   assertEquals(expected[i],DtcCounterConfirmPolicy.confirmed(prev,samples[i]));
   prev=samples[i];
  }
 }
 @Test public void genuineResetNeedsTwoConsecutiveLowSamples(){
  int prev=44;
  assertEquals(-1,DtcCounterConfirmPolicy.confirmed(prev,0));
  prev=0;
  assertEquals(0,DtcCounterConfirmPolicy.confirmed(prev,0));
 }
 @Test public void isolatedHighGlitchCannotInflateCounter(){
  int prev=44;
  assertEquals(-1,DtcCounterConfirmPolicy.confirmed(prev,900));
  prev=900;
  assertEquals(-1,DtcCounterConfirmPolicy.confirmed(prev,45));
  prev=45;
  assertEquals(45,DtcCounterConfirmPolicy.confirmed(prev,45));
 }
 @Test public void normalIncrementAtKmBoundaryIsAccepted(){
  assertEquals(-1,DtcCounterConfirmPolicy.confirmed(-1,44));
  assertEquals(44,DtcCounterConfirmPolicy.confirmed(44,44));
  assertEquals(45,DtcCounterConfirmPolicy.confirmed(44,45));
  assertEquals(48,DtcCounterConfirmPolicy.confirmed(44,48));
  assertEquals(-1,DtcCounterConfirmPolicy.confirmed(44,49));
 }
}