package com.motohub.next;
import static org.junit.Assert.*;import org.junit.Test;
public class CardoGpsFallbackGateTest {
 @Test public void requiresSustainedSeventyMetresWithHighAccuracy(){
  CardoGpsFallbackGate g=new CardoGpsFallbackGate();
  assertFalse(g.accept(5000,25,6,6,false));
  assertFalse(g.accept(5000,25,6,6,false));
  assertTrue(g.accept(5000,25,6,6,false));
 }
 @Test public void walkingDriftInaccurateAndMocklikeSpeedCannotStart(){
  CardoGpsFallbackGate g=new CardoGpsFallbackGate();
  for(int i=0;i<5;i++)assertFalse(g.accept(5000,8,5,5,false));
  assertFalse(g.accept(5000,500,5,5,false));
  assertFalse(g.accept(5000,25,5,41,false));
  assertFalse(g.accept(15000,25,5,5,false));
 }
 @Test public void realEcuSpeedCancelsGpsTakeover(){
  CardoGpsFallbackGate g=new CardoGpsFallbackGate();
  assertFalse(g.accept(5000,25,5,5,false));
  assertFalse(g.accept(5000,25,5,5,true));
  assertFalse(g.accept(5000,25,5,5,false));
  assertFalse(g.accept(5000,25,5,5,false));
  assertTrue(g.accept(5000,25,5,5,false));
 }
 @Test public void slowBikeLaunchNeedsAccurateLongMovement(){
  CardoGpsFallbackGate g=new CardoGpsFallbackGate();
  for(int i=0;i<5;i++)assertFalse(g.accept(7000,17,5,5,false));
  assertTrue(g.accept(7000,17,5,5,false));
 }
 @Test public void lowSpeedPoorAccuracyDoesNotTrigger(){
  CardoGpsFallbackGate g=new CardoGpsFallbackGate();
  for(int i=0;i<12;i++)assertFalse(g.accept(7000,18,18,18,false));
 }
 @Test public void implausibleGapResetsEvidence(){
  CardoGpsFallbackGate g=new CardoGpsFallbackGate();
  assertFalse(g.accept(5000,25,5,5,false));
  assertFalse(g.accept(25000,25,5,5,false));
  assertFalse(g.accept(5000,25,5,5,false));
 }
}