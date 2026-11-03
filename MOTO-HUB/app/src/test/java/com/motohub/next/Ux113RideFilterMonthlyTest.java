package com.motohub.next;

import org.junit.Test;
import java.time.YearMonth;
import java.time.ZoneId;
import static org.junit.Assert.*;

/** v573 follow-up: persistent category filter and calendar-aligned GPS summaries. */
public final class Ux113RideFilterMonthlyTest {
 @Test public void categoryFilterUsesActualSavedTagAndDoesNotTreatMemoAsTag(){
  assertEquals(RideCategoryFilter.ALL,RideCategoryFilter.normalize(null));
  assertEquals(RideCategoryFilter.ALL,RideCategoryFilter.normalize("invalid"));
  assertTrue(RideCategoryFilter.matches(RideCategoryFilter.ALL,RideCategory.AUTO));
  assertTrue(RideCategoryFilter.matches(RideCategory.WINDING,RideCategory.WINDING));
  assertFalse(RideCategoryFilter.matches(RideCategory.WINDING,RideCategory.AUTO));
  assertFalse(RideCategoryFilter.matches(RideCategory.WINDING,RideCategory.TOUR));
  assertTrue(RideHistorySearch.matches("2026-11-03 · 출퇴근","완료","출퇴근"));
  assertTrue(RideCategoryFilter.matches(RideCategory.COMMUTE,RideCategory.COMMUTE));
  assertEquals(7,RideCategoryFilter.OPTIONS.length);
 }
 @Test public void sixCalendarMonthsCoverTheFirstDayAndRejectPriorMonth(){
  ZoneId zone=ZoneId.of("Asia/Seoul");
  RideStatsSeries s=new RideStatsSeries(YearMonth.of(2026,11),zone);
  long first=YearMonth.of(2026,6).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli();
  long now=YearMonth.of(2026,11).atDay(3).atStartOfDay(zone).toInstant().toEpochMilli();
  assertEquals(first,s.earliestStartMs());
  assertTrue(s.inDisplayedPeriod(first,now));
  assertFalse(s.inDisplayedPeriod(first-1,now));
  assertTrue(s.add(first,5.5,60000L));
  assertEquals(1,s.rides());
  assertFalse(s.add(first-1,5.5,60000L));
  assertFalse(s.inDisplayedPeriod(now+1,now));
 }
}
