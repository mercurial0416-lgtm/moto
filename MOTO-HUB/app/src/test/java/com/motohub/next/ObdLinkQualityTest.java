package com.motohub.next;
import org.junit.Test;
import static org.junit.Assert.*;
public final class ObdLinkQualityTest {
 @Test public void staleOrMissingIsNeverScoredGood(){
  assertEquals(0,ObdLinkQuality.score(0,2000,50,15,0,0,1,1));
  assertEquals(0,ObdLinkQuality.score(1000,25000,50,15,0,0,1,1));
  assertTrue(ObdLinkQuality.label(0,0).contains("평가 보류"));
 }
 @Test public void faultsAndReconnectsLowerEstimate(){
  int good=ObdLinkQuality.score(1000,1400,200,100,0,0,1,1);
  int bad=ObdLinkQuality.score(1000,1400,1700,100,3,4,6,2);
  assertEquals(100,good);assertTrue(bad<good);
  assertTrue(ObdLinkQuality.label(bad,100).contains("품질 추정"));
 }
}
