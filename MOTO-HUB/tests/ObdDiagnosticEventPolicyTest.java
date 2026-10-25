package com.motohub.next;
public final class ObdDiagnosticEventPolicyTest {
 static int checks;
 static void check(boolean ok,String name){checks++;if(!ok)throw new AssertionError(name);}
 public static void main(String[] args){
  check(ObdDiagnosticEventPolicy.valid("BT_OK"),"BT accepted");
  check(ObdDiagnosticEventPolicy.valid("NO_ECU"),"ECU status accepted");
  check(ObdDiagnosticEventPolicy.valid("NO_SPEED"),"speed status accepted");
  check(!ObdDiagnosticEventPolicy.valid("11:22:33:44:55:66"),"MAC not accepted");
  check(!ObdDiagnosticEventPolicy.valid("../path"),"path not accepted");
  check(!ObdDiagnosticEventPolicy.valid("GPS 37.30,127.4"),"location not accepted");
  check(!ObdDiagnosticEventPolicy.valid(null),"null not accepted");
  check(ObdDiagnosticEventPolicy.text("SENSOR_OK").contains("정상"),"plain language");
  check(ObdDiagnosticEventPolicy.text("unknown").isEmpty(),"no raw exception");
  System.out.println("PASS ObdDiagnosticEventPolicyTest "+checks);
 }
}
