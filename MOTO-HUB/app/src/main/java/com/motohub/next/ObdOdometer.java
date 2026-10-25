package com.motohub.next;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
/** Standard SAE mode 01 PID A6 reader. Experimental: the bike may not expose this PID. */
final class ObdOdometer {
 private static final Pattern REPLY=Pattern.compile("(?i)(?:^|[^0-9a-f])41\\s*A6\\s*([0-9a-f]{2})\\s*([0-9a-f]{2})\\s*([0-9a-f]{2})\\s*([0-9a-f]{2})(?=$|\\s|>)");
 static double parseKm(String raw){
  if(raw==null||raw.length()>4096)return Double.NaN;
  Matcher match=REPLY.matcher(raw);
  Double first=null;
  while(match.find()){
   long value=0;
   for(int i=1;i<=4;i++)value=(value<<8)|Integer.parseInt(match.group(i),16);
   double km=value/10.0;
   if(km<=0||km>2_000_000)return Double.NaN;
   // Multiple ECU/adapter replies must agree; never import an ambiguous ODO.
   if(first!=null&&Double.compare(first,km)!=0)return Double.NaN;
   first=km;
  }
  return first==null?Double.NaN:first;
 }
 private ObdOdometer(){}
}
