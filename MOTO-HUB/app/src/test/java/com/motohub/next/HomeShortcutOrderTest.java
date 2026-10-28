package com.motohub.next;
import org.junit.Test;
import static org.junit.Assert.*;
public final class HomeShortcutOrderTest {
 @Test public void rejectUnknownAndDuplicateKeysWithoutLosingDefaultActions(){
  assertArrayEquals(new String[]{"fuel","ride","nav","service"},
    HomeShortcutOrder.parse("fuel,fuel,invalid,ride"));
 }
 @Test public void swapOnlyInsideFourActionSet(){
  assertEquals("ride,fuel,nav,service",
    HomeShortcutOrder.move("ride,nav,fuel,service","fuel",-1));
  assertEquals("ride,nav,fuel,service",
    HomeShortcutOrder.move("ride,nav,fuel,service","ride",-1));
 }
}
