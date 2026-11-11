package com.motohub.next;
import org.junit.Test;
import static org.junit.Assert.*;
public class V605StartHandshakeTest {
 @Test public void timeWindowRejectsFutureAndStaleRequests(){
  assertFalse(CardoStartHandshake.within(100,101,1000));
  assertFalse(CardoStartHandshake.within(200,0,1000));
  assertTrue(CardoStartHandshake.within(300,200,1000));
  assertFalse(CardoStartHandshake.within(1300,200,1000));
 }
}
