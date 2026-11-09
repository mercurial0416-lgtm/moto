package com.motohub.next;

import java.util.Locale;

/**
 * SAE J1979 Mode 01 PID 31: distance since diagnostic trouble codes were
 * cleared, in integral km, NOT a vehicle lifetime odometer.
 * Read-only. No reset, DTC clear, ECU programming or proprietary commands.
 */
final class DtcDistanceSinceClear {
 static final String PID="0131";
 static final String REQUEST="0131\r";
 static final int MAX_KM=65535;
 private DtcDistanceSinceClear(){}

 static int parseKm(String raw){
  if(raw==null||raw.length()>4096)return -1;
  String upper=raw.toUpperCase(Locale.ROOT);
  for(String bad:new String[]{"NO DATA","UNABLE TO CONNECT","CAN ERROR","BUS ERROR",
   "STOPPED","BUFFER FULL","DATA ERROR","UNSUPPORTED","ACT ALERT"})
   if(upper.contains(bad))return -1;
  if(upper.indexOf('?')>=0)return -1;
  int found=-1;
  for(String line:upper.split("[\r\n>]+")){
   String compact=line.replaceAll("[ \t]","");
   int idx=compact.indexOf("4131");
   if(idx<0)continue;
   String before=compact.substring(0,idx);
   if(!before.isEmpty()){
    if(!before.matches("(?:[0-9A-F]{3}|[0-9A-F]{8})(?:[0-9A-F]{2})?"))return -1;
    int idLength=(before.length()==3||before.length()==5)?3:8;
    if(before.length()>idLength&&Integer.parseInt(before.substring(idLength),16)!=4)return -1;
   }
   String payload=compact.substring(idx+4);
   // 2 data bytes; optional ELM CAN 00 padding. Reject unrelated trailing bytes.
   if(!payload.matches("[0-9A-F]{4}(?:00){0,4}"))return -1;
   int value=Integer.parseInt(payload.substring(0,4),16);
   if(found>=0&&found!=value)return -1; // conflicting ECUs
   found=value;
  }
  return found;
 }
}
