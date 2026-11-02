package com.motohub.next;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Experimental ELM327 OBD-II mode 01 sensor decoding. No ECU writes or DTC clearing. */
final class ObdReadOnlyProtocol {
 private ObdReadOnlyProtocol() {}
 static final String[] SENSOR_PIDS={"010C","010D","0105","0142","012F"};
 // Standard mode-01 supported PID bitmaps; never request proprietary odometer data.
 static final String[] CAPABILITY_PIDS={"0100","0120","0140","01A0"};
 // 01A6 is used only after explicit developer-mode consent; never polled by auto riding.
 static final String ODOMETER_PID="01A6";
 static final String[] SENSOR_NAMES={"엔진 RPM","차량 속도","냉각수 온도","ECU 전압","연료 잔량 (%)"};
 static String query(String pid){
  for(String safe:SENSOR_PIDS)if(safe.equals(pid))return pid+"\r";
  for(String safe:CAPABILITY_PIDS)if(safe.equals(pid))return pid+"\r";
  if(ODOMETER_PID.equals(pid))return pid+"\r";
  throw new IllegalArgumentException("읽기 전용으로 허용되지 않은 요청입니다.");
 }
 /** A mixed ECU reply is invalid: never let a corrupted speed arm GPS recording. */
 static int speedKmh(String raw){
  int[] bytes=responseBytes("010D",raw);
  return bytes!=null&&bytes[0]<=200?bytes[0]:-1;
 }
 /**
  * ELM327 adapters may include an echoed request and multiple ECU replies.
  * Accept only complete 41/PID frames; every matching reply must agree.
  * A trailing partial frame or conflicting replies are NOT trustworthy data.
  */
 static int[] responseBytes(String pid,String raw){
  query(pid);
  if(!ObdReplyStatus.valid(raw))return null;
  String responsePid=pid.substring(2);
  boolean twoBytes="010C".equals(pid)||"0142".equals(pid);
  java.util.regex.Pattern frame=Pattern.compile("(?i)(?:^|[^0-9a-f])41\\s*"+responsePid+
    "\\s*([0-9a-f]{2})"+(twoBytes?"\\s*([0-9a-f]{2})":"")+"(?![0-9a-f])");
  Matcher m=frame.matcher(raw);
  int[] agreed=null;
  while(m.find()){
   int a=Integer.parseInt(m.group(1),16);
   int b=twoBytes?Integer.parseInt(m.group(2),16):-1;
   if(agreed!=null&&(agreed[0]!=a||agreed[1]!=b))return null;
   agreed=new int[]{a,b};
  }
  return agreed;
 }
 static String decode(String pid,String raw){
  int[] bytes=responseBytes(pid,raw);
  if(bytes==null)return "지원 안 함";
  int a=bytes[0],b=bytes[1];
  if("010C".equals(pid))return String.format(Locale.KOREA,"%,.0f rpm",(a*256+b)/4.0);
  if("0142".equals(pid))return String.format(Locale.KOREA,"%.3f V",(a*256+b)/1000.0);
  if("010D".equals(pid))return a<=200?a+" km/h":"지원 안 함";
  if("0105".equals(pid))return (a-40)+" °C";
  if("012F".equals(pid))return String.format(Locale.KOREA,"%.1f %%",a*100.0/255.0);
  return "지원 안 함";
 }
}
