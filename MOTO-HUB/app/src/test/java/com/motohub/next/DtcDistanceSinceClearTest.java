package com.motohub.next;

import org.junit.Test;
import static org.junit.Assert.*;

public class DtcDistanceSinceClearTest {
 @Test public void decodesRealMode01Pid31(){
  assertEquals(44,DtcDistanceSinceClear.parseKm("41 31 00 2C"));
  assertEquals(44,DtcDistanceSinceClear.parseKm("4131002C"));
  assertEquals(44,DtcDistanceSinceClear.parseKm("7E8 04 41 31 00 2C 00 00"));
  assertEquals(44,DtcDistanceSinceClear.parseKm("7E8044131002C"));
  assertEquals(44,DtcDistanceSinceClear.parseKm("18DAF110 04 41 31 00 2C"));
  assertEquals(0,DtcDistanceSinceClear.parseKm("41 31 00 00"));
  assertEquals(65535,DtcDistanceSinceClear.parseKm("41 31 FF FF"));
  assertEquals("0131\r",DtcDistanceSinceClear.REQUEST);
 }
 @Test public void rejectsEchoFaultsMixedEcusAndTruncations(){
  assertEquals(-1,DtcDistanceSinceClear.parseKm("0131\rNO DATA"));
  assertEquals(-1,DtcDistanceSinceClear.parseKm("NO DATA"));
  assertEquals(-1,DtcDistanceSinceClear.parseKm("7F 01 11"));
  assertEquals(-1,DtcDistanceSinceClear.parseKm("41 31 00"));
  assertEquals(-1,DtcDistanceSinceClear.parseKm("41 31 00 2C 8A"));
  assertEquals(-1,DtcDistanceSinceClear.parseKm("7E8 03 41 31 00 2C"));
  assertEquals(-1,DtcDistanceSinceClear.parseKm("7E8 04 41 31 00 2C\r7E9 04 41 31 00 30"));
  assertEquals(-1,DtcDistanceSinceClear.parseKm("11 41 31 00 2C"));
  assertEquals(-1,DtcDistanceSinceClear.parseKm(null));
 }
 @Test public void resetSaturationAndPlausibilityNeverFabricateOdo(){
  assertEquals(DtcOdoCounterPolicy.Decision.ACCEPT,DtcOdoCounterPolicy.check(44,50,120000));
  assertEquals(DtcOdoCounterPolicy.Decision.RESET,DtcOdoCounterPolicy.check(44,1,120000));
  assertEquals(DtcOdoCounterPolicy.Decision.SATURATED,DtcOdoCounterPolicy.check(65534,65535,5000));
  assertEquals(DtcOdoCounterPolicy.Decision.IMPLAUSIBLE,DtcOdoCounterPolicy.check(44,300,500));
  assertEquals(1206.0,DtcOdoCounterPolicy.estimated(1200,44,50),0.001);
  assertTrue(Double.isNaN(DtcOdoCounterPolicy.estimated(1200,44,0)));
  assertTrue(Double.isNaN(DtcOdoCounterPolicy.estimated(1200,44,65535)));
 }
}