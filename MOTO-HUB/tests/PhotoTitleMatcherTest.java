package com.motohub.next;

public final class PhotoTitleMatcherTest {
 private static int checks;
 private static void verify(boolean actual, boolean expected, String label) {
  checks++;
  if(actual != expected) throw new AssertionError(label + " expected " + expected + " got " + actual);
 }
 public static void main(String[] args) {
  verify(PhotoTitleMatcher.matches("File:2024 KTM 390 Duke.jpg", "KTM", "390 Duke", 2024), true, "word order");
  verify(PhotoTitleMatcher.matches("File:2024 Yamaha YZF-R3.JPG", "Yamaha", "YZF-R3", 2024), true, "hyphen model");
  verify(PhotoTitleMatcher.matches("File:BMW G310R 2023.jpg", "BMW Motorrad", "G 310 R", 2023), true, "joined model token");
  verify(PhotoTitleMatcher.matches("File:Honda CBR500R 2024.jpg", "Honda", "CBR 500 R", 2024), true, "joined displacement");
  verify(PhotoTitleMatcher.matches("File:Harley-Davidson Sportster S 2024.jpg", "Harley-Davidson", "Sportster S", 2024), true, "two word brand");
  verify(PhotoTitleMatcher.matches("File:KTM Duke 3900 2024.jpg", "KTM", "Duke 390", 2024), false, "390 must not match 3900");
  verify(PhotoTitleMatcher.matches("File:2024 Yamaha R30.jpg", "Yamaha", "R3", 2024), false, "R3 must not match R30");
  verify(PhotoTitleMatcher.matches("File:2024 Kawasaki Ninja 400R.jpg", "Kawasaki", "Ninja 400", 2024), false, "400 must not match 400R");
  verify(PhotoTitleMatcher.matches("File:20240 KTM Duke 390.jpg", "KTM", "Duke 390", 2024), false, "year boundary");
  verify(PhotoTitleMatcher.matches("File:2023 KTM Duke 390.jpg", "KTM", "Duke 390", 2024), false, "wrong year");
  verify(PhotoTitleMatcher.matches("File:2023 2024 KTM Duke 390.jpg", "KTM", "Duke 390", 2024), false, "ambiguous years");
  verify(PhotoTitleMatcher.matches("File:2024 Indianapolis Scout.jpg", "Indian", "Scout", 2024), false, "brand boundary");
  verify(PhotoTitleMatcher.matches("File:2024 Honda Super Cub C125.jpg", "Honda", "Super Cub 110", 2024), false, "110 must not match C125");
  verify(PhotoTitleMatcher.matches("File:2024 Honda Super Cub 110.jpg", "Honda", "Super Cub 110", 2024), true, "correct cub");
  verify(PhotoTitleMatcher.matches("File:2024 KTM 390 Adventure.jpg", "KTM", "390 Duke", 2024), false, "different trim");
  verify(PhotoTitleMatcher.matchesModel("File:2024 KTM 390 Duke.jpg", "KTM", "390 Duke"), true, "fallback same model");
  verify(PhotoTitleMatcher.matchesModel("File:2024 KTM 390 Adventure.jpg", "KTM", "390 Duke"), false, "fallback different model");
  verify(PhotoTitleMatcher.matchesModel("File:2024 KTM 390 Duke.jpg", "", "390 Duke"), false, "missing brand");
  verify(PhotoTitleMatcher.matchesModel("File:2024 KTM 390 Duke.jpg", "KTM", ""), false, "missing model");
  System.out.println("PASS: " + checks + " conservative photo filename checks");
 }
}
