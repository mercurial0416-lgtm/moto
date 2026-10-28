package com.motohub.next;
import org.junit.Test;
import static org.junit.Assert.*;
public final class RideRecoveryPromptPolicyTest {
 @Test public void laterIsTemporaryAndFilesRemainIndependent(){
  long now=1_798_500_000_000L;
  assertTrue(RideRecoveryPromptPolicy.shouldOffer("ride1.track","","",0,now));
  assertFalse(RideRecoveryPromptPolicy.shouldOffer("ride1.track","","ride1.track",now,now));
  assertFalse(RideRecoveryPromptPolicy.shouldOffer("ride1.track","","ride1.track",now,now+RideRecoveryPromptPolicy.SNOOZE_MS-1));
  assertTrue(RideRecoveryPromptPolicy.shouldOffer("ride1.track","","ride1.track",now,now+RideRecoveryPromptPolicy.SNOOZE_MS));
  assertTrue(RideRecoveryPromptPolicy.shouldOffer("ride2.track","","ride1.track",now,now+1));
  assertFalse(RideRecoveryPromptPolicy.shouldOffer("ride1.track","ride1.track","",0,now));
  assertTrue(RideRecoveryPromptPolicy.shouldOffer("ride1.track","","ride1.track",now,now-1000));
  assertFalse(RideRecoveryPromptPolicy.shouldOffer("","","",0,now));
 }
}
