package com.motohub.next;

import android.content.Context;
import android.util.AtomicFile;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/** Worker-only validated JSON commit. Callers own the existing process-wide write reservation. */
final class FeatureRecordWriter {
 static final class Result {final JSONObject data;final boolean checkpoint;Result(JSONObject data,boolean checkpoint){this.data=data;this.checkpoint=checkpoint;}}
 private FeatureRecordWriter(){}
 static Result persist(Context context,AtomicFile disk,String before,String after)throws Exception{
  JSONObject previous=Store.parse(before),next=Store.parse(after);byte[] bytes=next.toString(2).getBytes(StandardCharsets.UTF_8);LocalSafetyBackup.snapshot(context,previous);
  FileOutputStream output=null;try{output=disk.startWrite();output.write(bytes);disk.finishWrite(output);output=null;}catch(Exception failure){if(output!=null)disk.failWrite(output);throw failure;}
  boolean checkpoint=false;try{checkpoint=LocalSafetyBackup.checkpoint(context,next);}catch(Exception ignored){}return new Result(next,checkpoint);
 }
}
