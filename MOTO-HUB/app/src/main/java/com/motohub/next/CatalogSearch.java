package com.motohub.next;

import java.util.Collection;
import java.util.Locale;

/** Case/punctuation-insensitive offline search of model names and their Korean aliases. */
final class CatalogSearch {
 private CatalogSearch() {}
 static String norm(String value) {
  return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{Nd}]", "");
 }
 static boolean matches(String model, Collection<String> aliases, String query) {
  String needle = norm(query);
  if(needle.isEmpty()) return true;
  if(norm(model).contains(needle)) return true;
  if(aliases != null) for(String alias : aliases) if(norm(alias).contains(needle)) return true;
  return false;
 }
}
