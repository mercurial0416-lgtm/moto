package com.motohub.next;
final class ObdCeHardeningTest {
 static int checks=0;
 static void yes(boolean b,String why){if(!b)throw new AssertionError(why);checks++;}
 static void close(double a,double b){yes(Math.abs(a-b)<.001,"expected "+b+", got "+a);}
 public static void main(String[] args){
  yes(ObdConnectionPolicy.shouldRetry(1,true,true),"retry 1");
  yes(ObdConnectionPolicy.shouldRetry(3,true,true),"retry 3");
  yes(ObdConnectionPolicy.shouldRetry(8,true,true),"last retry");
  yes(!ObdConnectionPolicy.shouldRetry(9,true,true),"max retries");
  yes(!ObdConnectionPolicy.shouldRetry(1,false,true),"manual disconnect");
  yes(!ObdConnectionPolicy.shouldRetry(1,true,false),"unsupported PID");
  yes(ObdConnectionPolicy.delayMs(1)==1500,"backoff 1");
  yes(ObdConnectionPolicy.delayMs(3)==6000,"backoff 3");
  yes(ObdConnectionPolicy.delayMs(8)==20000,"capped backoff");
  close(ObdSensorReading.number("010C","41 0C 1A F8"),1726.0);
  close(ObdSensorReading.number("010C","410C1AF8>"),1726.0);
  close(ObdSensorReading.number("0105","41 05 65"),61);
  close(ObdSensorReading.number("0142","41 42 34 12"),13.33);
  close(ObdSensorReading.number("010D","41 0D 21"),33);
  yes(Double.isNaN(ObdSensorReading.number("010C","NO DATA")),"no data");
  yes(!ObdSensorReading.valid("010C",Double.POSITIVE_INFINITY),"non finite");
  yes(!ObdSensorReading.valid("010D",201),"unsafe speed");
  yes(ObdSensorReading.valid("0142",12.6),"ecu voltage");
  boolean refused=false;try{ObdReadOnlyProtocol.query("04FF");}catch(IllegalArgumentException e){refused=true;}
  yes(refused,"no write commands");
  ObdRideAutoGate gate=new ObdRideAutoGate();
  gate.accept(8,1000);gate.accept(8,2000);
  yes(gate.accept(8,3000)==ObdRideAutoGate.Event.START,"start");
  gate.accept(0,4000);gate.connectionLost();
  yes(gate.recording(),"connection loss preserves ongoing ride");
  yes(!gate.stoppedBeforeDisconnect(1000000),"stale idle is not proof of finish");
  yes(gate.accept(15,1000000)==ObdRideAutoGate.Event.NONE,"reconnect should not split ride");
  yes(gate.accept(0,1001000)==ObdRideAutoGate.Event.NONE,"fresh idle time");
  for(long tick=1002000;tick<1181000;tick+=1000)
   yes(gate.accept(0,tick)==ObdRideAutoGate.Event.NONE,"stationary until 3 minutes");
  yes(gate.accept(0,1181000)==ObdRideAutoGate.Event.FINISH,"safe stop after fresh 3 minutes");
  yes(RideSpeedSegments.category(1000,2,5,5)==RideSpeedSegments.UNKNOWN,"no GPS jitter");
  yes(RideSpeedSegments.category(31000,200,5,5)==RideSpeedSegments.UNKNOWN,"GPS gap");
  yes(RideSpeedSegments.category(10000,60,5,5)==RideSpeedSegments.SLOW,"slow speed");
  yes(RideSpeedSegments.category(5000,60,5,5)==RideSpeedSegments.CRUISE,"cruise speed");
  yes(RideSpeedSegments.category(2000,60,5,5)==RideSpeedSegments.FAST,"fast speed");
  System.out.println("CE OBD2/reconnect/map checks: "+checks);
 }
}
