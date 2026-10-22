package com.motohub.next;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Cautious offline OCR suggestions, never a persistent or automatic odometer update. */
final class OdometerOcrCandidates {
 private OdometerOcrCandidates(){}
 private static final Pattern NUMBER=Pattern.compile("(?<![0-9])(?:[0-9]{1,3}(?:,[0-9]{3})+|[0-9]{2,7})(?:\\.[0-9])?(?![0-9])");
 private static final int MAX_OPTIONS=5;
 static List<String> fromText(String text){
  LinkedHashSet<String> results=new LinkedHashSet<>();
  if(text==null||text.length()>30_000)return new ArrayList<>();
  String[] lines=text.split("\\r?\\n",120);
  for(String line:lines){
   String lower=line.toLowerCase(Locale.ROOT);
   if(lower.contains("trip")||lower.contains("rpm")||lower.contains("타코")||lower.contains("잔여")||lower.contains("range"))continue;
   boolean labeled=lower.contains("odo")||lower.contains("km")||lower.contains("주행거리")||lower.contains("총거리");
   if(!labeled)continue;
   Matcher m=NUMBER.matcher(line);
   while(m.find()&&results.size()<MAX_OPTIONS){
    String raw=m.group().replace(",","");
    try {
     double km=Double.parseDouble(raw);
     if(Double.isFinite(km)&&km>=10&&km<=9_999_999)results.add(raw);
    }catch(NumberFormatException ignored){}
   }
  }
  // Exact one-token digital odometer screens without a KM label are still useful,
  // but never guess from a receipt-like screen with many unrelated numbers.
  if(results.isEmpty()&&text.trim().matches("[0-9]{2,7}(?:\\.[0-9])?"))results.add(text.trim());
  return new ArrayList<>(results);
 }
}
