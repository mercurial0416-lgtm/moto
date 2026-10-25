package com.motohub.next;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class MaintenanceNoticeTextTest {
 @Test public void upcomingBoth() {assertEquals("2026-10-20 (D-2) · 500 km 남음", MaintenanceNoticeText.detail("2026-10-20",5000,"2026-10-18",4500));}
 @Test public void overdueBoth() {assertEquals("2026-10-16 (2일 지남) · 200 km 초과", MaintenanceNoticeText.detail("2026-10-16",5000,"2026-10-18",5200));}
 @Test public void dueToday() {assertEquals("2026-10-18 (오늘)", MaintenanceNoticeText.detail("2026-10-18",0,"2026-10-18",4500));}
 @Test public void badDateKeepsMileage() {assertEquals("300 km 남음", MaintenanceNoticeText.detail("invalid",5000,"2026-10-18",4700));}
 @Test public void badMileageIgnored() {assertEquals("", MaintenanceNoticeText.detail("",Double.NaN,"2026-10-18",4700));}
}
