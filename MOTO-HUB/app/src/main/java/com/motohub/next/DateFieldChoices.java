package com.motohub.next;

/** UI policy for calendar fields: optional dates can be reset without typing. */
final class DateFieldChoices {
 private DateFieldChoices() {}
 static boolean isDateField(String key) {
  return "date".equals(key) || "purchaseDate".equals(key)
      || "soldDate".equals(key) || "insuranceDate".equals(key)
      || "nextDate".equals(key);
 }
 static boolean canClear(String form, String key) {
  if(!isDateField(key)) return false;
  // Service and expense records require their transaction date for sorting and history.
  return !"date".equals(key) || (!"services".equals(form) && !"expenses".equals(form));
 }
}
