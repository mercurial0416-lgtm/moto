package com.motohub.next;

import java.util.*;

public final class PhotoGalleryChoicesTest {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 public static void main(String[] args){
  List<String> result=new ArrayList<>();Set<String> seen=new HashSet<>();
  PhotoGalleryChoices.append(result,seen,Arrays.asList("year-A","year-B","year-A",null,""),s->s,3);
  check(result.equals(Arrays.asList("year-A","year-B")),"dedupe/empty filtering");
  PhotoGalleryChoices.append(result,seen,Arrays.asList("model-B","model-C","model-D"),s->s,3);
  check(result.equals(Arrays.asList("year-A","year-B","model-B")),"year candidates keep priority, maximum respected");
  PhotoGalleryChoices.append(result,seen,Arrays.asList("late"),s->s,3);
  check(result.size()==3,"cannot append beyond cap");
  List<String> empty=new ArrayList<>();PhotoGalleryChoices.append(empty,new HashSet<>(),Arrays.asList("one"),s->s,0);
  check(empty.isEmpty(),"zero cap");
  PhotoGalleryChoices.append(empty,new HashSet<>(),null,s->s,4);
  check(empty.isEmpty(),"null candidates");
  System.out.println("PhotoGalleryChoicesTest PASS (5 assertions)");
 }
}
