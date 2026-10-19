package com.motohub.next;

/** Bound the number of Android views created for long local ride histories. */
final class RideHistoryPage {
 static final int PAGE_SIZE=25;
 private RideHistoryPage(){}
 static int nextLimit(int current){
  return current>=Integer.MAX_VALUE-PAGE_SIZE?Integer.MAX_VALUE:Math.max(0,current)+PAGE_SIZE;
 }
 static int remaining(int matches,int shown){return Math.max(0,matches-Math.max(0,shown));}
}
