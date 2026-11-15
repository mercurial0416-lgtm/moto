package com.motohub.next;
public final class ObdBikeDiagnosticScopeTest {
 static int count;
 static void yes(boolean b){count++;if(!b)throw new AssertionError("assertion "+count);}
 static void no(boolean b){yes(!b);}
 static void fails(Runnable r){count++;try{r.run();throw new AssertionError("accepted invalid key");}catch(IllegalArgumentException ok){}}
 public static void main(String[] args){
  String a="11111111-1111-4111-8111-111111111111";
  String b="22222222-2222-4222-8222-222222222222";
  yes(!ObdBikeDiagnosticScope.key(a,"events").equals(ObdBikeDiagnosticScope.key(b,"events")));
  yes(!ObdBikeDiagnosticScope.key(a,"last_AUTO_ON").equals(ObdBikeDiagnosticScope.key(a,"events")));
  fails(()->ObdBikeDiagnosticScope.key("INVALID","events"));
  fails(()->ObdBikeDiagnosticScope.key(a,"gps/raw"));
  yes(ObdBikeDiagnosticScope.shouldRecord(true,"SENSOR_OK"));
  no(ObdBikeDiagnosticScope.shouldRecord(false,"SENSOR_OK"));
  yes(ObdBikeDiagnosticScope.shouldRecord(false,"GPS_SAVED"));
  yes(ObdBikeDiagnosticScope.shouldRecord(false,"AUTO_OFF"));
  no(ObdBikeDiagnosticScope.shouldRecord(false,"PID"));
  System.out.println("ObdBikeDiagnosticScopeTest PASS "+count);
 }
}
