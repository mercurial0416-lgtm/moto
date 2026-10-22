package com.motohub.next;

/** Shared ZIP limits between exporter, readback validator and restorer. */
final class BackupFormat {
 private BackupFormat() {}
 static final long MAX_TOTAL_BYTES=250_000_000L;
 static final long MAX_JSON_BYTES=8_000_000L;
 static final long MAX_TRACK_BYTES=25_000_000L;
 static final long MAX_GPX_BYTES=50_000_000L;
 static final int MAX_TRACKS=10_000;
 static final int MAX_PHOTOS=1_000;
 static final int MAX_RECEIPTS=4_000;
 static final int MAX_ENTRIES=100_000;
 static final String RECEIPT=ReceiptSlots.PATTERN;
 static final String PHOTO="garage-photo-[a-fA-F0-9-]{36}\\.jpg";
 static final String TRACK="[0-9]{10,15}-[a-f0-9-]{36}\\.track";
 static final String MODEL_PHOTO="[a-f0-9]{24}\\.jpg";
 static final String MODEL_META="[a-f0-9]{24}\\.jpg\\.json";
 static boolean isModelPhoto(String name){return name!=null&&name.matches("model-photos/"+MODEL_PHOTO);}
 static boolean isModelMetadata(String name){return name!=null&&name.matches("model-photos/"+MODEL_META);}
 static boolean selectedModelMetadata(String data){
  if(data==null||data.length()>8192)return false;
  try{
   PhotoSelectionJson reader=new PhotoSelectionJson(data);
   boolean chosen=reader.readObject(true,0);
   reader.skipSpace();
   return chosen&&reader.atEnd();
  }catch(IllegalArgumentException invalid){return false;}
 }
 /** Never evict a photo unless complete, structurally sound metadata proves automatic origin. */
 static boolean automaticallyCachedModelMetadata(String data){
  if(data==null||data.length()>8192)return false;
  try{
   PhotoSelectionJson reader=new PhotoSelectionJson(data);
   boolean selected=reader.readObject(true,0);
   reader.skipSpace();
   return reader.atEnd()&&!selected&&reader.validAutomaticFields()
    &&(!reader.selectionPresent||reader.selectionType==2);
  }catch(IllegalArgumentException invalid){return false;}
 }
 /** Small strict JSON reader, avoiding an Android-only JSON dependency in backup policies. */
 private static final class PhotoSelectionJson {
  private final String value;
  private int index;
  private boolean sawImage,sawPage,sawExact,selectionPresent;
  private boolean imageString,pageString,exactBoolean;
  private int selectionType;
  PhotoSelectionJson(String input){value=input;}
  boolean validAutomaticFields(){return sawImage&&sawPage&&sawExact&&imageString&&pageString&&exactBoolean;}
  boolean atEnd(){return index==value.length();}
  void skipSpace(){
   while(index<value.length()){
    char c=value.charAt(index);
    if(c==' '||c=='\n'||c=='\r'||c=='\t')index++;else break;
   }
  }
  boolean eat(char symbol){
   if(index<value.length()&&value.charAt(index)==symbol){index++;return true;}
   return false;
  }
  void expect(char symbol){if(!eat(symbol))throw new IllegalArgumentException("Invalid JSON");}
  boolean digit(){return index<value.length()&&value.charAt(index)>='0'&&value.charAt(index)<='9';}
  String readString(){
   expect('"');
   StringBuilder result=new StringBuilder();
   while(index<value.length()){
    char c=value.charAt(index++);
    if(c=='"')return result.toString();
    if(c<0x20)throw new IllegalArgumentException("Unescaped JSON control character");
    if(c!='\\'){result.append(c);continue;}
    if(index>=value.length())throw new IllegalArgumentException("Unterminated JSON escape");
    char e=value.charAt(index++);
    if(e=='"'||e=='\\'||e=='/')result.append(e);
    else if(e=='b')result.append('\b');
    else if(e=='f')result.append('\f');
    else if(e=='n')result.append('\n');
    else if(e=='r')result.append('\r');
    else if(e=='t')result.append('\t');
    else if(e=='u'){
     if(index+4>value.length())throw new IllegalArgumentException("Bad unicode escape");
     int code=0;
     for(int i=0;i<4;i++){
      int digit=Character.digit(value.charAt(index++),16);
      if(digit<0)throw new IllegalArgumentException("Bad unicode digit");
      code=(code<<4)|digit;
     }
     result.append((char)code);
    }else throw new IllegalArgumentException("Bad JSON escape");
   }
   throw new IllegalArgumentException("Unterminated JSON string");
  }
  void keyword(String expected){
   if(!value.regionMatches(index,expected,0,expected.length()))throw new IllegalArgumentException("Invalid JSON literal");
   index+=expected.length();
  }
  void number(){
   eat('-');
   if(!eat('0')){
    if(index>=value.length()||value.charAt(index)<'1'||value.charAt(index)>'9')
     throw new IllegalArgumentException("Invalid JSON number");
    do{index++;}while(digit());
   }
   if(eat('.')){
    if(!digit())throw new IllegalArgumentException("Missing decimal digits");
    do{index++;}while(digit());
   }
   if(eat('e')||eat('E')){
    if(!eat('+'))eat('-');
    if(!digit())throw new IllegalArgumentException("Missing exponent digits");
    do{index++;}while(digit());
   }
  }
  int readValue(int depth){
   if(depth>24)throw new IllegalArgumentException("JSON depth exceeded");
   skipSpace();
   if(index>=value.length())throw new IllegalArgumentException("Missing JSON value");
   char c=value.charAt(index);
   if(c=='{'){readObject(false,depth);return 4;}
   if(c=='['){
    index++;skipSpace();
    if(eat(']'))return 5;
    while(true){
     readValue(depth+1);skipSpace();
     if(eat(']'))return 5;
     expect(',');skipSpace();
    }
   }
   if(c=='"'){readString();return 3;}
   if(c=='t'){keyword("true");return 1;}
   if(c=='f'){keyword("false");return 2;}
   if(c=='n'){keyword("null");return 0;}
   number();return 6;
  }
  boolean readObject(boolean outer,int depth){
   if(depth>24)throw new IllegalArgumentException("JSON depth exceeded");
   skipSpace();expect('{');skipSpace();
   if(eat('}'))return false;
   boolean selected=false,seen=false;
   while(true){
    String key=readString();
    skipSpace();expect(':');
    int typed=readValue(depth+1);
    if(outer){
     if("userSelected".equals(key)){
      if(seen)throw new IllegalArgumentException("Duplicate userSelected key");
      seen=true;selectionPresent=true;selectionType=typed;selected=typed==1;
     }else if("imageUrl".equals(key)){
      if(sawImage)throw new IllegalArgumentException("Duplicate imageUrl key");
      sawImage=true;imageString=typed==3;
     }else if("pageUrl".equals(key)){
      if(sawPage)throw new IllegalArgumentException("Duplicate pageUrl key");
      sawPage=true;pageString=typed==3;
     }else if("exactYearCandidate".equals(key)){
      if(sawExact)throw new IllegalArgumentException("Duplicate exactYearCandidate key");
      sawExact=true;exactBoolean=typed==1||typed==2;
     }
    }
    skipSpace();
    if(eat('}'))return selected;
    expect(',');skipSpace();
   }
  }
 }
 static String sidecarSuffix(String name){
  for(String suffix:new String[]{".done",".imported",".recovered",".unmerged",".mergedinto",".bike",".category",".favorite"})
   if(name.endsWith(suffix))return suffix;
  return "";
 }
 static boolean isTrack(String name){return name!=null&&name.matches("rides/"+TRACK);}
 static boolean isReceipt(String name){return name!=null&&name.matches("receipts/"+RECEIPT);}
 static boolean isPhoto(String name){return name!=null&&name.matches("photos/"+PHOTO);}
 static boolean isGpx(String name){return name!=null&&name.matches("gpx/[0-9]{10,15}-[a-f0-9-]{36}\\.gpx");}
 static boolean isSidecar(String name){
  if(name==null||!name.startsWith("rides/"))return false;
  String suffix=sidecarSuffix(name);
  return !suffix.isEmpty()&&name.substring(0,name.length()-suffix.length()).matches("rides/"+TRACK);
 }
 static boolean isDirectory(String name){return "rides/".equals(name)||"photos/".equals(name)||"receipts/".equals(name)||"gpx/".equals(name)||"model-photos/".equals(name);}
 static boolean allowed(String name,boolean directory){
  if(name==null||name.startsWith("/")||name.contains("..")||name.contains("\\")||name.indexOf('\0')>=0)return false;
  if(directory)return isDirectory(name);
  return "MOTO-HUB-data.json".equals(name)||"README-KO.txt".equals(name)||isPhoto(name)||isReceipt(name)||isTrack(name)||isGpx(name)||isSidecar(name)||isModelPhoto(name)||isModelMetadata(name);
 }
 static long entryLimit(String name){
  if("MOTO-HUB-data.json".equals(name))return MAX_JSON_BYTES;
  if("README-KO.txt".equals(name))return 8192;
  if(isGpx(name))return MAX_GPX_BYTES;
  if(isModelMetadata(name))return 8192;
  if(isSidecar(name)){
   if(name.endsWith(".mergedinto"))return 100;
   if(name.endsWith(".category")||name.endsWith(".bike"))return 64;
   return 0;
  }
  return MAX_TRACK_BYTES;
 }
}
