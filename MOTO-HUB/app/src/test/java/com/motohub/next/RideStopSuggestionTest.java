package com.motohub.next;

import org.junit.Test;
import static org.junit.Assert.*;

public final class RideStopSuggestionTest {
 private static final long T=1_000_000L;
 private boolean eligible(long sinceLastMotion,boolean gpsFresh,double km,
                          double speed,boolean shown){
  return RideStopSuggestion.shouldSuggest(
   T+20*60_000L,T,T+20*60_000L-sinceLastMotion,gpsFresh,km,speed,shown);
 }
 @Test public void promptsOnlyForCompletedStationaryInterval(){
  assertFalse(eligible(11*60_000L,true,3.2,0,false));
  assertTrue(eligible(12*60_000L,true,3.2,0,false));
  assertFalse(eligible(12*60_000L,true,3.2,0,true));
 }
 @Test public void noFalsePromptForTrafficOrSignalLoss(){
  assertFalse(eligible(20*60_000L,false,3.2,0,false));
  assertFalse(eligible(20*60_000L,true,0.1,0,false));
  assertFalse(eligible(20*60_000L,true,3.2,9,false));
  assertFalse(eligible(20*60_000L,true,3.2,Double.NaN,false));
 }
 @Test public void neverUseInvalidClockOrMotion(){
  assertFalse(RideStopSuggestion.shouldSuggest(T,T+1,T,true,1,0,false));
  assertFalse(RideStopSuggestion.shouldSuggest(T+30*60_000L,T,T-1,true,1,0,false));
  assertFalse(RideStopSuggestion.shouldSuggest(T+30*60_000L,T,T+40*60_000L,true,1,0,false));
 }
 @Test public void motionDebouncesLowSpeedNoise(){
  assertFalse(RideStopSuggestion.moving(4.9));
  assertTrue(RideStopSuggestion.moving(8.0));
  assertFalse(RideStopSuggestion.moving(Double.POSITIVE_INFINITY));
  assertFalse(RideStopSuggestion.moving(251));
 }
}
