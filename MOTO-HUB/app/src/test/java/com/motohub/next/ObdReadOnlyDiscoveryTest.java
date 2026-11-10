package com.motohub.next;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObdReadOnlyDiscoveryTest {
 private static ObdReadOnlyDiscovery.Request scripted(Map<String,String> replies,List<String> commands){
  return (cmd,timeout)->{
   commands.add(cmd);
   if(!cmd.startsWith("01")||!cmd.endsWith("\r"))throw new AssertionError("unsafe command: "+cmd);
   String result=replies.get(cmd.trim());
   if(result==null)throw new java.io.IOException("unscripted "+cmd);
   return result;
  };
 }

 @Test public void discoversSupportedPagesAndOnlyReadsAdvertisedPids()throws Exception{
  Map<String,String> replies=new HashMap<>();
  replies.put("0100","410000180001"); // 0C RPM, 0D speed, extension 20
  replies.put("0120","412000020000"); // 2F fuel, no extension
  replies.put("010C","410C1F40");
  replies.put("010D","410D00");
  replies.put("012F","412F80");
  List<String> commands=new ArrayList<>();
  ObdReadOnlyDiscovery.Result result=ObdReadOnlyDiscovery.all(scripted(replies,commands),(a,b,c)->{},()->false);
  assertEquals(3,result.advertised);
  assertEquals(3,result.readings.size());
  assertEquals(Arrays.asList("0100\r","0120\r","010C\r","010D\r","012F\r"),commands);
  assertTrue(result.readings.get(0).value.contains("2000 rpm"));
  assertTrue(result.readings.get(2).value.contains("%"));
 }

 @Test public void onlyAcceptsSingleAgreedMatchedFrames(){
  assertNull(ObdReadOnlyDiscovery.interpret("010C","410C1F40\n410C1020"));
  assertNull(ObdReadOnlyDiscovery.interpret("010D","NO DATA"));
  assertNull(ObdReadOnlyDiscovery.interpret("010D","410C00"));
  assertEquals("0 km/h",ObdReadOnlyDiscovery.interpret("010D","41 0D 00").value);
  assertFalse(ObdReadOnlyDiscovery.interpret("0103","41030100").decoded);
 }

 @Test public void standardOdometerOnlyWhenAdvertisedIsNeverAutoCommitted()throws Exception{
  Map<String,String> replies=new HashMap<>();
  replies.put("0100","410000000001");
  replies.put("0120","412000000001");
  replies.put("0140","414000000001");
  replies.put("0160","416000000001");
  replies.put("0180","418000000001");
  replies.put("01A0","41A004000000");
  replies.put("01A6","41A60001E240");
  List<String> commands=new ArrayList<>();
  ObdReadOnlyDiscovery.Result result=ObdReadOnlyDiscovery.all(scripted(replies,commands),(a,b,c)->{},()->false);
  assertEquals(1,result.readings.size());
  assertEquals("01A6",result.readings.get(0).pid);
  assertTrue(result.readings.get(0).value.contains("12345.6 km"));
  assertTrue(result.readings.get(0).value.contains("자동 반영 안 함"));
 }

 @Test public void unsafeCommandsAreRejected(){
  for(String value:new String[]{"04","220001","09FF","ATZ","01FF","010C\n04","01a6",""}) {
   try{ObdReadOnlyDiscovery.command(value);fail("accepted "+value);}
   catch(IllegalArgumentException expected){}
  }
  assertEquals("01A6\r",ObdReadOnlyDiscovery.command("01A6"));
 }

 @Test public void selectedOnlyRefreshSkipsBitmapAndDoesNotPersistValues()throws Exception{
  Map<String,String> replies=new HashMap<>();
  replies.put("010D","410D00");
  replies.put("010C","NO DATA");
  List<String> commands=new ArrayList<>();
  ObdReadOnlyDiscovery.Result result=ObdReadOnlyDiscovery.selected(scripted(replies,commands),
    Arrays.asList("010D","010C"),(a,b,c)->{},()->false);
  assertEquals(1,result.readings.size());
  assertEquals(1,result.rejected);
  assertEquals(Arrays.asList("010D\r","010C\r"),commands);
 }
}