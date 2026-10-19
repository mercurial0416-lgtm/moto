package com.motohub.next;
import java.util.List;
public final class MaintenanceNoticeOrderTest {
 private static int checks;
 private static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
 private static MaintenanceNoticeOrder.Notice n(String type,String recorded,String due,double km){
  return new MaintenanceNoticeOrder.Notice(type,recorded,due,km);
 }
 public static void main(String[] args){
  String today="2026-10-18";
  check(MaintenanceNoticeOrder.prioritized(List.of(),today,10000).isEmpty(),"empty history");
  check(MaintenanceNoticeOrder.prioritized(List.of(n("oil","2026-10-01","",0)),today,10000).isEmpty(),"unscheduled ignored");
  check(MaintenanceNoticeOrder.isNewerOrSame("2026-10-03","2026-10-02"),"later record wins");
  check(!MaintenanceNoticeOrder.isNewerOrSame("2026-10-01","2026-10-02"),"older record loses");
  check(MaintenanceNoticeOrder.isNewerOrSame("2026-10-02","2026-10-02"),"last saved wins on tie");
  var history=List.of(
   n("oil","2026-10-12","2027-01-01",15000),
   n("brakes","2026-10-11","2026-11-01",0),
   n("chain","2026-10-10","2026-10-01",0),
   n("oil","2026-10-01","2026-10-02",9500), // stale overdue record
   n("tyres","2026-10-09","2027-02-01",0));
  var ranked=MaintenanceNoticeOrder.prioritized(history,today,10000);
  check(ranked.size()==4,"latest record per type");
  check(ranked.get(0).type.equals("chain"),"overdue first");
  check(ranked.get(1).type.equals("brakes"),"soon before later");
  check(ranked.get(2).type.equals("oil"),"older overdue oil ignored");
  check(ranked.get(3).type.equals("tyres"),"later sorted by date");
  var tied=MaintenanceNoticeOrder.prioritized(List.of(
   n("oil","2026-10-01","2026-10-01",0),
   n("oil","2026-10-01","2027-01-01",0)),today,10000);
  check(tied.size()==1,"tied service records deduplicated");
  check(tied.get(0).nextDate.equals("2027-01-01"),"last tied service wins");
  var invalid=MaintenanceNoticeOrder.prioritized(List.of(n("oil","2026-10-01","bad-date",10100)),today,10000);
  check(invalid.size()==1,"invalid date does not hide valid km schedule");
  check(MaintenancePlanner.order(HomeMaintenanceStatus.safeStatus(invalid.get(0).nextDate,invalid.get(0).nextKm,today,10000))==1,"invalid date fallback still soon");
  System.out.println("PASS: "+checks+" maintenance notice ordering checks");
 }
}
