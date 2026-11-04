package com.motohub.next;
import java.io.File;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;

/** Regression for split ride durations and order-independent period counts. */
public final class Ux123SegmentStatsTest {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 static final String SESSION="12345678-1234-4234-9234-123456789abc";
 static File track(long timestamp){return new File(timestamp+"-"+SESSION+".track");}
 public static void main(String[] args){
  ZoneId zone=ZoneId.of("Asia/Tokyo");
  long nov1=java.time.LocalDate.of(2026,11,1).atStartOfDay(zone).toInstant().toEpochMilli();
  RideStatsSeries chart=new RideStatsSeries(YearMonth.of(2026,11),zone);
  // Newest-first iteration used by the real app; the oldest month must own the ride.
  check(chart.add(SESSION,nov1+10_000,1.0,40_000),"newer segment accepted");
  check(chart.add(SESSION,nov1-10_000,2.0,35_000),"older segment accepted");
  check(chart.rides()==1 && chart.count[4]==1 && chart.count[5]==0,
   "calendar month attribution depends on file iteration order");
  check(chart.totalMinutes()==1,"sub-minute sections lost across months");
  check(Math.abs(chart.totalKm-3.0)<0.0001,"sections lost distance");
  RideStatsSeries reverse=new RideStatsSeries(YearMonth.of(2026,11),zone);
  reverse.add(SESSION,nov1-10_000,2.0,35_000);
  reverse.add(SESSION,nov1+10_000,1.0,40_000);
  check(reverse.count[4]==1&&reverse.count[5]==0,"reverse order differs");
  RideStatsSeries sameMonth=new RideStatsSeries(YearMonth.of(2026,11),zone);
  sameMonth.add(SESSION,nov1+1_000,1.0,40_000);
  sameMonth.add(SESSION,nov1+41_000,2.0,45_000);
  check(sameMonth.minutes[5]==1 && sameMonth.totalMinutes()==1 &&
        sameMonth.rides()==1,"same-month split duration rounded per section");
  long now=Instant.parse("2026-11-04T00:00:00Z").toEpochMilli();
  long cut=now-30L*24*60*60*1000;
  RideMonthlyReport report=new RideMonthlyReport();
  report.add(track(now-60_000),now,1.5,40_000);
  report.add(track(now-20_000),now,2.5,45_000);
  check(report.current.count==1&&report.current.minutes==1&&
    Math.abs(report.current.km-4)<.0001,"60-day report lost split minutes");
  RideMonthlyReport boundary=new RideMonthlyReport();
  boundary.add(track(cut+10_000),now,1,20_000);
  boundary.add(track(cut-10_000),now,2,40_000);
  check(boundary.current.count==0&&boundary.previous.count==1,
    "30-day boundary incorrectly counted one ride twice");
  RideMonthlyReport reverseBoundary=new RideMonthlyReport();
  reverseBoundary.add(track(cut-10_000),now,2,40_000);
  reverseBoundary.add(track(cut+10_000),now,1,20_000);
  check(reverseBoundary.current.count==0&&reverseBoundary.previous.count==1,
    "reverse 30-day order is inconsistent");
  System.out.println("PASS UX123 split-segment durations, month attribution, 30-day boundaries");
 }
}
