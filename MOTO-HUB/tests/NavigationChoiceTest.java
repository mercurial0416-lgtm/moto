package com.motohub.next;

public final class NavigationChoiceTest {
 private static int checks=0;
 private static void check(boolean pass,String name){checks++;if(!pass)throw new AssertionError(name);}
 public static void main(String[] args){
  check(NavigationChoice.values().length==3,"only three approved navigation apps");
  check(NavigationChoice.fromId(null)==NavigationChoice.KAKAO,"missing setting defaults to Kakao");
  check(NavigationChoice.fromId("invalid")==NavigationChoice.KAKAO,"unknown setting safely defaults to Kakao");
  check(NavigationChoice.fromId("naver")==NavigationChoice.NAVER,"naver setting");
  check(NavigationChoice.fromId("tmap")==NavigationChoice.TMAP,"tmap setting");
  check(NavigationChoice.NAVER.packageName.equals("com.nhn.android.nmap"),"official Naver app");
  check(NavigationChoice.TMAP.packageName.equals("com.skt.tmap.ku"),"official Tmap app");
  check(NavigationChoice.KAKAO.packageName.equals("com.locnall.KimGiSa"),"existing Kakao compatible");
  check(NavigationChoice.labels().length==3,"all options available");
  check(NavigationChoice.at(-1)==NavigationChoice.KAKAO,"invalid index safely handled");
  check(NavigationChoice.at(3)==NavigationChoice.KAKAO,"invalid upper index safely handled");
  check(NavigationChoice.at(2)==NavigationChoice.TMAP,"selected TMAP correctly");
  System.out.println("PASS NavigationChoiceTest "+checks);
 }
}
