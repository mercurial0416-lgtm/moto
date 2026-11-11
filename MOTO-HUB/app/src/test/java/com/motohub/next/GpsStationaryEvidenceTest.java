package com.motohub.next;
import org.junit.Test;
import static org.junit.Assert.*;
public class GpsStationaryEvidenceTest {
 @Test public void sustainedStationaryFixesAreVerified(){
  GpsStationaryEvidence gate=new GpsStationaryEvidence();
  for(int i=0;i<=17;i++)gate.accept(i*5000L,37.2+(i%2)*.000001,127.2,4);
  assertTrue(gate.confirmed(85000));assertFalse(gate.confirmed(110001));
 }
 @Test public void walkingNeverCountsAsParking(){
  GpsStationaryEvidence gate=new GpsStationaryEvidence();
  for(int i=0;i<=18;i++)gate.accept(i*5000L,37.2+i*.00001,127.2,5);
  assertFalse(gate.confirmed(90000));
 }
 @Test public void gapsAndBadAccuracyReset(){
  GpsStationaryEvidence gate=new GpsStationaryEvidence();
  for(int i=0;i<=17;i++)gate.accept(i*5000L,37.2,127.2,3);
  gate.accept(90000,37.2,127.2,35);
  assertFalse(gate.confirmed(90000));
  gate.accept(100000,37.2,127.2,4);
  assertFalse(gate.confirmed(100000));
 }
}
