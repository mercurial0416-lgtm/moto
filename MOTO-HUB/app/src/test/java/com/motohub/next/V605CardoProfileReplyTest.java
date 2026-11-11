package com.motohub.next;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class V605CardoProfileReplyTest {
 @Test public void oneProfileCannotCountTwice(){
  Set<Integer> observed=new HashSet<>();
  assertTrue(CardoConnectionProbe.acceptProfileReply(observed,1));
  assertFalse(CardoConnectionProbe.acceptProfileReply(observed,1));
  assertEquals(1,observed.size());
  assertTrue(CardoConnectionProbe.acceptProfileReply(observed,2));
  assertEquals(2,observed.size());
 }
}
