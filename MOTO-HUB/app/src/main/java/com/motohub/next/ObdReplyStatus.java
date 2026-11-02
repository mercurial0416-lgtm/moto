package com.motohub.next;

import java.util.Locale;

/** Keep stale or contradictory ELM327 replies from being interpreted as valid ECU data. */
final class ObdReplyStatus {
 private ObdReplyStatus(){}
 static String error(String response){
  if(response==null||response.isEmpty())return "응답 없음";
  if(response.length()>4096)return "응답 길이 초과";
  String text=response.toUpperCase(Locale.ROOT);
  for(String value:new String[]{"NO DATA","UNABLE TO CONNECT","CAN ERROR","BUS ERROR",
    "BUS INIT: ERROR","STOPPED","BUFFER FULL","DATA ERROR","ACT ALERT"}){
   if(text.contains(value))return value;
  }
  // Adapter '?' means the query was rejected. It is not a supported PID.
  if(text.indexOf('?')>=0)return "ELM 명령 거부";
  return "";
 }
 static boolean valid(String response){return error(response).isEmpty();}
}
