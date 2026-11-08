package com.motohub.next;

import org.junit.Test;
import static org.junit.Assert.*;

public class ObdOdometerExtraProbeTest {
 @Test public void standardMode01AcceptsTwoEncodingStylesAndCanPadding(){
  assertEquals(12345.6,ObdOdometer.parseKm("41 A6 00 01 E2 40"),0.001);
  assertEquals(12345.6,ObdOdometer.parseKm("41A60001E240"),0.001);
  assertEquals(12345.6,ObdOdometer.parseKm("7E8 06 41 A6 00 01 E2 40 00"),0.001);
  assertEquals(12345.6,ObdOdometer.parseKm("7E8 06 41 A6 00 01 E2 40 00\r7E9 06 41 A6 00 01 E2 40 00"),0.001);
 }
 @Test public void readOnlyService22ParsesOnlyMatchingF4A6Reply(){
  assertEquals("22F4A6\r",ObdOdometer.UDS_REQUEST);
  assertEquals("01A6\r",ObdOdometer.STANDARD_REQUEST);
  assertEquals(12345.6,ObdOdometer.parseUdsKm("62 F4 A6 00 01 E2 40"),0.001);
  assertEquals(12345.6,ObdOdometer.parseUdsKm("7E8 07 62 F4 A6 00 01 E2 40"),0.001);
  assertEquals(12345.6,ObdOdometer.parseUdsKm("62F4A60001E240"),0.001);
  assertTrue(Double.isNaN(ObdOdometer.parseUdsKm("7F 22 31")));
  assertTrue(Double.isNaN(ObdOdometer.parseUdsKm("NO DATA")));
  assertTrue(Double.isNaN(ObdOdometer.parseUdsKm("62 F4 A5 00 01 E2 40")));
 }
 @Test public void refusesAmbiguousTruncatedOrFabricatedDistances(){
  assertTrue(Double.isNaN(ObdOdometer.parseKm("41 A6 00 01 E2")));
  assertTrue(Double.isNaN(ObdOdometer.parseKm("41 A6 00 01 E2 40 12 34 56")));
  assertTrue(Double.isNaN(ObdOdometer.parseKm("41 A6 00 00 00 00")));
  assertTrue(Double.isNaN(ObdOdometer.parseKm("41 A6 FF FF FF FF")));
  assertTrue(Double.isNaN(ObdOdometer.parseKm("41A60001E240\r41A60001E241")));
  assertTrue(Double.isNaN(ObdOdometer.parseUdsKm("62F4A60001E240\r62F4A60001E241")));
  assertTrue(Double.isNaN(ObdOdometer.parseKm("01 A6")));
  assertTrue(Double.isNaN(ObdOdometer.parseKm("21 00 00 01 E2 40")));
  assertTrue(Double.isNaN(ObdOdometer.parseKm("7E8 05 41 A6 00 01 E2 40")));
  assertTrue(Double.isNaN(ObdOdometer.parseKm("7E8 06 99 41 A6 00 01 E2 40")));
  assertTrue(Double.isNaN(ObdOdometer.parseKm("041A60001E240")));
  assertTrue(Double.isNaN(ObdOdometer.parseUdsKm("7E8 06 62 F4 A6 00 01 E2 40")));
  assertEquals(12345.6,ObdOdometer.parseUdsKm("18DAF110 07 62 F4 A6 00 01 E2 40"),0.001);
 }
}
