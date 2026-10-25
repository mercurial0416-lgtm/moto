package com.motohub.next;
final class ObdReadOnlyProtocolTest {
 static int checks=0;
 static void eq(String expected,String actual){if(!expected.equals(actual))throw new AssertionError(expected+" != "+actual);checks++;}
 static void yes(boolean b){if(!b)throw new AssertionError("Unexpected speed result");checks++;}
 static void blocked(String p){try{ObdReadOnlyProtocol.query(p);throw new AssertionError("Allowed unsafe command: "+p);}catch(IllegalArgumentException expected){checks++;}}
 public static void main(String[] args){
  eq("010C\r",ObdReadOnlyProtocol.query("010C"));
  eq("010D\r",ObdReadOnlyProtocol.query("010D"));
  eq("0105\r",ObdReadOnlyProtocol.query("0105"));
  eq("0142\r",ObdReadOnlyProtocol.query("0142"));
  blocked("04");blocked("0104");blocked("03");blocked("08");blocked("22F190");blocked("ATSH7E0");blocked("0142\rATZ");
  eq("1,726 rpm",ObdReadOnlyProtocol.decode("010C","SEARCHING...\r7E8 04 41 0C 1A F8\r>"));
  eq("40 km/h",ObdReadOnlyProtocol.decode("010D","41 0D 28"));
  eq("80 °C",ObdReadOnlyProtocol.decode("0105","410578"));
  eq("12.400 V",ObdReadOnlyProtocol.decode("0142","41 42 30 70"));
  eq("지원 안 함",ObdReadOnlyProtocol.decode("010C","NO DATA"));
  eq("지원 안 함",ObdReadOnlyProtocol.decode("010C","41 0C 01"));
  eq("지원 안 함",ObdReadOnlyProtocol.decode("0142",null));
  yes(40==ObdReadOnlyProtocol.speedKmh("7E8 03 41 0D 28"));
  yes(0==ObdReadOnlyProtocol.speedKmh("410D00"));
  yes(-1==ObdReadOnlyProtocol.speedKmh("NO DATA"));
  yes(-1==ObdReadOnlyProtocol.speedKmh("410DFF"));
  yes(-1==ObdReadOnlyProtocol.speedKmh("410D"));
  System.out.println("OBD read-only checks passed: "+checks);
 }
}
