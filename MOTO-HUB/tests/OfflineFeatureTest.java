import com.motohub.next.TradeInCalculator;
import com.motohub.next.MaintenancePlanner;
import static com.motohub.next.MaintenancePlanner.Status;

public class OfflineFeatureTest {
 static int passed = 0;
 static void check(boolean condition,String label) {
  if(!condition)throw new AssertionError(label);
  passed++;System.out.println("PASS " + label);
 }
 static void fails(Runnable r,String label) {
  try{r.run();throw new AssertionError(label + " accepted invalid data");}
  catch(RuntimeException e){passed++;System.out.println("PASS " + label);}
 }
 public static void main(String[] args) {
  TradeInCalculator.Result result = TradeInCalculator.calculate(5_300_000,125_000,100_000,80_000,210_000,2_500_000);
  check(result.totalPurchaseCost == 5_815_000,"trade-in full cost");
  check(result.additionalCashNeeded == 3_315_000,"trade-in net additional cash");
  check(result.cashRemaining == 0,"no surplus on expensive replacement");
  result = TradeInCalculator.calculate(1_000_000,0,0,0,0,2_000_000);
  check(result.cashRemaining == 1_000_000,"surplus from cheaper bike");
  check(result.additionalCashNeeded == 0,"zero cash needed for cheaper bike");
  fails(()->TradeInCalculator.calculate(-1,0,0,0,0,0),"reject negative amounts");
  fails(()->TradeInCalculator.calculate(0,0,0,0,0,0),"require bike purchase price");
  fails(()->TradeInCalculator.calculate(Long.MAX_VALUE,0,0,0,0,0),"reject extreme amount");
  check(MaintenancePlanner.status("2026-10-15",0,"2026-10-16",100,30,500)==Status.OVERDUE,"expired date");
  check(MaintenancePlanner.status("",4500,"2026-10-16",4500,30,500)==Status.OVERDUE,"reached mileage");
  check(MaintenancePlanner.status("2026-10-15",8000,"2026-10-16",4500,30,500)==Status.OVERDUE,"expired date takes priority over distant mileage");
  check(MaintenancePlanner.status("2026-11-05",0,"2026-10-16",100,30,500)==Status.SOON,"upcoming 30 day date");
  check(MaintenancePlanner.status("",5000,"2026-10-16",4700,30,500)==Status.SOON,"upcoming 500 km");
  check(MaintenancePlanner.status("2027-01-01",10000,"2026-10-16",4700,30,500)==Status.LATER,"long-range maintenance");
  check(MaintenancePlanner.status("",0,"2026-10-16",4700,30,500)==Status.NONE,"not configured");
  check(MaintenancePlanner.order(Status.OVERDUE)<MaintenancePlanner.order(Status.SOON),"urgent first");
  check(MaintenancePlanner.order(Status.SOON)<MaintenancePlanner.order(Status.LATER),"soon before later");
  check("곧 점검".equals(MaintenancePlanner.caption(Status.SOON)),"Korean label");
  fails(()->MaintenancePlanner.status("2026-02-30",0,"2026-10-16",0,30,500),"invalid calendar date");
  fails(()->MaintenancePlanner.status("",Double.NaN,"2026-10-16",0,30,500),"invalid odometer");
  System.out.println("TOTAL " + passed + " passed");
 }
}
