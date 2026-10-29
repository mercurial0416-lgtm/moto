package com.motohub.next;
import org.junit.Test;
import static org.junit.Assert.*;
public final class Ux76ConnectionStateTest {
 @Test public void mustNotCallOldDataCurrent(){
  assertTrue(ObdConnectionState.label(true,1000,40000,2,1,0,"없음","").contains("응답 지연"));
 }
 @Test public void reportsRetryAndUnknown(){
  assertTrue(ObdConnectionState.label(true,0,90000,4,2,2,"없음","").contains("재접속"));
  assertTrue(ObdConnectionState.label(true,0,90000,2,1,0,"010D unsupported","").contains("미지원"));
 }
 @Test public void liveAndDisabledDiffer(){
  assertTrue(ObdConnectionState.label(true,1000,2000,1,1,0,"없음","").contains("수신 중"));
  assertTrue(ObdConnectionState.label(false,1000,2000,1,1,0,"없음","").contains("꺼짐"));
 }
}
