package com.motohub.next;
import org.junit.Test;
import static org.junit.Assert.*;
public class RideResumePolicyTest {
 @Test public void boundedResumeWindowAndCanonicalName(){
  String name="1790000000000-01234567-89ab-4cde-8fab-0123456789ab.track";
  long now=1790000600000L;
  assertTrue(RideResumePolicy.eligible(name,now-10000,now));
  assertFalse(RideResumePolicy.eligible(name,now-1000000,now));
  assertFalse(RideResumePolicy.eligible("../"+name,now-10000,now));
  assertFalse(RideResumePolicy.eligible(name,now+1,now));
 }
}
