package com.motohub.next;
import org.junit.Test;
import static org.junit.Assert.*;
public final class UpdatePromptPolicyTest {
 @Test public void newVersionAlwaysPrompts(){
  assertTrue(UpdatePromptPolicy.shouldPrompt(525,524,524,100,101));
  assertFalse(UpdatePromptPolicy.shouldPrompt(524,524,523,100,101));
 }
 @Test public void snoozeForOneDayThenRemind(){
  long day=UpdatePromptPolicy.REMIND_AFTER_MS;
  assertFalse(UpdatePromptPolicy.shouldPrompt(525,524,525,1_000,1_000+day-1));
  assertTrue(UpdatePromptPolicy.shouldPrompt(525,524,525,1_000,1_000+day));
  assertTrue(UpdatePromptPolicy.shouldPrompt(525,524,525,0,100));
 }
 @Test public void phoneClockReversalNeverHidesIndefinitely(){
  assertTrue(UpdatePromptPolicy.shouldPrompt(525,524,525,3_000,2_000));
 }
}
