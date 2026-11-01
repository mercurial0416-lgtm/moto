package com.motohub.next;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/** Standard mode-01 supported-PID bitmap. UNKNOWN must never be treated as "not supported". */
final class ObdPidCapabilities {
 private static final Pattern REPLY=Pattern.compile("(?i)41\\s*(00|20|40|A0)\\s*([0-9a-f]{2})\\s*([0-9a-f]{2})\\s*([0-9a-f]{2})\\s*([0-9a-f]{2})(?![0-9a-f])");
 enum Support { YES, NO, UNKNOWN }
 private final Long mode00,mode20,mode40,modeA0;
 private ObdPidCapabilities(Long first,Long twenty,Long second,Long third){mode00=first;mode20=twenty;mode40=second;modeA0=third;}
 static ObdPidCapabilities unknown(){return new ObdPidCapabilities(null,null,null,null);}
 static ObdPidCapabilities fromResponses(String first,String second){return fromResponses(first,second,null);}
 static ObdPidCapabilities fromResponses(String first,String second,String a0){return new ObdPidCapabilities(bitmap("00",first),null,bitmap("40",second),bitmap("A0",a0));}
 static ObdPidCapabilities fromAllResponses(String first,String twenty,String second,String a0){
  return new ObdPidCapabilities(bitmap("00",first),bitmap("20",twenty),bitmap("40",second),bitmap("A0",a0));}
 private static Long bitmap(String header,String response){
  if(response==null||response.length()>4096)return null;
  Matcher match=REPLY.matcher(response);
  Long agreed=null;
  while(match.find())if(header.equalsIgnoreCase(match.group(1))){
   String hex=match.group(2)+match.group(3)+match.group(4)+match.group(5);
   long bitmap=Long.parseLong(hex,16);
   // Different ECUs/adapters claiming different PID support is ambiguous.
   if(agreed!=null&&agreed.longValue()!=bitmap)return null;
   agreed=bitmap;
  }
  return agreed;
 }
 Support forPid(String pid){
  if(pid==null||!pid.matches("01[0-9A-Fa-f]{2}"))return Support.UNKNOWN;
  int n=Integer.parseInt(pid.substring(2),16);
  if(n>=1&&n<=32&&mode00!=null)return ((mode00&(1L<<(32-n)))!=0)?Support.YES:Support.NO;
  if(n>=33&&n<=64&&mode20!=null)return ((mode20&(1L<<(64-n)))!=0)?Support.YES:Support.NO;
  if(n>=65&&n<=96&&mode40!=null)return ((mode40&(1L<<(96-n)))!=0)?Support.YES:Support.NO;
  if(n>=161&&n<=192&&modeA0!=null)return ((modeA0&(1L<<(192-n)))!=0)?Support.YES:Support.NO;
  return Support.UNKNOWN;
 }
 private static String state(Support x){return x==Support.YES?"지원":x==Support.NO?"미지원":"확인 불가";}
 String summary(){return String.format(Locale.KOREA,"속도 %s · RPM %s · 수온 %s · 전압 %s · 누적거리(ODO) 미검증",
   state(forPid("010D")),state(forPid("010C")),state(forPid("0105")),state(forPid("0142")))+" · 연료 잔량 "+state(forPid("012F"));}
 private ObdPidCapabilities() {mode00=null;mode20=null;mode40=null;modeA0=null;}
}
