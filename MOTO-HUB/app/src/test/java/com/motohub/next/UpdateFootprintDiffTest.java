package com.motohub.next;
import org.junit.Test;
import static org.junit.Assert.*;
public final class UpdateFootprintDiffTest {
 @Test public void distinguishesFileTamperingFromCountChanges(){
  String before="schema3;vehicles=1;tracks=2;contentSha256=aaa";
  String after="schema3;vehicles=1;tracks=2;contentSha256=bbb";
  assertTrue(UpdateFootprintDiff.describe(before,after).contains("GPS·사진 파일 내용"));
  assertTrue(UpdateFootprintDiff.describe(before,"schema3;vehicles=2;tracks=2;contentSha256=aaa").contains("저장 기록 vehicles"));
 }
}
