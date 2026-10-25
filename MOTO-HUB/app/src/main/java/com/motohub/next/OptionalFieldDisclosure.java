package com.motohub.next;

/** Small UI policy: do not hide optional values when an existing record/draft is opened. */
final class OptionalFieldDisclosure {
 private OptionalFieldDisclosure() {}

 /** A numeric placeholder of zero means 'not set', unlike valid text such as a note. */
 static boolean hasValue(String key, String input) {
  if(input == null || input.trim().isEmpty()) return false;
  String value=input.trim();
  if("nextKm".equals(key) || "distance".equals(key) || "purchase".equals(key)) {
   try { return Double.isFinite(Double.parseDouble(value)) && Double.parseDouble(value) != 0; }
   catch(NumberFormatException ignored) { return true; } // Invalid user text must stay visible for repair.
  }
  return true;
 }
}
