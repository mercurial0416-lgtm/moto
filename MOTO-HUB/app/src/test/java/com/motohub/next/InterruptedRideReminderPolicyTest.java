package com.motohub.next;
import org.junit.Test;
import static org.junit.Assert.*;
public final class InterruptedRideReminderPolicyTest {
 @Test public void neverLoseTrackJustBecauseLaterWasClicked(){
  String file="1793100000000-11111111-1111-1111-1111-111111111111.track";
  long now=1793100000000L;
  assertTrue(InterruptedRideReminderPolicy.shouldShow(file,"","",0,now));
  assertFalse(InterruptedRideReminderPolicy.shouldShow(file,"",file,now,now+500));
  assertFalse(InterruptedRideReminderPolicy.shouldShow(file,"",file,now,now+23*3600_000L));
  assertTrue(InterruptedRideReminderPolicy.shouldShow(file,"",file,now,now+24*3600_000L));
  assertFalse(InterruptedRideReminderPolicy.shouldShow(file,file,"",0,now));
  assertTrue(InterruptedRideReminderPolicy.shouldShow(file+"2","",file,now,now+500));
  assertFalse(InterruptedRideReminderPolicy.shouldShow(file,"",file,now,now-500));
 }
 @Test public void invalidCandidateIsIgnored(){
  assertFalse(InterruptedRideReminderPolicy.shouldShow("","","",0,1));
  assertFalse(InterruptedRideReminderPolicy.shouldShow(null,"","",0,1));
 }
}
