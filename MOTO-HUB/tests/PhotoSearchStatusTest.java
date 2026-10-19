package com.motohub.next;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public final class PhotoSearchStatusTest {
 static int count;
 static void check(boolean yes,String name){count++;if(!yes)throw new AssertionError(name);}
 public static void main(String[] args){
  check(PhotoSearchStatus.classify(new UnknownHostException())==PhotoSearchStatus.Kind.NETWORK,"DNS error");
  check(PhotoSearchStatus.classify(new SocketTimeoutException())==PhotoSearchStatus.Kind.NETWORK,"timeout");
  check(PhotoSearchStatus.classify(new IOException("Commons HTTP 503"))==PhotoSearchStatus.Kind.SERVICE,"server error");
  check(PhotoSearchStatus.classify(new IOException("socket closed"))==PhotoSearchStatus.Kind.NETWORK,"connection IO error");
  PhotoSearchStatus.Trace empty=new PhotoSearchStatus.Trace();empty.searchSucceeded(0);
  check(empty.result()==PhotoSearchStatus.Kind.NO_MATCH,"no matching photo");
  PhotoSearchStatus.Trace offline=new PhotoSearchStatus.Trace();offline.searchFailed(new UnknownHostException());
  check(offline.result()==PhotoSearchStatus.Kind.NETWORK,"all searches offline");
  PhotoSearchStatus.Trace server=new PhotoSearchStatus.Trace();server.searchFailed(new IOException("Commons HTTP 429"));
  check(server.result()==PhotoSearchStatus.Kind.SERVICE,"server throttling");
  PhotoSearchStatus.Trace broken=new PhotoSearchStatus.Trace();broken.searchSucceeded(3);broken.imageFailed();
  check(broken.result()==PhotoSearchStatus.Kind.IMAGE,"matching images failed to load");
  PhotoSearchStatus.Trace mixed=new PhotoSearchStatus.Trace();mixed.searchFailed(new UnknownHostException());mixed.searchSucceeded(0);
  check(mixed.result()==PhotoSearchStatus.Kind.NO_MATCH,"successful fallback with no results");
  PhotoSearchStatus.Trace fallback=new PhotoSearchStatus.Trace();fallback.searchSucceeded(1);fallback.searchFailed(new IOException("Commons HTTP 503"));
  check(fallback.result()==PhotoSearchStatus.Kind.IMAGE,"matching candidate with failed image");
  for(PhotoSearchStatus.Kind kind:PhotoSearchStatus.Kind.values()){
   String message=PhotoSearchStatus.message(kind);
   check(message!=null&&!message.isEmpty()&&!message.contains("http"),"safe message "+kind);
  }
  System.out.println("PASS: "+count+" photo error classification checks");
 }
}
