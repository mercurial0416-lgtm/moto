package com.motohub.next;
import org.junit.Test;
import static org.junit.Assert.*;
public class CardoStandbyPolicyTest {
 @Test public void cappedWaits(){assertEquals(600000L,CardoStandbyPolicy.idleWindowMs(false));assertEquals(900000L,CardoStandbyPolicy.idleWindowMs(true));}
}
