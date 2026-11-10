package com.motohub.next;

import java.util.Locale;

/**
 * Read-only ODO decoder. Neither standard PID A6 nor the standardized
 * UDS/J1979-2 F4A6 DID is guaranteed to be implemented by a motorcycle.
 * A decoded candidate must be read twice and confirmed against the dash
 * before it can be saved. This class does not modify ECU or app state.
 */
final class ObdOdometer {
 static final String STANDARD_REQUEST="01A6\r";
 static final String UDS_REQUEST="22F4A6\r";
 private static final double MAX_REASONABLE_KM=2_000_000.0;

 /** Legacy caller: Mode 01 / PID A6. */
 static double parseKm(String raw) {
  return parse(raw,"41A6");
 }

 /** ReadDataByIdentifier 0xF4A6, 0x62 positive response. */
 static double parseUdsKm(String raw) {
  return parse(raw,"62F4A6");
 }

 private static boolean invalidResponse(String raw) {
  String upper=raw.toUpperCase(Locale.ROOT);
  for(String error:new String[]{"NO DATA","UNABLE TO CONNECT","CAN ERROR",
    "BUS ERROR","BUS INIT: ERROR","STOPPED","BUFFER FULL","DATA ERROR",
    "ACT ALERT","UNSUPPORTED"})
   if(upper.contains(error))return true;
  return upper.indexOf('?')>=0;
 }
 private static double parse(String raw,String marker) {
  if(raw==null||raw.length()>4096||invalidResponse(raw))return Double.NaN;
  Double first=null;
  for(String line:raw.toUpperCase(Locale.ROOT).split("[\r\n>]+")) {
   // Accept headers, CAN data length and optional zero padding, but never
   // infer an ODO from an unlabelled group of bytes.
   String compact=line.replaceAll("[ \t]","");
   int at=compact.indexOf(marker);
   if(at<0)continue;
   // Only trust a full positive reply at the start of a frame or directly
   // after a CAN responder ID with an optional transport byte count.
   // Do not accept a coincidental marker embedded inside unrelated payload.
   String header=compact.substring(0,at);
   int expectedLength=marker.equals("41A6")?6:7;
   if(!header.isEmpty()){
    if(!header.matches("(?:[0-9A-F]{3}|[0-9A-F]{8})(?:[0-9A-F]{2})?"))
     return Double.NaN;
    int canIdDigits=header.length()==3||header.length()==5?3:8;
    if(header.length()>canIdDigits){
     int declared=Integer.parseInt(header.substring(canIdDigits),16);
     if(declared!=expectedLength)return Double.NaN;
    }
   }
   String payload=compact.substring(at+marker.length());
   // Four ODO bytes; an ELM CAN frame can append at most two 00 padding bytes.
   if(!payload.matches("[0-9A-F]{8}(?:00){0,2}"))return Double.NaN;
   long v=Long.parseLong(payload.substring(0,8),16);
   double km=v/10.0;
   if(km<=0||km>MAX_REASONABLE_KM)return Double.NaN;
   if(first!=null&&Double.compare(first,km)!=0)return Double.NaN;
   first=km;
  }
  return first==null?Double.NaN:first;
 }

 static String responseSummary(String raw) {
  if(raw==null||raw.trim().isEmpty())return "무응답";
  String s=raw.replaceAll("[\r\n\t]+"," ").trim();
  return s.length()>125?s.substring(0,125)+"…":s;
 }

 private ObdOdometer(){}
}
