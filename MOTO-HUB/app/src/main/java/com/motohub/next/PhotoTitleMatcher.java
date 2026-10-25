package com.motohub.next;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Conservative Commons filename filter. A filename is only a candidate, never proof of model year. */
final class PhotoTitleMatcher {
 private static final Pattern TOKEN = Pattern.compile("[\\p{L}\\p{N}]+");
 private PhotoTitleMatcher() {}

 private static List<String> tokens(String value) {
  List<String> result = new ArrayList<>();
  if(value == null) return result;
  Matcher matcher = TOKEN.matcher(value.toLowerCase(Locale.ROOT));
  while(matcher.find()) result.add(matcher.group());
  return result;
 }

 private static boolean hasTerm(List<String> title, String term) {
  if(title.contains(term)) return true;
  // Filenames sometimes join a model's tokens: "YZF-R3" vs "YZFR3".
  for(int i = 0; i < title.size(); i++) {
   StringBuilder joined = new StringBuilder();
   for(int j = i; j < title.size() && j < i + 4; j++) {
    joined.append(title.get(j));
    if(joined.toString().equals(term)) return true;
    if(joined.length() > term.length()) break;
   }
  }
  return false;
 }

 private static boolean containsModel(List<String> title, List<String> model) {
  if(model.isEmpty()) return false;
  boolean all = true;
  for(String term : model) if(!hasTerm(title, term)) { all = false; break; }
  if(all) return true;
  // Accept the entire model code as one token, but not as a substring of a different code.
  StringBuilder joined = new StringBuilder();
  for(String term : model) joined.append(term);
  return hasTerm(title, joined.toString());
 }

 static boolean matchesModel(String title, String brand, String model) {
  List<String> filename = tokens(title);
  List<String> maker = tokens(brand);
  // "BMW Motorrad" is commonly shortened to "BMW" in photo filenames.
  maker.removeIf("motorrad"::equals);
  if(maker.isEmpty() || !containsModel(filename, maker)) return false;
  return containsModel(filename, tokens(model));
 }

 /** Avoid assigning a photo titled with multiple model years to one exact year. */
 private static boolean hasConflictingYear(String title,int year) {
  for(String token:tokens(title)) {
   if(token.length()!=4)continue;
   try {int value=Integer.parseInt(token);if(value>=1900&&value<=2200&&value!=year)return true;}
   catch(NumberFormatException ignored){}
  }
  return false;
 }
 /** Automatic fallback is allowed for undated photos, but never knowingly picks another model year.
  *  Manual gallery browsing still uses matchesModel so a person can inspect and choose.
  */
 static boolean matchesFallback(String title,String brand,String model,int requestedYear){
  return matchesModel(title,brand,model)
   && (requestedYear<1900||requestedYear>2200||!hasConflictingYear(title,requestedYear));
 }
 static boolean matches(String title, String brand, String model, int year) {
  if(year < 1900 || year > 2200) return false;
  // Year must be a standalone token, not part of a serial number or 20240.
  return tokens(title).contains(Integer.toString(year)) && matchesModel(title, brand, model) && !hasConflictingYear(title, year);
 }
}
