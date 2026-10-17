package com.motohub.next;
import java.util.Locale;
/** Model-aware illustrations: do not present a different model as the owner's bike. */
final class BikeVisual {
  private BikeVisual() {}
  static boolean superCub(String raw) {
    if(raw == null) return false;
    String name=raw.toLowerCase(Locale.ROOT).replaceAll("[\\s_-]+", "");
    // Only show the bundled 110 illustration for an explicitly identified 110.
    // C125, 50 and unspecified Super Cub models must use the neutral placeholder.
    return (name.contains("슈퍼커브") || name.contains("supercub") || name.contains("슈퍼컵"))
        && name.contains("110") && !name.contains("125");
  }
  static String caption(String model, boolean custom) {
    if(custom)return "내 바이크 사진";
    return superCub(model)?"슈퍼커브 110 참고 일러스트 · 탭하여 내 사진 변경":"모델 사진 미등록 · 탭하여 내 사진 추가";
  }
}
