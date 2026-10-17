package com.motohub.next;
public final class HomeBikeSwitchTest {
 public static void main(String[] args) {
  if(HomeBikeSwitch.visible(0))throw new AssertionError("empty garage switch");
  if(HomeBikeSwitch.visible(1))throw new AssertionError("single bike switch");
  if(!HomeBikeSwitch.visible(2)||!HomeBikeSwitch.visible(5))throw new AssertionError("multiple bikes switch");
  if(!HomeBikeSwitch.label(1).equals("MY BIKE"))throw new AssertionError("single label");
  if(!HomeBikeSwitch.label(2).contains("바이크 변경"))throw new AssertionError("multi label");
  System.out.println("PASS: 5 home bike switch assertions");
 }
}
