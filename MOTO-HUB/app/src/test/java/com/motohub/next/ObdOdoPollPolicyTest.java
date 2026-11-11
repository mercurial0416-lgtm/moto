package com.motohub.next;

import org.junit.Test;
import static org.junit.Assert.*;

public class ObdOdoPollPolicyTest {
 @Test public void supportedPidUsesLowRateNormalPolling(){
  assertFalse(ObdOdoPollPolicy.shouldProbe(true,ObdPidCapabilities.Support.YES,40,59,false));
  assertFalse(ObdOdoPollPolicy.shouldProbe(true,ObdPidCapabilities.Support.YES,40,60,false));
  assertTrue(ObdOdoPollPolicy.shouldProbe(true,ObdPidCapabilities.Support.YES,0,60,false));
  assertTrue(ObdOdoPollPolicy.shouldProbe(true,ObdPidCapabilities.Support.YES,0,120,true));
 }
 @Test public void absentA0BitmapGetsOnlyOneStationaryTry(){
  assertFalse(ObdOdoPollPolicy.shouldProbe(true,ObdPidCapabilities.Support.UNKNOWN,25,120,false));
  assertFalse(ObdOdoPollPolicy.shouldProbe(true,ObdPidCapabilities.Support.NO,0,7,false));
  assertTrue(ObdOdoPollPolicy.shouldProbe(true,ObdPidCapabilities.Support.UNKNOWN,0,8,false));
  assertFalse(ObdOdoPollPolicy.shouldProbe(true,ObdPidCapabilities.Support.NO,0,9,false));
  assertFalse(ObdOdoPollPolicy.shouldProbe(true,ObdPidCapabilities.Support.UNKNOWN,0,80,true));
 }
 @Test public void cannotQueryWithoutConsentOrValidSpeed(){
  assertFalse(ObdOdoPollPolicy.shouldProbe(false,ObdPidCapabilities.Support.YES,0,60,false));
  assertFalse(ObdOdoPollPolicy.shouldProbe(false,ObdPidCapabilities.Support.UNKNOWN,0,10,false));
  assertFalse(ObdOdoPollPolicy.shouldProbe(true,ObdPidCapabilities.Support.YES,-1,60,false));
 }
}