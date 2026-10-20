package com.motohub.next;
public final class LocalDiagnosticsTest {
 static int assertions=0;
 static void check(boolean ok){assertions++;if(!ok)throw new AssertionError("privacy report #"+assertions);}
 public static void main(String[] args){
  String r=LocalDiagnosticReport.create("4.0.0-alpha6-dj",true,true,false,false,true,false,false,true,false,-123);
  check(r.contains("고정 서명: 예"));check(r.contains("GPS 제공자: 아니요"));
  check(r.contains("그룹 공유 상태: 꺼짐"));check(r.contains("좌표 수: 0"));
  check(!r.contains("-123"));check(!r.contains("https://"));check(!r.contains("@"));
  String hostile=LocalDiagnosticReport.create("4.0.0\nGPS 37.123, 127.2 token=SECRET",false,false,false,false,false,false,false,false,false,123);
  check(!hostile.contains("GPS 37"));check(!hostile.contains("token"));check(!hostile.contains("SECRET"));check(!hostile.contains("37.123"));
  check(hostile.contains("좌표 수: 123"));
  String big=LocalDiagnosticReport.create("!".repeat(500),true,true,true,true,true,true,true,true,true,Integer.MAX_VALUE);
  check(big.length()<1200);check(big.contains("좌표 수: 2147483647"));
  System.out.println("PASS local diagnostic allowlist: "+assertions);
 }
}
