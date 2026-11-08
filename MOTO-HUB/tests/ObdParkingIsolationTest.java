package com.motohub.next;

public final class ObdParkingIsolationTest {
 private static void check(boolean yes,String what){if(!yes)throw new AssertionError(what);}
 public static void main(String[] args){
  ObdParkingIsolation p=new ObdParkingIsolation();
  p.onSpeed(48,0);
  check(!p.holdGps(),"moving bike must keep GPS recording");
  check(!p.onLinkLoss(1000),"loss while riding must not auto-finish");
  check(!p.dueAfterLost(999999),"movement-related disconnection must not finish");
  p.reset();
  p.onSpeed(15,0);
  p.onSpeed(0,1000);
  check(p.holdGps(),"stopped ECU speed must freeze walking-phone GPS");
  check(!p.onLinkLoss(1500),"one stopped sample must not prove ignition off");
  p.onSpeed(0,2000);
  check(p.onLinkLoss(2100),"two fresh stopped samples may form parking candidate");
  check(p.holdGps(),"walking home must not write phone GPS points");
  check(p.onLinkLoss(5000),"second reconnect attempt retains first lost timestamp");
  check(!p.dueAfterLost(121000),"grace must be respected");
  check(p.dueAfterLost(122100),"stationary loss should eventually request verified finish");
  p.onLinkConnected();
  check(!p.dueAfterLost(500000),"reconnect alone must cancel lost-link deadline");
  check(p.holdGps(),"reconnect alone must not imply bike motion");
  p.onSpeed(0,500010);
  check(p.holdGps(),"stationary OBD after reconnect still excludes walk");
  p.onSpeed(7,500100);
  check(!p.holdGps(),"fresh moving ECU samples resume ride GPS");
  check(!p.dueAfterLost(900000),"resumption must erase old parked-loss deadline");
  p.reset();
  p.onSpeed(0,1000);p.onSpeed(0,2000);
  check(!p.onLinkLoss(20000),"stale ECU zero must never be called a verified stop");
  check(p.holdGps(),"stopped bike must not be charged for phone walk");
  p.reset();
  check(!p.holdGps()&&!p.dueAfterLost(500000),"reset must clear old session state");
  System.out.println("PASS ObdParkingIsolationTest: 13 assertions");
 }
}