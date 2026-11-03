package com.motohub.next;

/**
 * Saved ride category is independent from free-text title and memo search.
 * "All" never changes an existing search query, and unset tags are not inferred.
 */
final class RideCategoryFilter {
 static final String ALL="전체 종류";
 static final String[] OPTIONS={ALL,RideCategory.AUTO,RideCategory.DAILY,RideCategory.COMMUTE,
  RideCategory.WINDING,RideCategory.LONG,RideCategory.TOUR};
 private RideCategoryFilter(){}
 static String normalize(String value){
  if(value==null)return ALL;
  for(String option:OPTIONS)if(option.equals(value))return option;
  return ALL;
 }
 static boolean matches(String filter,String stored){
  String selected=normalize(filter);
  if(ALL.equals(selected))return true;
  return selected.equals(stored);
 }
}
