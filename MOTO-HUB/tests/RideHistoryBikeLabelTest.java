package com.motohub.next;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
public final class RideHistoryBikeLabelTest {
 static int checks;
 static void check(boolean condition,String reason){checks++;if(!condition)throw new AssertionError(reason);}
 public static void main(String[] args)throws Exception{
  Path dir=Files.createTempDirectory("moto-bike-caption");
  String id="a231c9d0-2f0b-4d91-a73b-12034f4e20bc";
  File track=dir.resolve("1800000000000-00000000-0000-4000-8000-000000000001.track").toFile();
  Map<String,String> names=new HashMap<>();
  try{
   Files.write(track.toPath(),"37,127,3,1800000000000\n".getBytes(StandardCharsets.UTF_8));
   check("바이크 미지정".equals(RideHistoryBikeLabel.label(track,names)),"unassigned visible");
   Files.write(Paths.get(track.getPath()+".bike"),id.getBytes(StandardCharsets.US_ASCII));
   check("등록되지 않은 바이크".equals(RideHistoryBikeLabel.label(track,names)),"deleted bike distinct from unassigned");
   names.put(id,"슈퍼커브 110");
   check("슈퍼커브 110".equals(RideHistoryBikeLabel.label(track,names)),"registered bike shown");
   Files.write(Paths.get(track.getPath()+".bike"),"invalid id".getBytes(StandardCharsets.US_ASCII));
   check("바이크 연결 정보 손상".equals(RideHistoryBikeLabel.label(track,names)),"corruption not unassigned");
   check(Files.exists(track.toPath()),"GPS source preserved");
  }finally{
   Files.deleteIfExists(Paths.get(track.getPath()+".bike"));Files.deleteIfExists(track.toPath());Files.deleteIfExists(dir);
  }
  System.out.println("PASS: "+checks+" bike label integrity assertions");
 }
}
