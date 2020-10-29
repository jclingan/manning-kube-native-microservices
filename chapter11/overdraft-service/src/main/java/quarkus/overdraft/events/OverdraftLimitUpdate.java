package quarkus.overdraft.events;

import java.math.BigDecimal;

public class OverdraftLimitUpdate {
  public Long accountNumber;
  public BigDecimal newOverdraftLimit;
}
