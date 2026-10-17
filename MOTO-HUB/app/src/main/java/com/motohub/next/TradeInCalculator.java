package com.motohub.next;

/** Offline motorcycle replacement budget; all amounts are Korean won. */
public final class TradeInCalculator {
 private TradeInCalculator() {}
 public static final class Result {
  public final long totalPurchaseCost;
  public final long expectedSale;
  public final long additionalCashNeeded;
  public final long cashRemaining;
  private Result(long totalPurchaseCost, long expectedSale) {
   this.totalPurchaseCost = totalPurchaseCost;
   this.expectedSale = expectedSale;
   this.additionalCashNeeded = Math.max(0L, totalPurchaseCost - expectedSale);
   this.cashRemaining = Math.max(0L, expectedSale - totalPurchaseCost);
  }
 }
 /** Calculates buy price + transfer/registration + delivery + initial servicing - sale proceeds. */
 public static Result calculate(long purchasePrice, long registration, long delivery,
                                long firstService, long equipment, long saleProceeds) {
  long[] amounts = {purchasePrice, registration, delivery, firstService, equipment, saleProceeds};
  for(long amount : amounts) if(amount < 0 || amount > 1_000_000_000_000L)
   throw new IllegalArgumentException("금액은 0원 이상 1조 원 이하여야 합니다.");
  if(purchasePrice == 0) throw new IllegalArgumentException("구매가를 입력해 주세요.");
  long total = purchasePrice + registration + delivery + firstService + equipment;
  return new Result(total, saleProceeds);
 }
}
