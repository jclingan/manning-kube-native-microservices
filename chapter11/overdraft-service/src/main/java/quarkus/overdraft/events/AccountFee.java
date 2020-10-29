package quarkus.overdraft.events;

import java.math.BigDecimal;

public class AccountFee {
  public Long accountNumber;
  public BigDecimal overdraftFee;
}
