package com.motohub.next;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Completing an interrupted GPS file must never manufacture a clean .done marker. */
public final class Ux9TrackCompletionTest {
 static int checks;
 static void yes(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
 interface Action{void run()throws Exception;}
 static void fails(Action action,String label)throws Exception{
  try{action.run();throw new AssertionError("accepted "+label);}catch(IOException expected){checks++;}
 }
 public static void main(String[]args)throws Exception{
  File folder=Files.createTempDirectory("moto-ux9-completion-").toFile();
  String base="1792500000000-12345678-1234-1234-1234-123456789abc.track";
  File clean=new File(folder,base);
  File cut=new File(folder,"1792500000001-12345678-1234-1234-1234-123456789abc.track");
  File wrong=new File(folder,"1792500000002-12345678-1234-1234-1234-123456789abc.track");
  String first=TrackFile.point(37,127,5,1792500000000L);
  String second=TrackFile.point(37.01,127.01,5,1792500005000L);
  try{
   Files.writeString(clean.toPath(),first+second,StandardCharsets.UTF_8);
   byte[] source=Files.readAllBytes(clean.toPath());
   TrackCompletion.mark(clean,2);
   yes(new File(clean.getPath()+".done").isFile(),"complete track receives .done marker");
   yes(Arrays.equals(source,Files.readAllBytes(clean.toPath())),"source exact bytes unchanged");
   fails(()->TrackCompletion.mark(clean,2),"duplicate completion");
   Files.writeString(cut.toPath(),first+second.trim(),StandardCharsets.UTF_8);
   byte[] partial=Files.readAllBytes(cut.toPath());
   fails(()->TrackCompletion.mark(cut,1),"incomplete final row");
   yes(!new File(cut.getPath()+".done").exists(),"no false marker for partial GPS");
   yes(Arrays.equals(partial,Files.readAllBytes(cut.toPath())),"partial bytes remain recoverable");
   Files.writeString(wrong.toPath(),first+second,StandardCharsets.UTF_8);
   fails(()->TrackCompletion.mark(wrong,3),"wrong committed-point count");
   yes(!new File(wrong.getPath()+".done").exists(),"no marker after point-count mismatch");
  }finally{
   File[] files=folder.listFiles();
   if(files!=null)for(File f:files)Files.deleteIfExists(f.toPath());
   Files.deleteIfExists(folder.toPath());
  }
  System.out.println("PASS UX9 GPS completion integrity: "+checks+" checks");
 }
}
