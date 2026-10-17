package com.motohub.next;

/** Year choices for bike registration. Pure Java to allow offline regression tests. */
final class CatalogYearChoices {
 static final int FIRST_YEAR = 1900;
 private CatalogYearChoices() {}
 static int lastYear(int currentYear) { return currentYear + 2; }
 static boolean valid(int year,int currentYear) {
  return year >= FIRST_YEAR && year <= lastYear(currentYear);
 }
 static int preferred(String typed,int currentYear) {
  try {
   int parsed = Integer.parseInt(typed == null ? "" : typed.trim());
   if(valid(parsed,currentYear)) return parsed;
  } catch(NumberFormatException ignored) {}
  return currentYear;
 }
 static String[] labels(int currentYear) {
  String[] result = new String[lastYear(currentYear) - FIRST_YEAR + 1];
  for(int i=0;i<result.length;i++) result[i] = (lastYear(currentYear) - i) + "년식";
  return result;
 }
 static int yearAt(int index,int currentYear) {
  if(index < 0 || index > lastYear(currentYear)-FIRST_YEAR) throw new IllegalArgumentException("연식 선택 범위 초과");
  return lastYear(currentYear) - index;
 }
 static int selectedIndex(int year,int currentYear) {
  return valid(year,currentYear) ? lastYear(currentYear)-year : lastYear(currentYear)-currentYear;
 }
}
