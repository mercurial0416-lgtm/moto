package com.motohub.next;
/** Constant-memory, 30-row paging: the user can reach every saved record. */
final class ListPageLimit {
 static final int PAGE=30;
 private ListPageLimit(){}
 static int start(int total,int offset){
  if(total<=0)return 0;
  int safe=Math.max(0,offset);
  return Math.min(safe,((total-1)/PAGE)*PAGE);
 }
 static int end(int total,int offset){return Math.min(Math.max(0,total),start(total,offset)+PAGE);}
 static int next(int total,int offset){return end(total,offset)<total?end(total,offset):start(total,offset);}
 static int previous(int total,int offset){return Math.max(0,start(total,offset)-PAGE);}
 static boolean canNext(int total,int offset){return end(total,offset)<total;}
 static boolean canPrevious(int total,int offset){return start(total,offset)>0;}
}
