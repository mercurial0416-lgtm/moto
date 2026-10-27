package com.motohub.next;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public final class WidgetMaintenanceNextDistanceTest {
 @Test public void latestServiceOnlyAndOdometerChanges(){
  List<WidgetMaintenanceSummary.Service> a=Arrays.asList(
    new WidgetMaintenanceSummary.Service("오일","2026-01-01","",1100),
    new WidgetMaintenanceSummary.Service("오일","2026-08-01","",2200),
    new WidgetMaintenanceSummary.Service("체인","2026-08-10","",1900));
  assertEquals("다음 점검 100km 남음",WidgetMaintenanceSummary.nextDistance(a,1800));
  assertEquals("거리 기준 점검 시기 지남",WidgetMaintenanceSummary.nextDistance(a,2000));
  assertEquals("",WidgetMaintenanceSummary.nextDistance(Collections.emptyList(),1800));
 }
}
