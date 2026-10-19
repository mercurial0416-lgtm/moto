package com.motohub.next;

import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/** Pure-Java classification of photo lookup failures, with safe user-facing messages. */
final class PhotoSearchStatus {
 private PhotoSearchStatus(){}
 enum Kind { NO_MATCH, NETWORK, SERVICE, IMAGE }
 static Kind classify(Exception error){
  if(error instanceof UnknownHostException||error instanceof SocketTimeoutException
   ||error instanceof ConnectException||error instanceof NoRouteToHostException)return Kind.NETWORK;
  if(error instanceof IOException){
   String msg=error.getMessage();
   if(msg!=null&&msg.startsWith("Commons HTTP "))return Kind.SERVICE;
   return Kind.NETWORK;
  }
  return Kind.SERVICE;
 }
 static String message(Kind kind){
  if(kind==Kind.NETWORK)return "네트워크 연결을 확인해 주세요";
  if(kind==Kind.SERVICE)return "사진 서버 응답에 문제가 있어요";
  if(kind==Kind.IMAGE)return "검색된 사진을 불러오지 못했어요";
  return "일치하는 모델 사진이 없어요";
 }
 static final class Trace {
  private boolean successfulSearch;
  private boolean matchingCandidate;
  private boolean imageFailure;
  private Kind networkOrService;
  void searchSucceeded(int candidateCount){successfulSearch=true;if(candidateCount>0)matchingCandidate=true;}
  void imageFailed(){imageFailure=true;}
  void searchFailed(Exception error){
   Kind kind=classify(error);
   if(kind==Kind.NETWORK||networkOrService==null)networkOrService=kind;
  }
  Kind result(){
   if(matchingCandidate||imageFailure)return Kind.IMAGE;
   if(successfulSearch)return Kind.NO_MATCH;
   return networkOrService==null?Kind.SERVICE:networkOrService;
  }
 }
}
