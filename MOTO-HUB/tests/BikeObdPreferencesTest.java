package com.motohub.next;

public final class BikeObdPreferencesTest {
 public static void main(String[] args){
  String duke="ktm-390-2024",other="another-bike";
  check(!BikeObdPreferences.adapterKey(duke).equals(BikeObdPreferences.adapterKey(other)),"OBD adapter key must be bike-specific");
  check(!BikeObdPreferences.stopAfterKey(duke).equals(BikeObdPreferences.stopAfterKey(other)),"OBD stop-after key must be bike-specific");
  check(!BikeObdPreferences.adapterKey(duke).equals(BikeObdPreferences.stopAfterKey(duke)),"settings keys must be disjoint");
  try{BikeObdPreferences.adapterKey("");throw new AssertionError("blank ID accepted");}catch(IllegalArgumentException correct){}
  try{BikeObdPreferences.stopAfterKey(null);throw new AssertionError("null ID accepted");}catch(IllegalArgumentException correct){}
  System.out.println("BikeObdPreferencesTest PASS");
 }
 static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
