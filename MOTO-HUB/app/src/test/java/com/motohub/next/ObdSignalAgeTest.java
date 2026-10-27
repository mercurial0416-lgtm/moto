package com.motohub.next;
import org.junit.Test;
import static org.junit.Assert.*;
public final class ObdSignalAgeTest {
 @Test public void lastValidSampleOrMissing(){
  assertEquals("응답 대기",ObdSignalAge.label(0,1000));
  assertEquals("응답 대기",ObdSignalAge.label(2000,1000));
  assertEquals("방금 수신",ObdSignalAge.label(1000,2500));
  assertEquals("마지막 응답 12초 전",ObdSignalAge.label(1000,13_000));
  assertEquals("10분 이상 신호 없음",ObdSignalAge.label(1000,700_000));
 }
}
