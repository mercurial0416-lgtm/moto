package com.motohub.next;

import org.junit.Test;
import static org.junit.Assert.*;

public class DtcOdoCapturePolicyTest {
 @Test public void recentPairingReadCanBeBoundToDashboard(){
  assertTrue(DtcOdoCapturePolicy.fresh(1000,1000));
  assertTrue(DtcOdoCapturePolicy.fresh(1000,121000));
 }
 @Test public void expiredOrInvalidTimeIsRejected(){
  assertFalse(DtcOdoCapturePolicy.fresh(1000,121001));
  assertFalse(DtcOdoCapturePolicy.fresh(0,150));
  assertFalse(DtcOdoCapturePolicy.fresh(1000,999));
 }
}