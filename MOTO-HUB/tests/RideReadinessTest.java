package com.motohub.next;

public final class RideReadinessTest {
 static int checks;
 static void yes(boolean ok,String name){checks++;if(!ok)throw new AssertionError(name);}
 static void state(RideReadiness.State expected,boolean running,boolean fine,boolean gps,boolean permission,boolean notifications){
  yes(RideReadiness.check(running,fine,gps,permission,notifications)==expected,"expected "+expected);
 }
 public static void main(String[] args){
  state(RideReadiness.State.READY,false,true,true,true,true);
  state(RideReadiness.State.LOCATION_PERMISSION,false,false,true,true,true);
  state(RideReadiness.State.GPS_DISABLED,false,true,false,true,true);
  state(RideReadiness.State.NOTIFICATION_PERMISSION,false,true,true,false,true);
  state(RideReadiness.State.NOTIFICATIONS_DISABLED,false,true,true,true,false);
  state(RideReadiness.State.RECORDING,true,false,false,false,false);
  state(RideReadiness.State.LOCATION_PERMISSION,false,false,false,false,false);
  state(RideReadiness.State.GPS_DISABLED,false,true,false,false,false);
  state(RideReadiness.State.NOTIFICATION_PERMISSION,false,true,true,false,false);
  yes(RideReadiness.action(RideReadiness.State.READY).isEmpty(),"ready no action");
  yes(RideReadiness.action(RideReadiness.State.RECORDING).isEmpty(),"recording no action");
  yes(RideReadiness.action(RideReadiness.State.GPS_DISABLED).contains("설정"),"gps settings shortcut");
  yes(RideReadiness.action(RideReadiness.State.NOTIFICATIONS_DISABLED).contains("설정"),"notifications settings shortcut");
  for(RideReadiness.State s:RideReadiness.State.values()){
   yes(!RideReadiness.title(s).isBlank(),"title "+s);
   yes(!RideReadiness.guidance(s).isBlank(),"guidance "+s);
  }
  System.out.println("PASS: "+checks+" ride readiness checks");
 }
}
