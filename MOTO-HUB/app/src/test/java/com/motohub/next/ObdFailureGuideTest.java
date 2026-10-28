package com.motohub.next;
import org.junit.Test;
import static org.junit.Assert.*;
public final class ObdFailureGuideTest {
 @Test public void separateSafeReasons(){
  assertTrue(ObdFailureGuide.classify("SecurityException permission").contains("권한"));
  assertTrue(ObdFailureGuide.classify("010D 미지원").contains("PID"));
  assertTrue(ObdFailureGuide.classify("OBD2 응답 완료 신호 없음").contains("ECU"));
  assertTrue(ObdFailureGuide.classify("Bluetooth socket disconnect").contains("Bluetooth"));
  assertTrue(ObdFailureGuide.classify("Connection reset by peer").contains("끊김"));
  assertTrue(ObdFailureGuide.classify("socket closed").contains("끊김"));
  assertTrue(ObdFailureGuide.classify("Timeout waiting for prompt").contains("응답 지연"));
  assertTrue(ObdFailureGuide.classify(null).contains("OBD"));
 }
}
