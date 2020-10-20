package quarkus.overdraft.events;

import java.math.BigDecimal;

public class OverdraftFee {
  public Long accountNumber;
  public BigDecimal overdraftFee;
}
