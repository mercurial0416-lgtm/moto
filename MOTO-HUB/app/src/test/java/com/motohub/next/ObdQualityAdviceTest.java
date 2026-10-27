package com.motohub.next;
import org.junit.Test;
import static org.junit.Assert.*;
public final class ObdQualityAdviceTest{
 @Test public void prioritizeLostSignal(){
  assertTrue(ObdQualityAdvice.explain(false,200,0,0).contains("Bluetooth"));
  assertTrue(ObdQualityAdvice.explain(true,1700,0,0).contains("지연"));
  assertTrue(ObdQualityAdvice.explain(true,200,4,0).contains("재연결"));
 }
}
