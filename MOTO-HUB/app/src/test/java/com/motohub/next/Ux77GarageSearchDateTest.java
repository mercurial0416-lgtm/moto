package com.motohub.next;
import org.junit.Test;
import static org.junit.Assert.*;
public final class Ux77GarageSearchDateTest {
 @Test public void separatorVariantsMatchExactly() {
  assertEquals("2026-10-29",GarageSearchDate.iso("2026.10.29"));
  assertEquals("2026-10-29",GarageSearchDate.iso("2026/10/29"));
  assertEquals("2026-10-29",GarageSearchDate.iso("2026-10-29"));
  assertEquals("2026-01-09",GarageSearchDate.iso("2026/1/9"));
 }
 @Test public void invalidRangesNeverBecomePlausibleDates() {
  for(String bad:new String[]{"2026/02/30","2026-13-01","10/29","2026-00-01"}){
   try{GarageSearchDate.iso(bad);fail(bad);}catch(IllegalArgumentException expected){}
  }
 }
 @Test public void inclusiveRangeAndMissingRideDateAreSafe() {
  assertTrue(GarageSearchDate.within("2026-10-29","2026-10-29","2026-10-29"));
  assertFalse(GarageSearchDate.within("2026-10-28","2026-10-29",""));
  assertFalse(GarageSearchDate.within("","2026-10-01",""));
  assertTrue(GarageSearchDate.within("","",""));
 }
 @Test public void partialSearchTermsMatchFormattedDates() {
  assertTrue(RideHistorySearch.matchesIndex(
   RideHistorySearch.index("라이딩 2026-10-29",""),RideHistorySearch.terms("10/29")));
  assertTrue(RideHistorySearch.matchesIndex(
   RideHistorySearch.index("라이딩 2026.10.29",""),RideHistorySearch.terms("2026/10/29")));
 }
}
