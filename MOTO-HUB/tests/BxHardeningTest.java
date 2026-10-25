package com.motohub.next;
import java.io.File;
public final class BxHardeningTest {
 static int passed=0;
 static void ok(boolean b){if(!b)throw new AssertionError("failure "+(++passed));passed++;}
 public static void main(String[] args){
  FuelRecordValidation.requireFuel(15000,5);ok(true);
  for(double bad:new double[]{0,-1,Double.NaN,Double.POSITIVE_INFINITY,1001}){
   boolean failed=false;try{FuelRecordValidation.requireFuel(15000,bad);}catch(IllegalArgumentException e){failed=true;}ok(failed);
  }
  for(double bad:new double[]{0,-2,Double.NaN,Double.POSITIVE_INFINITY}){
   boolean failed=false;try{FuelRecordValidation.requireFuel(bad,5);}catch(IllegalArgumentException e){failed=true;}ok(failed);
  }
  ok(CostDashboard.add(100,12)==112);ok(CostDashboard.add(100,-2)==100);
  ok(CostDashboard.add(100,Double.NaN)==100);ok(CostDashboard.proportion(25,100)==25);
  ok(CostDashboard.proportion(-1,100)==0);ok(CostDashboard.proportion(3,0)==0);
  long now=1_780_000_000_000L;
  RideMonthlyReport report=new RideMonthlyReport();
  File fresh=new File((now-1000)+"-a.track");
  File past=new File((now-40L*86400*1000)+"-b.track");
  File old=new File((now-61L*86400*1000)+"-c.track");
  ok(RideMonthlyReport.recent(fresh,now,30));ok(!RideMonthlyReport.recent(past,now,30));ok(!RideMonthlyReport.recent(old,now,60));
  ok(!RideMonthlyReport.recent(new File("broken.track"),now,60));
  report.add(fresh,now,31.5,125000);report.add(past,now,45,60000);report.add(old,now,10,60000);
  ok(report.current.count==1&&report.current.km==31.5&&report.current.minutes==2);
  ok(report.previous.count==1&&report.previous.km==45);
  report.add(fresh,now,Double.NaN,60000);ok(report.current.count==1);
  System.out.println("PASS: BX hardening assertions "+passed);
 }
}
