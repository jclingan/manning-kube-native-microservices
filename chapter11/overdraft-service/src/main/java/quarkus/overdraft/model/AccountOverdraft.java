package quarkus.overdraft.model;

import java.math.BigDecimal;

public class AccountOverdraft {
  public Long accountNumber;
  public BigDecimal currentOverdraft;
  public int numberOverdrawnEvents = 0;
}
