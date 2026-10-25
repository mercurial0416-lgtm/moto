package com.motohub.next;

import java.util.Arrays;
import java.util.Collections;

public final class CatalogSearchTest {
 private static int checks;
 private static void test(boolean actual,boolean expected,String name){
  checks++;if(actual!=expected)throw new AssertionError(name+": expected "+expected+", got "+actual);
 }
 public static void main(String[] args){
  test(CatalogSearch.matches("Super Cub 110",Arrays.asList("슈퍼커브","슈퍼컵110"),"슈퍼커브"),true,"Korean nickname");
  test(CatalogSearch.matches("Super Cub 110",Arrays.asList("슈퍼커브","슈퍼컵110"),"슈퍼컵 110"),true,"Korean punctuation");
  test(CatalogSearch.matches("YZF-R3",Arrays.asList("알쓰리"),"yzf r3"),true,"Romanized punctuation");
  test(CatalogSearch.matches("390 Duke",Arrays.asList("듀크390"),"듀크 390"),true,"Korean word order nickname");
  test(CatalogSearch.matches("Super Cub C125",Collections.singletonList("C125"),"슈퍼커브110"),false,"do not match different model");
  test(CatalogSearch.matches("390 Duke",Collections.singletonList("듀크390"),"790"),false,"do not match other displacement");
  test(CatalogSearch.matches("ZX-4RR",Collections.emptyList(),"zx4rr"),true,"normalized model code");
  test(CatalogSearch.matches("Ninja 400",null,"ninja"),true,"null alias list");
  test(CatalogSearch.matches("Ninja 400",null,""),true,"empty query lists all");
  test(CatalogSearch.matches("Ninja 400",null," "),true,"whitespace query lists all");
  test(CatalogSearch.matches("Ninja 400",null,"R3"),false,"nonmatching query");
  test(CatalogSearch.matches(null,null,"r3"),false,"null model");
  test(CatalogSearch.matches("R3",null,"R3"),true,"exact model");
  System.out.println("PASS: "+checks+" alias-aware catalog search checks");
 }
}
