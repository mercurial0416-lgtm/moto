package com.motohub.next;
import org.junit.Test;
import static org.junit.Assert.*;
public final class RideEndSummaryTest {
 @Test public void summaryUsesObservedMovingRestAndElapsed(){
  String value=RideEndSummary.text(31.25,3600000L,2400000L,900000L,654);
  assertTrue(value.contains("31.25 km"));
  assertTrue(value.contains("기록 시간 60분"));
  assertTrue(value.contains("이동 40분 · 정차 15분"));
  assertTrue(value.contains("654개"));
 }
 @Test public void corruptInputsDoNotInventTravel(){
  String value=RideEndSummary.text(Double.NaN,-100,-50,Long.MAX_VALUE,-3);
  assertFalse(value.contains("NaN"));
  assertTrue(value.contains("0.00 km"));
  assertTrue(value.contains("위치 0개"));
 }
}
