package com.motohub.next;

/** Strict top-level gallery choice in persisted attribution metadata, pure Java. */
public final class ModelPhotoMetadataTest {
 private static int checks;
 private static void check(String json,boolean chosen,String label){
  checks++;
  boolean actual=BackupFormat.selectedModelMetadata(json);
  if(actual!=chosen)throw new AssertionError(label+" expected="+chosen+" actual="+actual);
 }
 public static void main(String[] args){
  check("{\"userSelected\":true,\"label\":\"Honda Super Cub\"}",true,"selected gallery image");
  check("{\"license\":\"CC BY-SA\",\"userSelected\": true,\"url\":\"https://example.org\"}",true,"with attribution");
  check("{\"userSelected\":false}",false,"auto cached image");
  check("{\"userSelected\":\"true\"}",false,"text is not boolean");
  check("{\"userSelected\":1}",false,"number is not boolean");
  check("{\"data\":{\"userSelected\":true}}",false,"nested selection must not count");
  check("{\"note\":\"\\\"userSelected\\\":true\",\"userSelected\":false}",false,"quoted annotation must not count");
  check("{\"note\":\"userSelected:true\"}",false,"quoted note must not count");
  check("{\"userSelected\":true,\"userSelected\":false}",false,"duplicate top-level selection rejected");
  check("{\"userSelected\":false,\"userSelected\":true}",false,"duplicated late override rejected");
  check("{\"userSelected\":true}bad",false,"trailing junk rejected");
  check("{\"userSelected\":true,}",false,"trailing comma rejected");
  check("{\"userSelected\":true",false,"truncated json rejected");
  check("{\"userSelected\":true,\"image\":{\"nested\":[{\"b\":1},false,3.14]}}",true,"valid nested metadata");
  check("{\"u\\u0073erSelected\":true}",true,"escaped key recognized");
  check("{\"userSelected\":true,\"note\":\"bad\\q\"}",false,"illegal JSON escape rejected");
  check("{\"userSelected\":true,\"note\":01}",false,"bad JSON number rejected");
  check("{\"userSelected\":true,\"note\":[[[[[[[[[[[[[[[[[[[[[[[[[[]]]]]]]]]]]]]]]]]]]]]]]]]]]}",false,"excessive JSON depth rejected");
  check(null,false,"null metadata");
  check("[]",false,"metadata must be object");
  check("{}" ,false,"no selection");
  System.out.println("PASS model photo JSON validation: "+checks+" cases");
 }
}
