package com.motohub.next;
import org.junit.Test;
import java.io.*;
import java.nio.file.*;
import static org.junit.Assert.*;
public final class MapCachePolicyTest {
 @Test public void onlyClearsSpecifiedCacheFolder()throws Exception{
  File root=Files.createTempDirectory("moto-map-cache").toFile();
  try{
   File tiles=MapCachePolicy.target(root);assertTrue(tiles.mkdirs());
   File outside=new File(root,"rides.track");
   Files.write(outside.toPath(),new byte[]{1,2});
   File leaf=new File(tiles,"01");assertTrue(leaf.mkdir());
   Files.write(new File(leaf,"tile").toPath(),new byte[]{1,2,3,4});
   assertEquals(1,MapCachePolicy.scan(tiles,false).files);
   MapCachePolicy.Result cleared=MapCachePolicy.scan(tiles,true);
   assertEquals(1,cleared.files);assertTrue(outside.exists());
   assertEquals(0,MapCachePolicy.scan(tiles,false).files);
  }finally{
   for(File f:root.listFiles())if(f.isFile())f.delete();
   File tiles=MapCachePolicy.target(root);tiles.delete();new File(root,"map").delete();root.delete();
  }
 }
}
