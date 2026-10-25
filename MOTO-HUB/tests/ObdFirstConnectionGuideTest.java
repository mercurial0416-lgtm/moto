package com.motohub.next;
import java.io.IOException;
import java.net.SocketTimeoutException;
public final class ObdFirstConnectionGuideTest {
 private static int count=0;
 private static void ok(boolean value,String description){
  count++;if(!value)throw new AssertionError(description);
 }
 public static void main(String[] args){
  ok(ObdFirstConnectionGuide.adapterPriority("Vgate iCar Pro") == 0,"Vgate first");
  ok(ObdFirstConnectionGuide.adapterPriority("ELM327") == 0,"ELM first");
  ok(ObdFirstConnectionGuide.adapterPriority("OBDII Bluetooth") == 0,"OBDII first");
  ok(ObdFirstConnectionGuide.adapterPriority("OBD2") == 0,"OBD2 first");
  ok(ObdFirstConnectionGuide.adapterPriority("Mini scanner") == 1,"scanner second");
  ok(ObdFirstConnectionGuide.adapterPriority("Galaxy Buds") == 2,"audio last");
  ok(ObdFirstConnectionGuide.adapterPriority(null) == 2,"null last");
  ok(ObdFirstConnectionGuide.adapterLabel("Vgate").contains("OBD 후보"),"honest candidate label");
  ok(ObdFirstConnectionGuide.result(ObdPidCapabilities.Support.YES,true,1).contains("별도 검증"),"speed read not auto-start validated");
  ok(ObdFirstConnectionGuide.result(ObdPidCapabilities.Support.NO,false,1).contains("불가"),"unsupported speed disables auto-start");
  ok(ObdFirstConnectionGuide.result(ObdPidCapabilities.Support.UNKNOWN,false,1).contains("속도 PID 실측 미확인"),"unknown speed never implied good");
  ok(ObdFirstConnectionGuide.result(ObdPidCapabilities.Support.YES,false,0).contains("아직"),"bluetooth is not ECU validation");
  ok(ObdFirstConnectionGuide.result(ObdPidCapabilities.Support.NO,false,0).contains("미지원"),"no speed supported");
  ok(ObdFirstConnectionGuide.failure(new SecurityException()).contains("권한"),"permission actionable");
  ok(ObdFirstConnectionGuide.failure(new SocketTimeoutException()).contains("동글"),"timeout actionable");
  ok(ObdFirstConnectionGuide.failure(new IOException("ECU 응답을 확인하지 못했습니다.")).contains("ECU 응답을"),"curated IO reason");
  ok(ObdFirstConnectionGuide.failure(new IOException("private.mac.address")).contains("변환 케이블"),"no raw IO identifiers");
  System.out.println("PASS ObdFirstConnectionGuideTest "+count+" checks");
 }
}
