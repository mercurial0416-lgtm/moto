package com.motohub.next;
import java.util.*;
public final class OdometerOcrCandidatesTest {
 static int checks;
 static void check(String text,String expected){
  checks++;List<String> got=OdometerOcrCandidates.fromText(text);
  if(expected==null&&!got.isEmpty()||expected!=null&&(got.isEmpty()||!expected.equals(got.get(0))))
   throw new AssertionError("OCR "+text+" got "+got+" expected "+expected);
 }
 public static void main(String[] args){
  check("ODO\n12,345 km","12345");
  check("ODO 23456.7 km","23456.7");
  check("TRIP A 352.1 km\nODO 15420 km","15420");
  check("RPM 6500\nODO 15000 km","15000");
  check("연료량 4.5L\n주행거리 4200km","4200");
  check("거리 2km",null);
  check("잔여 range 120km",null);
  check("23:48",null);
  check("12540","12540");
  check("fuel total 45000",null);
  check("ODO 99999999 km",null);
  check(null,null);
  System.out.println("PASS odometer OCR candidates "+checks);
 }
}
