package com.motohub.next;

import java.time.LocalDate;

/** Format search range dates; never infer a year from a partial search term. */
final class GarageSearchDate {
 private GarageSearchDate(){}
 static String iso(String raw){
  if(raw==null||raw.trim().isEmpty())return "";
  String value=raw.trim().replace('.','-').replace('/','-');
  String[] p=value.split("-", -1);
  if(p.length!=3||!p[0].matches("[0-9]{4}")||
      !p[1].matches("[0-9]{1,2}")||!p[2].matches("[0-9]{1,2}"))
   throw new IllegalArgumentException("연도·월·일을 모두 입력해 주세요.");
  try{
   return LocalDate.of(Integer.parseInt(p[0]),Integer.parseInt(p[1]),Integer.parseInt(p[2])).toString();
  }catch(java.time.DateTimeException bad){
   throw new IllegalArgumentException("올바른 날짜가 아닙니다.",bad);
  }
 }
 static boolean within(String day,String from,String to){
  if(day==null||day.isEmpty())return from.isEmpty()&&to.isEmpty();
  return (from.isEmpty()||day.compareTo(from)>=0)&&(to.isEmpty()||day.compareTo(to)<=0);
 }
}
