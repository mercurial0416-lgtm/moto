package com.motohub.next;
/** Sold bikes remain local records and retain all linked maintenance and fuel entries. */
final class GarageBikeStatus {
 private GarageBikeStatus(){}
 static boolean sold(String soldDate){return soldDate!=null&&!soldDate.trim().isEmpty();}
 static String label(String name,String soldDate){return name+(sold(soldDate)?"  ·  판매 완료":"");}
 static int rank(String soldDate){return sold(soldDate)?1:0;}
}
