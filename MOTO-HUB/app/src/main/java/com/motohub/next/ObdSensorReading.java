package com.motohub.next;

/** Pure formatting and bounds for volatile on-device telemetry. */
final class ObdSensorReading {
 private ObdSensorReading(){}
 static double number(String pid,String raw){
  int[] bytes=ObdReadOnlyProtocol.responseBytes(pid,raw);
  if(bytes==null)return Double.NaN;
  int a=bytes[0],b=bytes[1];
  switch(pid){
   case "010C":return (a*256+b)/4.0;
   case "010D":return a<=200?a:Double.NaN;
   case "0105":return a-40;
   case "0142":return (a*256+b)/1000.0;
   default:return Double.NaN;
  }
 }
 static boolean valid(String pid,double value){
  if(!Double.isFinite(value))return false;
  switch(pid){
   case "010C":return value>=0&&value<=15000;
   case "010D":return value>=0&&value<=200;
   case "0105":return value>=-40&&value<=215;
   case "0142":return value>=0&&value<=30;
   default:return false;
  }
 }
}
