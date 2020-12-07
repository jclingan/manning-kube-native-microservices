package io.quarkus.transactions;

import java.math.BigDecimal;

public class Account {
  public Long accountNumber;
  public Long customerNumber;
  public String customerName;
  public BigDecimal balance;
  public BigDecimal overdraftLimit;
  public AccountStatus accountStatus = AccountStatus.OPEN;
}
