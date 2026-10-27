package com.motohub.next;
import org.junit.Test;
import static org.junit.Assert.*;
public final class RideScreenRefreshPolicyTest{
 @Test public void slowAndStaleDisplayDoesNotPollAtOneSecond(){
  assertTrue(RideScreenRefreshPolicy.interval(false,0,true)>=3000);
  assertTrue(RideScreenRefreshPolicy.interval(true,0,true)>=2000);
  assertTrue(RideScreenRefreshPolicy.interval(true,80,true)>=1000);
 }
}
