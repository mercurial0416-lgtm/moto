package com.motohub.next;
import org.junit.Test;
import static org.junit.Assert.*;
public final class RideTypeComparisonTest{
 @Test public void manualCategoryOnly(){
  RideTypeComparison types=new RideTypeComparison();
  assertTrue(types.report().contains("GPS 위치로 출퇴근 여부를 추정하지 않음"));
  assertTrue(types.report().contains("출퇴근"));
  assertTrue(types.report().contains("와인딩"));
 }
}
