package com.motohub.next;

/** Explicit app-only launcher choices; no destination coordinates, routing, or private ride data. */
enum NavigationChoice {
 KAKAO("kakao","카카오내비","com.locnall.KimGiSa"),
 NAVER("naver","네이버지도","com.nhn.android.nmap"),
 TMAP("tmap","티맵","com.skt.tmap.ku");
 final String id,label,packageName;
 NavigationChoice(String id,String label,String packageName){
  this.id=id;this.label=label;this.packageName=packageName;
 }
 static NavigationChoice fromId(String value){
  for(NavigationChoice app:values())if(app.id.equals(value))return app;
  return KAKAO;
 }
 static String[] labels(){
  NavigationChoice[] choices=values();String[] labels=new String[choices.length];
  for(int i=0;i<choices.length;i++)labels[i]=choices[i].label;
  return labels;
 }
 static NavigationChoice at(int index){
  NavigationChoice[] choices=values();
  return index>=0&&index<choices.length?choices[index]:KAKAO;
 }
}
