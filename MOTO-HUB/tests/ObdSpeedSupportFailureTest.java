package com.motohub.next;
public final class ObdSpeedSupportFailureTest {
 public static void main(String[] args){
  check(ObdSpeedSupportFailure.confirmedUnsupported("차량 속도 PID 010D 미지원"),"declared no speed");
  check(!ObdSpeedSupportFailure.confirmedUnsupported(null),"missing reason");
  check(!ObdSpeedSupportFailure.confirmedUnsupported("OBD2 연결 실패"),"transient adapter failure");
  check(!ObdSpeedSupportFailure.confirmedUnsupported("차량 속도 응답 6회 연속 무효 · 안전하게 재연결합니다."),"transient response loss");
  check(!ObdSpeedSupportFailure.confirmedUnsupported("차량 속도 PID 010D 미지원 (미검증)"),"unverified capability");
  System.out.println("ObdSpeedSupportFailureTest PASS");
 }
 static void check(boolean test,String description){if(!test)throw new AssertionError(description);}
}
