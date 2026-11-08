package com.motohub.next;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One-shot, user-initiated discovery of SAE Mode 01 read-only data.
 * No 02/03/04/09, UDS, ECU write, reset, actuator, or arbitrary AT commands.
 * Results exist only in memory; callers may persist selected PID identifiers, not ECU data.
 */
final class ObdReadOnlyDiscovery {
 interface Request { String ask(String command, long timeoutMs) throws Exception; }
 interface Progress { void report(int completed, int total, int valid); }

 static final class Reading {
  final String pid;
  final String name;
  final String value;
  final boolean decoded;
  Reading(String pid, String name, String value, boolean decoded) {
   this.pid=pid; this.name=name; this.value=value; this.decoded=decoded;
  }
  String label(){return pid+" · "+name+"\n"+value+(decoded?"":" · 원시 응답 (단위 미확인)");}
 }
 static final class Result {
  final int advertised, attempted, rejected;
  final List<Reading> readings;
  Result(int advertised, int attempted, int rejected, List<Reading> readings){
   this.advertised=advertised; this.attempted=attempted; this.rejected=rejected; this.readings=readings;
  }
 }
 private ObdReadOnlyDiscovery(){}

 /** Never put an arbitrary user-supplied command on the ELM wire. */
 static String command(String pid){
  if(pid==null||!pid.matches("01[0-9A-F]{2}"))throw new IllegalArgumentException("읽기 전용 Mode 01 PID만 허용합니다.");
  int n=Integer.parseInt(pid.substring(2),16);
  if(n==0xff)throw new IllegalArgumentException("예약된 PID입니다.");
  return pid+"\r";
 }

 static List<String> supported(Request request, BooleanSupplier cancel) throws Exception {
  Set<String> pids=new LinkedHashSet<>();
  for(int base=0;base<=0xe0;base+=0x20){
   if(cancel.getAsBoolean())throw new InterruptedException("센서 탐색 중지");
   String pid=String.format(Locale.ROOT,"01%02X",base);
   Long bits=parseBitmap(base,request.ask(command(pid),2500));
   if(bits==null){
    if(base==0)throw new IOException("ECU 표준 PID 지원 목록(0100)을 읽을 수 없습니다.");
    break; // A missing later page is not proof that any of its PIDs are unsupported.
   }
   for(int bit=0;bit<32;bit++){
    int n=base+bit+1;
    if(n<=0xfe&&(bits&(1L<<(31-bit)))!=0&&(n%32)!=0)
     pids.add(String.format(Locale.ROOT,"01%02X",n));
   }
   // Each extension bitmap may only be requested when the preceding one advertises it.
   if((bits&1L)==0)break;
  }
  return new ArrayList<>(pids);
 }

 static Result all(Request request,Progress progress,BooleanSupplier cancel) throws Exception{
  return read(request,supported(request,cancel),progress,cancel);
 }
 static Result selected(Request request,List<String> selected,Progress progress,BooleanSupplier cancel) throws Exception{
  return read(request,selected,progress,cancel);
 }
 private static Result read(Request request,List<String> pids,Progress progress,BooleanSupplier cancel) throws Exception{
  Set<String> unique=new LinkedHashSet<>(pids);
  List<Reading> found=new ArrayList<>();
  int done=0,rejected=0;
  for(String pid:unique){
   if(cancel.getAsBoolean())throw new InterruptedException("센서 탐색 중지");
   try{
    String response=request.ask(command(pid),2200);
    Reading reading=interpret(pid,response);
    if(reading!=null)found.add(reading);
    else rejected++;
   }catch(InterruptedException e){throw e;}
   catch(Exception error){
    if(cancel.getAsBoolean())throw new InterruptedException("센서 탐색 중지");
    rejected++;
   }
   done++;
   progress.report(done,unique.size(),found.size());
   if(done<unique.size())Thread.sleep(55); // Bound traffic; no continuous full-scan loop.
  }
  return new Result(unique.size(),done,rejected,found);
 }

 /** Only complete matching 41/PID replies are displayed; no fabricated sensor values. */
 static Reading interpret(String pid,String raw){
  command(pid);
  if(raw==null||raw.length()>4096||!ObdReadOnlyProtocol.replyError(raw).isEmpty())return null;
  String marker="41"+pid.substring(2);
  byte[] bytes=null;
  for(String line:raw.toUpperCase(Locale.ROOT).split("[\r\n>]+")){
   String compact=line.replaceAll("[ \t]","");
   int index=compact.indexOf(marker);
   if(index<0)continue;
   String value=compact.substring(index+marker.length());
   if(!value.matches("(?:[0-9A-F]{2}){1,32}"))return null;
   byte[] candidate=new byte[value.length()/2];
   for(int i=0;i<candidate.length;i++)candidate[i]=(byte)Integer.parseInt(value.substring(i*2,i*2+2),16);
   if(bytes!=null&&!java.util.Arrays.equals(bytes,candidate))return null;
   bytes=candidate;
  }
  if(bytes==null)return null;
  String decoded=decode(pid,bytes);
  String hex=toHex(bytes);
  return new Reading(pid,name(pid),decoded==null?"HEX "+hex:decoded,decoded!=null);
 }

 private static Long parseBitmap(int base,String raw){
  if(raw==null||raw.length()>4096||!ObdReadOnlyProtocol.replyError(raw).isEmpty())return null;
  String target=String.format(Locale.ROOT,"%02X",base);
  Pattern p=Pattern.compile("(?i)(?:^|[^0-9a-f])41\\s*"+target+
    "\\s*((?:[0-9a-f]{2}[ \\t]*){4})(?![0-9a-f])");
  Long agreed=null;
  for(String line:raw.split("[\r\n>]+")){
   Matcher m=p.matcher(line);
   while(m.find()){
    String bytes=m.group(1).replaceAll("\\s+","");
    if(bytes.length()!=8)continue;
    long value=Long.parseLong(bytes,16);
    if(agreed!=null&&agreed.longValue()!=value)return null;
    agreed=value;
   }
  }
  return agreed;
 }

 private static int u(byte[] b,int i){return b[i]&255;}
 private static int two(byte[] b){return u(b,0)*256+u(b,1);}
 private static String fmt(String pattern,double n){return String.format(Locale.KOREA,pattern,n);}
 private static String decode(String pid,byte[] b){
  if(b.length==0)return null;
  int a=u(b,0);int n=b.length;
  switch(pid){
   case "0101": return n>=4?"MIL "+(((a&0x80)!=0)?"켜짐":"꺼짐")+" · DTC "+(a&0x7f)+"개":null;
   case "0104":case "0111":case "012C":case "012E":case "012F":
   case "0145":case "0147":case "0148":case "0149":case "014A":
   case "014B":case "014C":case "015A":case "015B":
    return fmt("%.1f %%",a*100.0/255.0);
   case "0105":case "010F":case "0146":case "015C":return (a-40)+" °C";
   case "0106":case "0107":case "0108":case "0109":
    return fmt("%.1f %%",(a-128)*100.0/128.0);
   case "010A":return (3*a)+" kPa";
   case "010B":case "0133":return a+" kPa";
   case "010C":return n>=2?fmt("%.0f rpm",two(b)/4.0):null;
   case "010D":return a<=200?a+" km/h":null;
   case "010E":return fmt("%.1f °",a/2.0-64.0);
   case "0110":return n>=2?fmt("%.2f g/s",two(b)/100.0):null;
   case "011F":return n>=2?two(b)+" s":null;
   case "0121":case "0131":return n>=2?two(b)+" km":null;
   case "0130":return a+"회";
   case "0142":return n>=2?fmt("%.3f V",two(b)/1000.0):null;
   case "0143":return n>=2?fmt("%.1f %%",two(b)*100.0/255.0):null;
   case "0144":return n>=2?fmt("%.3f λ",two(b)/32768.0):null;
   case "014D":case "014E":return n>=2?two(b)+" min":null;
   case "015E":return n>=2?fmt("%.2f L/h",two(b)/20.0):null;
   case "01A6":
    if(n!=4)return null;
    long raw=((long)u(b,0)<<24)|((long)u(b,1)<<16)|((long)u(b,2)<<8)|u(b,3);
    double km=raw/10.0;
    return km>0&&km<=2_000_000?fmt("%.1f km (계기판 대조 전, 자동 반영 안 함)",km):null;
   default:return null; // Other standard PIDs remain available as raw HEX, not guessed units.
  }
 }
 private static String toHex(byte[] data){
  StringBuilder sb=new StringBuilder();
  for(byte b:data){if(sb.length()>0)sb.append(' ');sb.append(String.format(Locale.ROOT,"%02X",b&255));}
  return sb.toString();
 }
 static String name(String pid){
  switch(pid){
   case "0101":return "엔진 경고등·고장코드 개수";
   case "0103":return "연료제어 상태";
   case "0104":return "엔진 부하";
   case "0105":return "냉각수 온도";
   case "0106":return "단기 연료보정 1";
   case "0107":return "장기 연료보정 1";
   case "0108":return "단기 연료보정 2";
   case "0109":return "장기 연료보정 2";
   case "010A":return "연료 압력";
   case "010B":return "흡기 매니폴드 압력";
   case "010C":return "엔진 RPM";
   case "010D":return "차량 속도";
   case "010E":return "점화시기";
   case "010F":return "흡기 온도";
   case "0110":return "흡입 공기량";
   case "0111":return "스로틀 개도";
   case "011F":return "시동 후 경과 시간";
   case "0121":return "경고등 점등 중 주행거리 (총 ODO 아님)";
   case "012C":return "EGR 명령";
   case "012E":return "증발가스 퍼지";
   case "012F":return "연료량 비율 (지원차량만)";
   case "0130":return "고장코드 삭제 후 엔진 워밍업 횟수";
   case "0131":return "고장코드 삭제 후 주행거리 (총 ODO 아님)";
   case "0133":return "대기압";
   case "0142":return "ECU 전압";
   case "0143":return "절대 엔진 부하";
   case "0144":return "명령 공연비 람다";
   case "0145":return "상대 스로틀 개도";
   case "0146":return "외기 온도";
   case "0147":return "스로틀 위치 B";
   case "0148":return "스로틀 위치 C";
   case "0149":return "가속 페달 위치 D";
   case "014A":return "가속 페달 위치 E";
   case "014B":return "가속 페달 위치 F";
   case "014C":return "스로틀 액추에이터 명령";
   case "014D":return "경고등 점등 시간";
   case "014E":return "고장코드 삭제 후 시간";
   case "015A":return "상대 가속페달 위치";
   case "015B":return "하이브리드 배터리 잔량";
   case "015C":return "엔진오일 온도";
   case "015E":return "엔진 연료 소모 속도";
   case "01A6":return "ECU 총 누적거리(지원차량만)";
   default:return "표준 OBD 데이터 "+pid;
  }
 }
}